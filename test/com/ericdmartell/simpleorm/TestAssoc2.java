package com.ericdmartell.simpleorm;

import com.ericdmartell.simpleorm.associations.SimpleORMAssociation;

public class TestAssoc2 extends SimpleORMAssociation {

	@Override
	public Class class1() {
		return Obj1.class;
	}

	@Override
	public Class class2() {
		return Obj2.class;
	}

	@Override
	public int type() {
		return SimpleORMAssociation.ONE_TO_MANY;
	}

	@Override
	public String class2Column() {
		return "joinColumn";
	}

}
