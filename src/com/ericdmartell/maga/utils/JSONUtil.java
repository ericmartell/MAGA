package com.ericdmartell.maga.utils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;



public class JSONUtil {
	
	private static ObjectMapper objectMapper;
	static {
		objectMapper = new ObjectMapper();
		objectMapper.enable(DeserializationFeature.USE_LONG_FOR_INTS);
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> readFileToString(File file) {
		try {
			return objectMapper.readValue(file, Map.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String serializableToString(Object serializable) {
		StringWriter writer = new StringWriter();
		try {
			objectMapper.writeValue(writer, serializable);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return writer.toString();
	}
	
	public static <T> T stringToObject(String string, Class<T> clazz) {
		StringWriter writer = new StringWriter();
		try {
			return objectMapper.readValue(string, clazz);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String mapToString(Map map) {
		StringWriter writer = new StringWriter();
		try {
			objectMapper.writeValue(writer, map);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return writer.toString();
	}

	public static Map<String, Object> stringToMap(String jsonString) {
		try {
			return objectMapper.readValue(jsonString, Map.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public static String listToString(List list) {
		StringWriter writer = new StringWriter();
		try {
			objectMapper.writeValue(writer, list);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return writer.toString();
	}

	public static List stringToList(String jsonString) {
		try {
			return objectMapper.readValue(jsonString, List.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
}
