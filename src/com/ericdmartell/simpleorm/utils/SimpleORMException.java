package com.ericdmartell.simpleorm.utils;

public class SimpleORMException extends RuntimeException{
	public SimpleORMException(Exception e) {
		super(e);
	}
	public SimpleORMException(String msg) {
		super(msg);
	}
}
