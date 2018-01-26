package com.chedifier.netsword;

public class Log {
	
	private static int sLogLevel = 10;
	
	public static final void setLogLevel(int level) {
		sLogLevel = 10;
	}

	public static final void i(String tag,String content) {
		if(sLogLevel > 2) {
			System.out.println("tid[" + Thread.currentThread().getId() + "]: " + tag + " >> " + content);
		}
	}
	
	public static final void d(String tag,String content) {
		if(sLogLevel > 1) {
			System.out.println("tid[" + Thread.currentThread().getId() + "]: " + tag + " >> " + content);
		}
	}
	
	public static final void e(String tag,String content) {
		System.out.println("tid[" + Thread.currentThread().getId() + "]: " + tag + " >> " + content);
	}
	
	public static final void r(String tag,String content) {
		System.out.println("tid[" + Thread.currentThread().getId() + "]: " + tag + " >> " + content);
	}
}
