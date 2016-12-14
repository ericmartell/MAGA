package com.ericdmartell.maga.objects;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.ericdmartell.maga.associations.MAGAAssociation;
import com.ericdmartell.maga.utils.MAGAException;

public abstract class MAGAObject implements Serializable, Cloneable {
	public String id;
	
	
	public MAGAObject clone() {
		try {
			return (MAGAObject) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new MAGAException(e);
		}
	}
	
	public Map<MAGAAssociation, List<MAGAObject>> templateAssociations = null;


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MAGAObject other = (MAGAObject) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
	
	
}
