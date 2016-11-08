package com.ericdmartell.maga;

import com.ericdmartell.maga.associations.MAGAAssociation;

public class TestAssoc extends MAGAAssociation {

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
		return MAGAAssociation.MANY_TO_MANY;
	}

	@Override
	public String class2Column() {
		return "joinColumn";
	}

}
