package com.ericdmartell.maga;

import com.ericdmartell.maga.annotations.MAGAORMField;
import com.ericdmartell.maga.objects.MAGAObject;

public class Obj2 extends MAGAObject {
	@MAGAORMField
	public String field2;
	
	@MAGAORMField
	public long joinColumn;
	
	@Override
	public String toString() {
		return "Obj2 [field2=" + field2 + ", id=" + id + ", joinColumn=" + joinColumn + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((field2 == null) ? 0 : field2.hashCode());
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
		Obj2 other = (Obj2) obj;
		if (field2 == null) {
			if (other.field2 != null)
				return false;
		} else if (!field2.equals(other.field2))
			return false;
		else if (this.id != other.id) {
			return false;
		} else if (this.joinColumn != other.joinColumn) {
			return false;
		}
		return true;
	}
}
