package com.ericdmartell.simpleorm;

import com.ericdmartell.simpleorm.annotations.SimpleORMField;
import com.ericdmartell.simpleorm.objects.SimpleORMObject;

public class Obj1 extends SimpleORMObject {
	@SimpleORMField
	public String field1;
	
	@Override
	public String toString() {
		return "Obj1 [field1=" + field1 + ", id=" + id + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((field1 == null) ? 0 : field1.hashCode());
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
		Obj1 other = (Obj1) obj;
		if (field1 == null) {
			if (other.field1 != null)
				return false;
		} else if (!field1.equals(other.field1))
			return false;
		else if (this.id != other.id) {
			return false;
		}
		return true;
	}
	
}
