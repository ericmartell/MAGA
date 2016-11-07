package com.ericdmartell.cache;

public class CacheData {
	public long hits;
	public long misses;
	public long sets;
	public long dirties;
	@Override
	public String toString() {
		return "CacheData [hits=" + hits + ", misses=" + misses + ", sets=" + sets + ", dirties=" + dirties + "]";
	}
	
}
