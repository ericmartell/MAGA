package com.ericdmartell.maga.objects;

import java.util.List;

import com.ericdmartell.maga.MAGA;

public abstract class MAGALoadTemplate<T> {
	
	public MAGALoadTemplate(Object... params) {
		
	}
	
	public abstract List<MAGAObject> run(MAGA simpleORM);
	
	public abstract String getKey();
	
}
