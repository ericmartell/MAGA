package com.ericdmartell.maga.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.ericdmartell.maga.objects.MAGAObject;

public class DirectSqlQuery {
	private List<Class> classes;
	private List<String> joins;
	private List<String> ons;
	private String where;
	private DataSource dataSource;

	public DirectSqlQuery(Class clazz, DataSource dataSource) {
		classes = new ArrayList<>();
		joins = new ArrayList<>();
		ons = new ArrayList<>();
		where = "";
		classes.add(clazz);
		this.dataSource = dataSource;
	}

	public DirectSqlQuery innerJoin(Class clazz, String on) {
		joins.add("inner join");
		classes.add(clazz);
		ons.add(on);
		return this;
	}

	public DirectSqlQuery leftJoin(Class clazz, String on) {
		joins.add("inner join");
		classes.add(clazz);
		ons.add(on);
		return this;
	}

	public DirectSqlQuery where(String where) {
		this.where = where;
		return this;
	}

	public List<Map<Class, MAGAObject>> go(Object... params) {
		Connection con = null;
		try {
			con = JDBCUtil.getConnection(dataSource);
			String query = "select * from `" + classes.get(0).getSimpleName() + "`";
			for (int i = 0; i < joins.size(); i++) {
				query += " " + joins.get(i) + " ";
				query += " `" + classes.get(i + 1).getSimpleName() + "` ";
				query += " on " + ons.get(i) + " ";
			}
			if (where != null && !where.trim().isEmpty()) {
				query += " where " + where;
			}
			Map<Class, List<String>> fieldNames = new HashMap<>();
			for (Class clazz : classes) {
				fieldNames.put(clazz, new ArrayList<>(ReflectionUtils.getFieldNames(clazz)));
			}
			ResultSet rst = JDBCUtil.executeQuery(con, query, params);

			List<Map<Class, MAGAObject>> results = new ArrayList<>();

			while (rst.next()) {
				Map<Class, MAGAObject> row = new HashMap<>();
				for (Class clazz : classes) {
					if (rst.getString(clazz.getSimpleName() + ".id") == null) {
						continue;
					}
					MAGAObject obj = (MAGAObject) clazz.newInstance();
					for (String fieldName : fieldNames.get(clazz)) {
						ReflectionUtils.setFieldValue(obj, fieldName,
								rst.getObject(clazz.getSimpleName() + "." + fieldName));
					}
					row.put(clazz, obj);
				}
				results.add(row);
			}
			return results;

		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			JDBCUtil.closeConnection(con);
		}
	}

}
