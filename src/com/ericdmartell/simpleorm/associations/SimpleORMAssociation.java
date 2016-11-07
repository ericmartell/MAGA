package com.ericdmartell.simpleorm.associations;

public abstract class SimpleORMAssociation {
	public static final int MANY_TO_MANY = 1;
	public static final int ONE_TO_MANY = 2;
	
	public abstract Class class1();
	public abstract Class class2();
	public abstract int type();
	public abstract String class2Column();
	
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((class1() == null) ? 0 : class1().hashCode());
		result = prime * result + ((class2() == null) ? 0 : class2().hashCode());
		result = prime * result + ((class2Column() == null) ? 0 : class2Column().hashCode());
		result = prime * result + type();
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
		SimpleORMAssociation other = (SimpleORMAssociation) obj;
		if (class1() == null) {
			if (other.class1() != null)
				return false;
		} else if (!class1().equals(other.class1()))
			return false;
		if (class2() == null) {
			if (other.class2() != null)
				return false;
		} else if (!class2().equals(other.class2()))
			return false;
		if (class2Column() == null) {
			if (other.class2Column() != null)
				return false;
		} else if (!class2Column().equals(other.class2Column()))
			return false;
		if (type() != other.type())
			return false;
		return true;
	}
	
	
	
	
	
	
}
