package com.ericdmartell.maga.actions;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.reflections.Reflections;

import com.ericdmartell.maga.MAGA;
import com.ericdmartell.maga.associations.MAGAAssociation;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.objects.MAGALoadTemplate;
import com.ericdmartell.maga.objects.MAGAObject;
import com.ericdmartell.maga.utils.JDBCUtil;
import com.ericdmartell.maga.utils.MAGAException;
import com.ericdmartell.maga.utils.ReflectionUtils;

import gnu.trove.map.hash.THashMap;

public class AssociationLoad {
	private DataSource dataSource;
	private MAGALoadTemplate template;
	private MAGACache cache;
	private MAGA maga;

	public AssociationLoad(DataSource dataSource, MAGACache cache, MAGA maga,
			MAGALoadTemplate template) {
		this.dataSource = dataSource;
		this.cache = cache;
		this.maga = maga;
		this.template = template;
	}

	public List<MAGAObject> load(MAGAObject obj, MAGAAssociation association) {
		// Before even going to memcached, did this object come out of a
		// template? If so we have associations stored on the object itself.
		List<MAGAObject> ret = cache.getAssociatedObjectsForTemplate(obj, association);

		if (ret != null) {
			return ret;
		} else {
			// Get classes from the other side of the assoc.
			Class classToGet = obj.getClass() == association.class1() ? association.class2() : association.class1();

			// Load ids from the other side of the assoc and then bulk load the
			// objects themselves
			ret = maga.load(classToGet, loadIds(obj, association));

			// If we're running in a template, cache the result on the object
			// itself.
			if (template != null) {
				if (obj.templateAssociations == null) {
					obj.templateAssociations = new THashMap<>();
				}
				cache.cacheAssociatedObjectsForTemplate(obj, association, ret);
			}
			return ret;
		}

	}

	public <T extends MAGAObject> List<T> loadWhere(Class<T> clazz, String where, Object... params) {
		//TODO: Add some caching for this, but then we'd have to check every object add and update to see if the associated where returns new results.
		List<String> ids = JDBCUtil.executeQueryAndReturnStrings(dataSource, "select id from `" + clazz.getSimpleName() + "` where " + where, params);
		return maga.load(clazz, ids);
		
	}
	
	private List<String> loadIds(MAGAObject obj, MAGAAssociation association) {
		// Memcached
		List<String> ret = cache.getAssociatedIds(obj, association);

		if (ret == null) {
			// Go to the database.
			if (association.type() == MAGAAssociation.MANY_TO_MANY) {
				ret = getManyToManyFromDB(obj, association);
			} else if (association.type() == MAGAAssociation.ONE_TO_MANY) {
				ret = getOneToManyFromDB(obj, association);
			}
			cache.setAssociatedIds(obj, association, ret, template);
		}
		if (this.template != null) {
			this.cache.addTemplateDependencyOnAssoc(obj, association, this.template);
		}
		return ret;
	}

	private List<String> getOneToManyFromDB(MAGAObject obj, MAGAAssociation association) {
		List<String> ret = new ArrayList<>();

		String query;
		if (obj.getClass() == association.class1()) {
			// We're on the one side of the one-many.
			query = "select id from `" + association.class2().getSimpleName() + "` where " + association.class2Column() + "= ?";
		} else {
			// We're on the many side of the one-many... The join data is right
			// on the object... But it might be dirty so we refresh
			obj = maga.load(obj.getClass(), obj.id);
			String val = null;
			val = (String) ReflectionUtils.getFieldValue(obj, association.class2Column());

			if (val != null && !val.equals("")) {
				ret.add(val);
				return ret;
			} else {
				// Field must not exist in javaland...
				query = "select `" + association.class2Column() + "` from `" + association.class2().getSimpleName()
						+ "` where id = ?";
			}
		}

		// Builds out list of ids from query.
		Connection con = JDBCUtil.getConnection(dataSource);
		try {
			ResultSet rst = JDBCUtil.executeQuery(con, query, obj.id);
			while (rst.next()) {
				ret.add(rst.getString(1));
			}
		} catch (SQLException e) {
			throw new MAGAException(e);
		} finally {
			JDBCUtil.closeConnection(con);
		}

		return ret;
	}

	private List<String> getManyToManyFromDB(MAGAObject obj, MAGAAssociation association) {
		List<String> ret = new ArrayList<>();
		
		String tableName = association.class1().getSimpleName() + "_to_" + association.class2().getSimpleName();

		String whereColumn;
		String otherColumn;
		if (obj.getClass() == association.class1()) {
			whereColumn = association.class1().getSimpleName();
			otherColumn = association.class2().getSimpleName();
		} else {
			whereColumn = association.class2().getSimpleName();
			otherColumn = association.class1().getSimpleName();
		}
		Connection con = JDBCUtil.getConnection(dataSource);
		try {
			ResultSet rst = JDBCUtil.executeQuery(con,
					"select distinct `" + otherColumn + "` from `" + tableName + "` where `" + whereColumn + "` = ?", obj.id);
			while (rst.next()) {
				ret.add(rst.getString(otherColumn));
			}
		} catch (SQLException e) {
			throw new MAGAException(e);
		} finally {
			JDBCUtil.closeConnection(con);
		}
		return ret;
	}

	
	//When we're deleting objects, or modifying objects with join columns, we need to dirty/delete assocs
	private static Map<Class<MAGAObject>, List<MAGAAssociation>> classToLoadColumnAssocs = null;
	private static Map<Class<MAGAObject>, List<MAGAAssociation>> classToAssocs = null;
	
	private void initializeClassToAssocs() {
		try {
			classToAssocs = new THashMap<>();
			classToLoadColumnAssocs = new THashMap<>();
			Reflections reflections = new Reflections("");
			List<Class<MAGAAssociation>> associations = new ArrayList(
					reflections.getSubTypesOf(MAGAAssociation.class));
			for (Class<MAGAAssociation> association : associations) {
				MAGAAssociation assoc = association.newInstance();
				
				if (!classToAssocs.containsKey(assoc.class1())) {
					classToAssocs.put(assoc.class1(), new ArrayList<>());
				}
				classToAssocs.get(assoc.class1()).add(assoc);
				
				if (!classToAssocs.containsKey(assoc.class2())) {
					classToAssocs.put(assoc.class2(), new ArrayList<>());
				}
				classToAssocs.get(assoc.class2()).add(assoc);
				
				if (assoc.type() == MAGAAssociation.ONE_TO_MANY) {
					if (!classToLoadColumnAssocs.containsKey(assoc.class2())) {
						classToLoadColumnAssocs.put(assoc.class2(), new ArrayList<>());
					}
					classToLoadColumnAssocs.get(assoc.class2()).add(assoc);
				}
			}
		} catch (InstantiationException | IllegalAccessException e) {
			throw new MAGAException(e);
		}
	}
	
	public List loadWhereHasClassWithJoinColumn(Class clazz) {
		if (classToLoadColumnAssocs == null) {
			initializeClassToAssocs();
		}
		List ret = classToLoadColumnAssocs.get(clazz);
		if (ret == null) {
			return new ArrayList<>();
		}
		return ret;

	}

	

	public List loadWhereHasClass(Class clazz) {
		if (classToAssocs == null) {
			initializeClassToAssocs();
		}
		List ret = classToAssocs.get(clazz);
		if (ret == null) {
			return new ArrayList<>();
		}
		return ret;
	}

	
}
