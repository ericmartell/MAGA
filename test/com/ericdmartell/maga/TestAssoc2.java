package com.ericdmartell.maga;

import com.ericdmartell.maga.associations.SimpleMAGAAssociation;

public class TestAssoc2 extends SimpleMAGAAssociation {

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
		return SimpleMAGAAssociation.ONE_TO_MANY;
	}

	@Override
	public String class2Column() {
		return "joinColumn";
	}

}
