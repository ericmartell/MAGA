package com.ericdmartell.maga.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

public class JDBCUtil {
	public static long queries = 0;
	public static long updates = 0;
	public static Connection getConnection(DataSource dataSource) {
		try {
			return dataSource.getConnection();
		} catch (SQLException e) {
			throw new MAGAException(e);
		}
	}

	public static ResultSet executeQuery(Connection connection, String sql, Object... params) {
		queries++;
		try {
			PreparedStatement pstmt = prepareStatmenent(connection, sql);
			if (params.length > 0) {
				if (params[0] instanceof List) {
					List list = (List) params[0];
					for (int i = 0; i < list.size(); i++) {
						pstmt.setObject(i + 1, list.get(i));
					}
				} else {
					for (int i = 0; i < params.length; i++) {
						pstmt.setObject(i + 1, params[i]);
					}
				}
				
			}
			return pstmt.executeQuery();
		} catch (SQLException e) {
			throw new MAGAException(e);
		}
	}

	public static List<Long> executeQueryAndReturnLongs(DataSource dataSource, String sql, Object... params) {
		queries++;
		Connection connection = getConnection(dataSource);
		try {
			PreparedStatement pstmt = prepareStatmenent(connection, sql);
			int index = 1;
			for (Object param : params) {
				pstmt.setObject(index++, param);
			}
			ResultSet rst = pstmt.executeQuery();
			List<Long> ret = new ArrayList<>();
			while (rst.next()) {
				ret.add(rst.getLong(1));
			}
			return ret;
		} catch (SQLException e) {
			throw new MAGAException(e);
		} finally {
			closeConnection(connection);
		}
	}

	public static long executeQueryAndReturnSingleLong(DataSource dataSource, String sql, Object... params) {
		List<Long> longs = executeQueryAndReturnLongs(dataSource, sql, params);
		if (longs.size() != 1) {
			throw new RuntimeException("Returned " + longs.size() + "results.");
		} else {
			return longs.get(0);
		}

	}

	public static List<String> executeQueryAndReturnStrings(DataSource dataSource, String sql, Object... params) {
		queries++;
		Connection connection = getConnection(dataSource);
		try {
			PreparedStatement pstmt = prepareStatmenent(connection, sql);
			int index = 1;
			for (Object param : params) {
				pstmt.setObject(index++, param);
			}
			ResultSet rst = pstmt.executeQuery();
			List<String> ret = new ArrayList<>();
			while (rst.next()) {
				ret.add(rst.getString(1));
			}
			return ret;
		} catch (SQLException e) {
			throw new MAGAException(e);
		} finally {
			closeConnection(connection);
		}
	}

	public static String executeQueryAndReturnSingleString(DataSource dataSource, String sql, Object... params) {
		List<String> strings = executeQueryAndReturnStrings(dataSource, sql, params);
		if (strings.size() != 1) {
			throw new RuntimeException("Returned " + strings.size() + "results.");
		} else {
			return strings.get(0);
		}

	}

	public static void closeConnection(Connection connection) {
		if (connection == null) {
			return;
		}
		try {
			connection.close();
		} catch (Exception e) {

		}
	}

	public static void executeUpdate(String sql, DataSource dataSource, Object... params) {
		updates++;
		Connection connection = getConnection(dataSource);
		try {
			PreparedStatement pstmt = prepareStatmenent(connection, sql);
			for (int i = 0; i < params.length; i++) {
				pstmt.setObject(i + 1, params[i]);
			}
			pstmt.executeUpdate();
		} catch (SQLException e) {
			throw new MAGAException(e);
		} finally {
			closeConnection(connection);
		}

	}

	public static PreparedStatement prepareStatmenent(Connection connection, String sql) {
		try {
			return connection.prepareStatement(sql);
		} catch (SQLException e) {
			throw new MAGAException(e);
		}
	}
}
