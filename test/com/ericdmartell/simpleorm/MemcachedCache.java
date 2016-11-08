package com.ericdmartell.simpleorm;

import java.util.List;
import java.util.Map;

import com.ericdmartell.cache.Cache;

import net.spy.memcached.MemcachedClient;

public class MemcachedCache implements Cache {
	
	private MemcachedClient memcachedClient;
	
	
	public MemcachedCache(MemcachedClient memcachedClient) {
		this.memcachedClient = memcachedClient;
	}
	
	
	public Object get(String key) {
		return memcachedClient.get(key);
	}
	
	public void set(String key, Object val) {
		memcachedClient.set(key, Integer.MAX_VALUE, val);
	}

	public void flush() {
		memcachedClient.flush();
	}
	public void dirty(String key) {
		memcachedClient.delete(key);
	}

	@Override
	public Map<String, Object> getBulk(List<String> keys) {
		return memcachedClient.getBulk(keys);
	}
}
