package com.ericdmartell.maga;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ericdmartell.cache.Cache;
import com.ericdmartell.cache.CacheData;
import com.fasterxml.jackson.databind.util.LRUMap;


public class HashMapCache extends Cache {

	Map<String, Object> data;
	public CacheData cacheData = new CacheData();

	public HashMapCache(int maxEntries) {
		data = new HashMap<>();
	}

	@Override
	public Object getImpl(String key) {
		System.out.println("Get: " + key);
		Object ret = cloneObject(data.get(key));
		if (ret != null) {
			cacheData.hits++;
		} else {
			cacheData.misses++;
		}
		return ret;
	}

	@Override
	public void setImpl(String key, Object val) {
		System.out.println("Get: " + key);
		cacheData.sets++;
		data.put(key, cloneObject(val));

	}

	@Override
	public void flushImpl() {
		System.out.println("Flush");
		data.clear();
	}

	@Override
	public void dirtyImpl(String key) {
		System.out.println("Dirty: " + key);
		cacheData.dirties++;
		data.put(key, null);

	}

	@Override
	public Map<String, Object> getBulkImpl(List<String> keys) {
		System.out.println("Get Bulk: " + keys);
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
