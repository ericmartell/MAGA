package com.ericdmartell.maga.actions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import com.ericdmartell.maga.MAGA;
import com.ericdmartell.maga.annotations.MAGATimestampID;
import com.ericdmartell.maga.associations.MAGAAssociation;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.objects.MAGAObject;
import com.ericdmartell.maga.utils.HistoryUtil;
import com.ericdmartell.maga.utils.JDBCUtil;
import com.ericdmartell.maga.utils.MAGAException;
import com.ericdmartell.maga.utils.ReflectionUtils;

import gnu.trove.map.hash.THashMap;

public class ObjectUpdate {

	private MAGACache cache;
	private final DataSource dataSource;
	private MAGA maga;

	public ObjectUpdate(DataSource dataSource, MAGACache cache, MAGA maga) {
		this.dataSource = dataSource;
		this.maga = maga;
		this.cache = cache;
	}

	public void update(final MAGAObject obj) {
		// Object for history
		MAGAObject oldObj = null;
		List<MAGAAssociation> affectedAssociations = maga.loadWhereHasClassWithJoinColumn(obj.getClass());
		if (obj.id == null) {
			// We're adding a new object.
			addSQL(obj);
		} else {
			oldObj = maga.load(obj.getClass(), obj.id);
			// If the object being updated has a join column, an old object
			// might have been joined with it. We
			// Dirty those assocs.
			for (MAGAAssociation assoc : affectedAssociations) {
				biDirectionalDirty(obj, assoc);
			}
			// Update the db after we've done our dirtying.
			updateSQL(obj);
		}

		cache.dirtyObject(obj);
		for (MAGAAssociation assoc : affectedAssociations) {
			String val = null;
			String oldVal = null;
			try {
				val = (String) ReflectionUtils.getFieldValue(obj, assoc.class2Column());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (oldObj != null) {
				oldVal = (String) ReflectionUtils.getFieldValue(oldObj, assoc.class2Column());
			}
			if (val != null && (oldObj == null || oldVal == null || !oldVal.equals(val))) {
				// We have a new assoc... the object on the other side needs to
				// have its assoc pointing at this one dirtied.
				biDirectionalDirty(obj, assoc);
			}

		}
		HistoryUtil.recordHistory(oldObj, obj, maga, dataSource);
	}

	private void biDirectionalDirty(MAGAObject obj, MAGAAssociation association) {

		List<MAGAObject> otherSide = maga.loadAssociatedObjects(obj, association);
		cache.dirtyObject(obj);
		for (MAGAObject other : otherSide) {
			cache.dirtyAssoc(other, association);
		}
		cache.dirtyAssoc(obj, association);

	}
	private static Map<Class, Boolean> autoId = new THashMap<>();
	private void addSQL(MAGAObject obj) {
		Class clazz = obj.getClass();
		if (!autoId.containsKey(clazz)) {
			autoId.put(clazz, clazz.isAnnotationPresent(MAGATimestampID.class));
		}
		
		boolean genId = autoId.get(clazz);
		
		List<String> fieldNames = new ArrayList<>(ReflectionUtils.getFieldNames(obj.getClass()));

		String sql = "insert into  `" + obj.getClass().getSimpleName() + "`(";
		for (String fieldName : fieldNames) {
			if (fieldName.equals("id")) {
				continue;
			}
			sql += fieldName + ",";
		}
		if (genId) {
			sql += "id) values(";
			for (int i = 0; i < fieldNames.size(); i++) {
				sql += "?,";
			}
		} else {
			sql = sql.substring(0, sql.length() - 1);
			sql += ") values(";
			for (int i = 0; i < fieldNames.size() - 1; i++) {
				sql += "?,";
			}
		}
		
		
		sql = sql.substring(0, sql.length() - 1);
		sql += ")";
		Connection con = JDBCUtil.getConnection(dataSource);
		try {
			PreparedStatement pstmt = JDBCUtil.prepareStatmenent(con, sql);

			int i = 1;
			for (String fieldName : fieldNames) {

				if (fieldName.equals("id")) {
					continue;
				}
				pstmt.setObject(i++, ReflectionUtils.getFieldValue(obj, fieldName));

			}
			boolean success = false;
			String id = UUID.randomUUID().toString();
			while (!success) {
				try {
					if (genId) {
						pstmt.setString(i, id);
					}
					pstmt.executeUpdate();
					success = true;
				} catch (SQLException e) {
					id = UUID.randomUUID().toString();
				}
			}
			if (!genId) {
				ResultSet rst = pstmt.executeQuery("select LAST_INSERT_ID()");
				rst.next();
				obj.id = rst.getString(1);
			} else {
				obj.id = id;
			}
		} catch (SQLException e) {
			throw new MAGAException(e);
		} finally {
			JDBCUtil.closeConnection(con);
		}

	}

	private void updateSQL(MAGAObject obj) {
		List<String> fieldNames = new ArrayList<>(ReflectionUtils.getFieldNames(obj.getClass()));
		fieldNames.remove("id");
		String sql = "update  `" + obj.getClass().getSimpleName() + "` set ";
		for (String fieldName : fieldNames) {
			sql += fieldName + "= ? ,";
		}
		sql = sql.substring(0, sql.length() - 1);
		sql += " where id = ?";

		Connection con = JDBCUtil.getConnection(dataSource);
		try {
			PreparedStatement pstmt = JDBCUtil.prepareStatmenent(con, sql);

			for (int i = 0; i < fieldNames.size(); i++) {
				String fieldName = fieldNames.get(i);
				pstmt.setObject(i + 1, ReflectionUtils.getFieldValue(obj, fieldName));
			}
			pstmt.setString(fieldNames.size() + 1, obj.id);
			pstmt.executeUpdate();

		} catch (SQLException e) {
			throw new MAGAException(e);
		} finally {
			JDBCUtil.closeConnection(con);
		}
	}
}
