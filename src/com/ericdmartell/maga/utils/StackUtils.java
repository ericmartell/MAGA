package com.ericdmartell.maga.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StackUtils {
	public static String throwableToStackString(Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}
}
