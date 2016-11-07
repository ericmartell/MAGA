package com.ericdmartell.simpleorm.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.ericdmartell.simpleorm.annotations.SimpleORMField;
import com.ericdmartell.simpleorm.objects.SimpleORMObject;
import com.esotericsoftware.reflectasm.FieldAccess;

import gnu.trove.map.hash.THashMap;

public class ReflectionUtils {
	// An in-memory cache because inspection is slow.

	private static Map<Class<SimpleORMObject>, Map<String, Integer>> classesToFieldNamesAndFieldIndex = new THashMap<>();
	private static Map<Class, FieldAccess> classToFieldAccess = new THashMap<>();
	private static Map<Class<SimpleORMObject>, Map<String, Class>> classesToFieldNamesAndTypes = new THashMap<>();
	private static Map<Class, List<String>> indexes = new THashMap<>();

	public static Collection<String> getFieldNames(Class clazz) {
		// Lazily populating classesToFieldNamesAndFields since 2016.
		if (!classesToFieldNamesAndFieldIndex.containsKey(clazz)) {
			buildIndex(clazz);
		}

		return classesToFieldNamesAndFieldIndex.get(clazz).keySet();
	}

	public static Class getFieldType(Class clazz, String fieldName) {
		return classesToFieldNamesAndTypes.get(clazz).get(fieldName);
	}
	static int count = 0;
	public static boolean setFieldValue(SimpleORMObject obj, String fieldName, Object value) {
		if (!classesToFieldNamesAndFieldIndex.containsKey(obj.getClass())) {
			buildIndex(obj.getClass());
		}
		Integer index = classesToFieldNamesAndFieldIndex.get(obj.getClass()).get(fieldName);
		if (index == null) {
			return false;
		} else {
			
			count++;
			Class fieldClass = classesToFieldNamesAndTypes.get(obj.getClass()).get(fieldName);
			if (fieldClass == Long.class || fieldClass == long.class) {
				if (fieldClass == long.class && value == null) {
					value = 0L;
				}
				classToFieldAccess.get(obj.getClass()).set(obj, index, ((Number) value).longValue());
			} else if (fieldClass == Integer.class || fieldClass == int.class) {
				if (fieldClass == int.class && value == null) {
					value = 0;
				}
				classToFieldAccess.get(obj.getClass()).set(obj, index, ((Number) value).intValue());
			} else {
				classToFieldAccess.get(obj.getClass()).set(obj, index, value);
			}

			return true;
		}
	}

	public static Object getFieldValue(SimpleORMObject obj, String fieldName) {
		if (!classesToFieldNamesAndFieldIndex.containsKey(obj.getClass())) {
			buildIndex(obj.getClass());
		}
		Integer index = classesToFieldNamesAndFieldIndex.get(obj.getClass()).get(fieldName);
		if (index == null) {
			return -1L;
		} else {
			return classToFieldAccess.get(obj.getClass()).get(obj, index);
		}
	}
	public static List<String> getIndexedColumns(Class clazz) {
		if (!classesToFieldNamesAndFieldIndex.containsKey(clazz)) {
			buildIndex(clazz);
		}
		return indexes.get(clazz);
	}
	private static void buildIndex(Class clazz) {
		FieldAccess access = FieldAccess.get(clazz);
		classToFieldAccess.put(clazz, access);

		Map<String, Integer> fieldNamesToFieldIndex = new THashMap<>();
		Map<String, Class> fieldNamesToType = new THashMap<>();
		List<String> indexedColumns = new ArrayList<>();
		for (Field field : clazz.getDeclaredFields()) {

			if (field.isAnnotationPresent(SimpleORMField.class)) {
				fieldNamesToFieldIndex.put(field.getName(), access.getIndex(field.getName()));
				fieldNamesToType.put(field.getName(), field.getType());
				SimpleORMField anno = field.getAnnotation(SimpleORMField.class);
				if (anno.isIndex()) {
					indexedColumns.add(field.getName());
				}
			}
		}
		fieldNamesToType.put("id", Long.class);
		fieldNamesToFieldIndex.put("id", access.getIndex("id"));
		indexes.put(clazz, indexedColumns);

		classesToFieldNamesAndFieldIndex.put(clazz, fieldNamesToFieldIndex);
		classesToFieldNamesAndTypes.put(clazz, fieldNamesToType);
	}
}
