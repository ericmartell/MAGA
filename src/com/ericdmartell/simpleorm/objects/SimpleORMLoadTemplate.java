package com.ericdmartell.simpleorm.objects;

import java.util.List;

import com.ericdmartell.simpleorm.SimpleORM;

public abstract class SimpleORMLoadTemplate {
	
	public SimpleORMLoadTemplate() {
		
	}
	
	protected long id;
	
	public abstract List<SimpleORMObject> run(SimpleORM simpleORM, Object... params);
	
	public abstract String getKey();
	
}
