package com.ericdmartell.simpleorm;

import java.math.BigDecimal;

import com.ericdmartell.simpleorm.annotations.SimpleORMField;
import com.ericdmartell.simpleorm.objects.SimpleORMObject;

public class Obj3 extends SimpleORMObject {
	@SimpleORMField
	public String field3;

	@SimpleORMField
	public BigDecimal val;

	@Override
	public String toString() {
		return "Obj3 [field3=" + field3 + ", id=" + id + "]";
	}
	
}
