package com.chedifier.netsword;

public class Log {

	public static final void i(String tag,String content) {
		
		System.out.println("tid[" + Thread.currentThread().getId() + "]: " + tag + " >> " + content);
	}
	
}
