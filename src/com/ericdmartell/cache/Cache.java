package com.ericdmartell.cache;

import java.util.List;
import java.util.Map;

public interface Cache {
	public Object get(String key);
	public void set(String key, Object val);
	public void flush();
	public void dirty(String key);
	public Map<String, Object> getBulk(List<String> keys);
}
