package com.ericdmartell.simpleorm.actions;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import com.ericdmartell.simpleorm.SimpleORM;
import com.ericdmartell.simpleorm.cache.SimpleORMCache;
import com.ericdmartell.simpleorm.factory.ActionFactory;
import com.ericdmartell.simpleorm.objects.SimpleORMLoadTemplate;
import com.ericdmartell.simpleorm.objects.SimpleORMObject;
import com.ericdmartell.simpleorm.utils.JDBCUtil;
import com.ericdmartell.simpleorm.utils.ReflectionUtils;
import com.ericdmartell.simpleorm.utils.SimpleORMException;

public class ObjectLoad {

	private DataSource dataSource;
	private ActionFactory loadPathFactory;
	private SimpleORMLoadTemplate template;
	private SimpleORMCache cache;

	public ObjectLoad(DataSource dataSource, SimpleORMCache cache, ActionFactory loadPathFactory,
			SimpleORMLoadTemplate template) {
		this.dataSource = dataSource;
		this.cache = cache;
		this.loadPathFactory = loadPathFactory;
		this.template = template;
	}

	public List<SimpleORMObject> loadTemplate(SimpleORMLoadTemplate template, Object[] args) {
		List<SimpleORMObject> ret = (List<SimpleORMObject>) cache.get(template.getKey());
		if (ret != null) {
			return ret;
		} else {
			ret = template.run(new SimpleORM(dataSource, cache, null, template), args);
			// save our result for next fetch.
			cache.set(template.getKey(), ret);
			return ret;
		}
	}

	public List<SimpleORMObject> loadAll(Class clazz) {
		Connection connection = JDBCUtil.getConnection(dataSource);
		//To get all ids, we go to the db, but there should be a faster way to do this (I hope).
		try {
			ResultSet rst = JDBCUtil.executeQuery(connection, "select id from " + clazz.getSimpleName());
			List<Long> ids = new ArrayList<>();
			while (rst.next()) {
				ids.add(rst.getLong(1));
			}
			return load(clazz, ids);
		} catch (SQLException e) {
			throw new SimpleORMException(e);
		} finally {
			JDBCUtil.closeConnection(connection);
		}
	}

	public SimpleORMObject load(Class clazz, long id) {
		//Just a wrapper on the load collection of ids.
		List<Long> ids = new ArrayList<>();
		ids.add(id);
		List<SimpleORMObject> retList = load(clazz, ids);
		if (retList.isEmpty()) {
			return null;
		} else {
			return retList.get(0);
		}
	}

	public List<SimpleORMObject> load(Class clazz, Collection<Long> ids) {

		// A running list of ids to load
		List<Long> toLoad = new ArrayList<>(ids);

		//Remove 0's
		List<Long> zeroList = new ArrayList<>();
		zeroList.add(0L);
		toLoad.removeAll(zeroList);
		
		//Don't make trips anywhere if the list is empty
		if (toLoad.isEmpty()) {
			return new ArrayList<>();
		}
		
		
		// Try getting them from memcached
		List<SimpleORMObject> ret = cache.getObjects(clazz, toLoad);

		// Remove the ids we got from memcached before going to the database.
		for (SimpleORMObject gotFromMemcached : ret) {
			toLoad.remove(gotFromMemcached.id);
		}

		// We still have ids that aren't in memcached, fetch from the database.
		if (!toLoad.isEmpty()) {
			List<SimpleORMObject> dbObjects = loadFromDB(clazz, toLoad);
			for (SimpleORMObject gotFromDB : dbObjects) {
				toLoad.remove(gotFromDB.id);
			}
			
			//We'll have them in the cache next time.
			cache.setObjects(dbObjects, template);

			ret.addAll(dbObjects);
		}

		// We went to memcached, we went to the db, and we still have ids left
		// over?
		if (!toLoad.isEmpty()) {
			// System.out.println("DB Misses for " + toLoad);
		}
		return ret;
	}

	private List<SimpleORMObject> loadFromDB(Class<SimpleORMObject> clazz, Collection<Long> ids) {
		List<SimpleORMObject> ret = new ArrayList<>();

		// Fields with annotations
		List<String> fieldNames = new ArrayList<>(ReflectionUtils.getFieldNames(clazz));

		// Sql to bulk fetch all ids.
		String sql = getSQL(clazz, fieldNames, ids);
		Connection connection = JDBCUtil.getConnection(this.dataSource);

		try {
			ResultSet rst = JDBCUtil.executeQuery(connection, sql);

			// Rather than repeatedly instantiating, we'll keep cloning this
			// guy. TODO: is this actually a perf gain?
			SimpleORMObject emptyObject = clazz.newInstance();

			while (rst.next()) {
				SimpleORMObject toFill = emptyObject.clone();
				// Fill those objects
				for (String fieldName : fieldNames) {
					Object value = rst.getObject(fieldName);
					ReflectionUtils.setFieldValue(toFill, fieldName, rst.getObject(fieldName));
				}

				ret.add(toFill);
			}
		} catch (SQLException | IllegalAccessException | InstantiationException e) {
			throw new SimpleORMException(e);
		} finally {
			JDBCUtil.closeConnection(connection);
		}
		return ret;

	}

	private String getSQL(Class<SimpleORMObject> clazz, Collection<String> fieldNames, Collection<Long> ids) {
		String sql = "select ";
		for (String fieldName : fieldNames) {
			sql += fieldName + ",";
		}
		sql = sql.substring(0, sql.length() - 1);
		sql += " from " + clazz.getSimpleName();
		if (ids.size() == 1) {
			sql += " where id = " + ids.iterator().next();
		} else {
			sql += " where id in (";
			for (long id : ids) {
				sql += id + ",";
			}
			sql = sql.substring(0, sql.length() - 1);
			sql += ")";
		}
		return sql;
	}

}
