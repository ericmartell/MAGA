package com.ericdmartell.maga.objects;

import java.util.List;

import com.ericdmartell.maga.MAGA;

public abstract class MAGALoadTemplate {
	
	public MAGALoadTemplate() {
		
	}
	
	protected long id;
	
	public abstract List<MAGAObject> run(MAGA simpleORM, Object... params);
	
	public abstract String getKey();
	
}
