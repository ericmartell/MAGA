package com.ericdmartell.simpleorm;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ericdmartell.cache.Cache;
import com.ericdmartell.cache.CacheData;


public class HashMapCache implements Cache {

	Map<String, Object> data = new HashMap<>();
	public CacheData cacheData = new CacheData();

	@Override
	public Object get(String key) {
		Object ret = cloneObject(data.get(key));
		if (ret != null) {
			cacheData.hits++;
		} else {
			cacheData.misses++;
		}
		return ret;
	}

	@Override
	public void set(String key, Object val) {
		cacheData.sets++;
		data.put(key, cloneObject(val));

	}

	@Override
	public void flush() {
		data = new HashMap<>();
	}

	@Override
	public void dirty(String key) {
		cacheData.dirties++;
		data.remove(key);

	}

	@Override
	public Map<String, Object> getBulk(List<String> keys) {
		Map<String, Object> ret = new HashMap<>();
		for (String key : keys) {
			if (data.get(key) != null) {
				ret.put(key, cloneObject(data.get(key)));
			}
		}
		cacheData.hits += ret.keySet().size();
		cacheData.misses += keys.size() - ret.keySet().size();
		return ret;

	}

	private static Object cloneObject(Object obj) {
		if (obj == null) {
			return null;
		}

		Object clone = null;
		try {
			clone = obj.getClass().newInstance();
		} catch (InstantiationException | IllegalAccessException e1) {
			
		}
		for (Field field : obj.getClass().getDeclaredFields()) {
			try {
				field.setAccessible(true);
				field.set(clone, field.get(obj));
			} catch (Exception e) {
			}
		}
		for (Field field : obj.getClass().getFields()) {
			try {
				field.setAccessible(true);
				field.set(clone, field.get(obj));
			} catch (Exception e) {
			}
		}
		return clone;

	}
}
