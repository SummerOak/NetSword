package com.chedifier.netsword.base;

public class Log {
	
	private static int sLogLevel = 0;
	
	public static final void setLogLevel(int level) {
		sLogLevel = 0;
	}
	
	private static final String getTimeFormat() {
		return getTimeFormat(System.currentTimeMillis());
	}
	
	private static final String getTimeFormat(long time) {
		long min = time/(1000*60);
		long sec = (time%(1000*60)) / 1000;
		long mils = (time%1000);
		return min + "." + sec + "." + mils;
	}

	public static final void i(String tag,String content) {
		if(sLogLevel > 2) {
			System.out.println("tid[" + Thread.currentThread().getId() + "] " + getTimeFormat() + " : " + tag + " >> " + content);
		}
	}
	
	public static final void d(String tag,String content) {
		if(sLogLevel > 1) {
			System.out.println("tid[" + Thread.currentThread().getId() + "] " + getTimeFormat() + " : " + tag + " >> " + content);
		}
	}
	
	public static final void e(String tag,String content) {
		System.out.println("tid[" + Thread.currentThread().getId() + "] " + getTimeFormat() + " : " + tag + " >> " + content);
	}
	
	public static final void r(String tag,String content) {
		if(sLogLevel >= 0) {
			System.out.println("tid[" + Thread.currentThread().getId() + "] " + getTimeFormat() + " : " + tag + " >> " + content);
		}
	}
	
	public static final void t(String tag,String content) {
		System.out.println("tid[" + Thread.currentThread().getId() + "] " + getTimeFormat() + " : " + tag + " >> " + content);
	}
}
