package com.ericdmartell.maga.objects;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.ericdmartell.maga.associations.MAGAAssociation;
import com.ericdmartell.maga.utils.MAGAException;

public abstract class MAGAObject implements Serializable, Cloneable {
	public long id;
	
	
	public MAGAObject clone() {
		try {
			return (MAGAObject) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new MAGAException(e);
		}
	}
	
	public Map<MAGAAssociation, List<MAGAObject>> templateAssociations = null;
	
}
