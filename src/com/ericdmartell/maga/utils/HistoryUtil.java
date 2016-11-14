package com.ericdmartell.maga.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import com.ericdmartell.maga.MAGA;
import com.ericdmartell.maga.objects.MAGAObject;
import com.fasterxml.jackson.databind.ObjectMapper;

import gnu.trove.map.hash.THashMap;

public class HistoryUtil {
	public static void recordHistory(final MAGAObject oldObjReference, final MAGAObject obj, MAGA maga, DataSource dataSource) {
		final Throwable e = new Throwable();
		maga.executorPool.submit(new Runnable() {
			@Override
			public void run() {
				MAGAObject oldObj = oldObjReference;
				MAGAObject object = obj;
				Map<String, Map<String, Object>> fieldsToChangedValues = new THashMap<>();
				for (String fieldName : ReflectionUtils.getFieldNames(object.getClass())) {
					Object oldValue = oldObjReference == null ? null : ReflectionUtils.getFieldValue(oldObjReference, fieldName);
					Object newValue = ReflectionUtils.getFieldValue(object, fieldName);
					if (oldValue == null && newValue == null) {
						
					} else if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
						
					} else {
						Map<String, Object> oldNewValues = new THashMap<>();
						oldNewValues.put("old", oldValue);
						oldNewValues.put("new", newValue);
						fieldsToChangedValues.put(fieldName, oldNewValues);
					}
				}
				ObjectMapper mapper = new ObjectMapper();
				String json;
				try {
					StringWriter jsonStringWriter = new StringWriter();
					mapper.writeValue(jsonStringWriter, fieldsToChangedValues);
					json = jsonStringWriter.toString();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
				Connection con = JDBCUtil.getConnection(dataSource);
				try {
					PreparedStatement stmt = JDBCUtil.prepareStatmenent(con, "insert into " + object.getClass().getSimpleName() + "_history values(?,now(),?,?)");
					stmt.setLong(1, object.id);
					stmt.setString(2, json);
					stmt.setString(3, StackUtils.throwableToStackString(e));
					stmt.executeUpdate();
				} catch (SQLException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				} finally {
					JDBCUtil.closeConnection(con);
				}
			}
		});
	}
}
