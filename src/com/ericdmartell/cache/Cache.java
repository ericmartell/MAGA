package com.ericdmartell.cache;

import java.util.List;
import java.util.Map;

public abstract class Cache {
	public long hits = 0;
	public long misses = 0;
	public long sets = 0;
	public long flushes = 0;
	public long dirties = 0;
	public long bulkHits = 0;
	public long bulkMisses = 0;
	public long bulkTrips = 0;
	
	public Object get(String key) {
		Object ret = getImpl(key);
		if (ret != null) {
			hits++;
		} else {
			misses++;
		}
		return ret;
	}
	public void set(String key, Object val) {
		sets++;
		setImpl(key, val);
	}
	public void flush() {
		flushes++;
		flushImpl();
	}
	public void dirty(String key) {
		dirties++;
		dirtyImpl(key);
	}
	public Map<String, Object> getBulk(List<String> keys) {
		Map<String, Object> ret = getBulkImpl(keys);
		bulkTrips++;
		bulkHits += ret.size();
		bulkMisses += keys.size() - ret.size();
		return ret;
	}
	
	public abstract Object getImpl(String key);
	public abstract void setImpl(String key, Object val);
	public abstract void flushImpl();
	public abstract void dirtyImpl(String key);
	public abstract Map<String, Object> getBulkImpl(List<String> keys);
}
