package com.ericdmartell.maga.actions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.ericdmartell.maga.MAGA;
import com.ericdmartell.maga.associations.MAGAAssociation;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.objects.MAGAObject;
import com.ericdmartell.maga.utils.HistoryUtil;
import com.ericdmartell.maga.utils.JDBCUtil;
import com.ericdmartell.maga.utils.MAGAException;
import com.ericdmartell.maga.utils.ReflectionUtils;

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
		//Object for history
		MAGAObject oldObj = null;
		List<MAGAAssociation> affectedAssociations = maga.
				loadWhereHasClassWithJoinColumn(obj.getClass());
		if (obj.id == 0) {
			//We're adding a new object.
			addSQL(obj);
		} else {
			oldObj = maga.load(obj.getClass(), obj.id);
			//If the object being updated has a join column, an old object might have been joined with it.  We
			//Dirty those assocs.
			for (MAGAAssociation assoc : affectedAssociations) {
				biDirectionalDirty(obj, assoc);
			}
			//Update the db after we've done our dirtying.
			updateSQL(obj);
		}
		
		cache.dirtyObject(obj);
		for (MAGAAssociation assoc : affectedAssociations) {
			long val = -1;
			long oldVal = -1;
			val = (long) ReflectionUtils.getFieldValue(obj, assoc.class2Column());
			if (oldObj != null) {
				oldVal = (long) ReflectionUtils.getFieldValue(oldObj, assoc.class2Column());
			}
			if (val != -1 && (oldObj == null || oldVal != val)) {
				//We have a new assoc... the object on the other side needs to have its assoc pointing at this one dirtied.
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

	private void addSQL(MAGAObject obj) {
		List<String> fieldNames = new ArrayList<>(ReflectionUtils.getFieldNames(obj.getClass()));

		String sql = "insert into  " + obj.getClass().getSimpleName() + "(";
		for (String fieldName : fieldNames) {
			if (fieldName.equals("id")) {
				continue;
			}
			sql += fieldName + ",";
		}
		sql = sql.substring(0, sql.length() - 1);
		sql += ") values(";
		for (int i = 0; i < fieldNames.size() - 1; i++) {

			sql += "?,";
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
			pstmt.executeUpdate();
			ResultSet rst = pstmt.executeQuery("select LAST_INSERT_ID()");
			rst.next();
			obj.id = rst.getLong(1);
		} catch (SQLException e) {
			throw new MAGAException(e);
		} finally {
			JDBCUtil.closeConnection(con);
		}

	}

	private void updateSQL(MAGAObject obj) {
		List<String> fieldNames = new ArrayList<>(ReflectionUtils.getFieldNames(obj.getClass()));

		String sql = "update  " + obj.getClass().getSimpleName() + " set ";
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
			pstmt.setLong(fieldNames.size() + 1, obj.id);
			pstmt.executeUpdate();

		} catch (SQLException e) {
			throw new MAGAException(e);
		} finally {
			JDBCUtil.closeConnection(con);
		}
	}
}
