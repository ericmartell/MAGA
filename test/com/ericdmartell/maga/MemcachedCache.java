package com.ericdmartell.maga;

import java.util.List;
import java.util.Map;

import com.ericdmartell.cache.Cache;

import net.spy.memcached.MemcachedClient;

public class MemcachedCache extends Cache {
	
	private MemcachedClient memcachedClient;
	
	
	public MemcachedCache(MemcachedClient memcachedClient) {
		this.memcachedClient = memcachedClient;
	}
	
	
	public Object getImpl(String key) {
		return memcachedClient.get(key);
	}
	
	public void setImpl(String key, Object val) {
		memcachedClient.set(key, Integer.MAX_VALUE, val);
	}

	public void flushImpl() {
		memcachedClient.flush();
	}
	public void dirtyImpl(String key) {
		memcachedClient.delete(key);
	}

	@Override
	public Map<String, Object> getBulkImpl(List<String> keys) {
		return memcachedClient.getBulk(keys);
	}
}
