package com.ericdmartell.maga.utils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ericdmartell.maga.annotations.MAGAORMField;
import com.ericdmartell.maga.objects.MAGAObject;

import gnu.trove.map.hash.THashMap;

public class ReflectionUtils {
	// An in-memory cache because inspection is slow.

	private static Map<Class<MAGAObject>, Map<String, Field>> classesToFieldNamesAndFields = new THashMap<>();
	private static Map<Class<MAGAObject>, Map<String, Class>> classesToFieldNamesAndTypes = new THashMap<>();
	private static Map<Class, List<String>> indexes = new THashMap<>();
	public static Set<Class> standardClasses = new HashSet<>(Arrays.asList(new Class[] {
		int.class, Integer.class, BigDecimal.class, String.class, long.class, Long.class
	}));
	
	
	public static Collection<String> getFieldNames(Class clazz) {
		// Lazily populating classesToFieldNamesAndFields since 2016.
		if (!classesToFieldNamesAndTypes.containsKey(clazz)) {
			buildIndex(clazz);
		}

		return classesToFieldNamesAndTypes.get(clazz).keySet();
	}

	public static Class getFieldType(Class clazz, String fieldName) {
		if (!classesToFieldNamesAndTypes.containsKey(clazz)) {
			buildIndex(clazz);
		}
		return classesToFieldNamesAndTypes.get(clazz).get(fieldName);
	}
	
	
			
	public static boolean setFieldValue(MAGAObject obj, String fieldName, Object value) {
		if (!classesToFieldNamesAndTypes.containsKey(obj.getClass())) {
			buildIndex(obj.getClass());
		}
		try {
			
			if (classesToFieldNamesAndFields.get(obj.getClass()).get(fieldName) == null) {
				return false;
			}
			
			if (getFieldType(obj.getClass(), fieldName).equals(BigDecimal.class)) {
				if (value == null) {
					classesToFieldNamesAndFields.get(obj.getClass()).get(fieldName).set(obj, value);
				} else {
					classesToFieldNamesAndFields.get(obj.getClass()).get(fieldName).set(obj, new BigDecimal(value + ""));
				}
			} else if (getFieldType(obj.getClass(), fieldName).equals(long.class) || getFieldType(obj.getClass(), fieldName).equals(Long.class)) {
				if (value == null) {
					value = 0L;
				}
				classesToFieldNamesAndFields.get(obj.getClass()).get(fieldName).set(obj, ((Number) value).longValue());
			} else if (getFieldType(obj.getClass(), fieldName).equals(int.class) || getFieldType(obj.getClass(), fieldName).equals(Integer.class)) {
				if (value == null) {
					value = 0;
				}
				classesToFieldNamesAndFields.get(obj.getClass()).get(fieldName).set(obj, ((Number) value).intValue());
			} else if (getFieldType(obj.getClass(), fieldName).equals(String.class) && value instanceof Number) {
				classesToFieldNamesAndFields.get(obj.getClass()).get(fieldName).set(obj, value + "");
			} else if ((getFieldType(obj.getClass(), fieldName).equals(Boolean.class) || getFieldType(obj.getClass(), fieldName).equals(boolean.class)) && value instanceof String) {
				classesToFieldNamesAndFields.get(obj.getClass()).get(fieldName).set(obj, value == null ? false : ("1".equals(value + "")));
			} else if (value != null && !standardClasses.contains(getFieldType(obj.getClass(), fieldName)) && Collection.class.isAssignableFrom(getFieldType(obj.getClass(), fieldName))) { 
				classesToFieldNamesAndFields.get(obj.getClass()).get(fieldName).set(obj, JSONUtil.stringToList(value + ""));
			} else if (value != null && !standardClasses.contains(getFieldType(obj.getClass(), fieldName))) {
				classesToFieldNamesAndFields.get(obj.getClass()).get(fieldName).set(obj, JSONUtil.stringToObject(value + "", getFieldType(obj.getClass(), fieldName)));
			} else {
				classesToFieldNamesAndFields.get(obj.getClass()).get(fieldName).set(obj, value);
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new MAGAException(e);
		}

		return true;

	}
	
	
	public static Object getFieldValue(MAGAObject obj, String fieldName) {
		try {
		if (!classesToFieldNamesAndTypes.containsKey(obj.getClass())) {
			buildIndex(obj.getClass());
		}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

		Object ret;
		try {
			if (classesToFieldNamesAndFields.get(obj.getClass()).get(fieldName) == null) {
				ret = null;
			} else {
				ret = classesToFieldNamesAndFields.get(obj.getClass()).get(fieldName).get(obj);
				if (ret != null && !standardClasses.contains(getFieldType(obj.getClass(), fieldName))) {
					if (Collection.class.isAssignableFrom(getFieldType(obj.getClass(), fieldName))) {
						ret = JSONUtil.listToString((List) ret);
					} else {
						ret = JSONUtil.serializableToString(ret);
					}
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new MAGAException(e);
		}
		if (getFieldType(obj.getClass(), fieldName) == null) {
			return ret;
		} else if (ret == null && (getFieldType(obj.getClass(), fieldName).equals(long.class) || getFieldType(obj.getClass(), fieldName).equals(Long.class))) {
			return 0L;
		} else if (ret == null && (getFieldType(obj.getClass(), fieldName).equals(int.class) || getFieldType(obj.getClass(), fieldName).equals(Integer.class))) {
			return 0;
		} else if (ret == null && (getFieldType(obj.getClass(), fieldName).equals(Boolean.class) || getFieldType(obj.getClass(), fieldName).equals(boolean.class))) {
			return false;
		}
		return ret;

	}

	public static List<String> getIndexedColumns(Class clazz) {
		if (!classesToFieldNamesAndTypes.containsKey(clazz)) {
			buildIndex(clazz);
		}
		return indexes.get(clazz);
	}

	private static void buildIndex(Class clazz) {
		
		Map<String, Field> fieldNamesToField = new THashMap<>();
		Map<String, Class> fieldNamesToType = new THashMap<>();
		List<String> indexedColumns = new ArrayList<>();
		for (Field field : clazz.getDeclaredFields()) {
			
			field.setAccessible(true);
			if (field.isAnnotationPresent(MAGAORMField.class)) {
				fieldNamesToField.put(field.getName(), field);
				fieldNamesToType.put(field.getName(), field.getType());
				MAGAORMField anno = field.getAnnotation(MAGAORMField.class);
				if (anno.isIndex()) {
					indexedColumns.add(field.getName());
				}
			}
		}
		fieldNamesToType.put("id", String.class);
		try {
			fieldNamesToField.put("id", clazz.getField("id"));
		} catch (NoSuchFieldException | SecurityException e) {
			throw new MAGAException(e);
		}
		indexes.put(clazz, indexedColumns);

		classesToFieldNamesAndFields.put(clazz, fieldNamesToField);
		classesToFieldNamesAndTypes.put(clazz, fieldNamesToType);
	}
}
