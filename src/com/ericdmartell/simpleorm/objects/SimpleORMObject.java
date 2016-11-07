package com.ericdmartell.simpleorm.objects;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.ericdmartell.simpleorm.associations.SimpleORMAssociation;
import com.ericdmartell.simpleorm.utils.SimpleORMException;

public abstract class SimpleORMObject implements Serializable, Cloneable {
	public long id;
	
	
	public SimpleORMObject clone() {
		try {
			return (SimpleORMObject) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new SimpleORMException(e);
		}
	}
	
	public Map<SimpleORMAssociation, List<SimpleORMObject>> templateAssociations = null;
	
}
