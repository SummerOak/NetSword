package com.chedifier.netsword.base;

public class StringUtils {
	
	public static boolean isEmpty(String s) {
		return s == null || s.equals("");
	}
	
	public static String toRawString(byte[] data) {
		if(data != null) {
			return toRawString(data,data.length);
		}
		return "";
	}

	public static String toRawString(byte[] data,int length) {
		return toRawString(data,0,length);
	}
	
	public static String toRawString(byte[] data,int offset,int length) {
		if(data != null && ArrayUtils.isValidateRange(data.length, offset, length)) {
			StringBuilder sb = new StringBuilder(length << 1);
			for(int i=0;i<length;i++) {
				sb.append(String.valueOf(data[offset+i] & 0xFF) + "|");
			}
			
			return sb.toString();
		}
		
		return "";
	}
	
	public static String toString(byte[] data) {
		if(data != null) {
			return toString(data,data.length);
		}
		return "";
	}
	
	public static String toString(byte[] data,int length) {
		return toString(data,0,length);
	}
	
	public static String toString(byte[] data,int offset,int length) {
		if(data != null && ArrayUtils.isValidateRange(data.length, offset, length)) {
			StringBuilder sb = new StringBuilder(length << 1);
			for(int i=0;i<length;i++) {
				sb.append((char)(data[offset+i] & 0xFF));
			}
			
			return sb.toString();
		}
		
		return "";
	}
	
	public static int parseInt(String s,int def) {
		try {
			return Integer.valueOf(s);
		}catch(Throwable t) {
			ExceptionHandler.handleException(t);
		}
		return def;
	}
	
	public static long parseLong(String s,long def) {
		try {
			return Long.valueOf(s);
		}catch(Throwable t) {
			ExceptionHandler.handleException(t);
		}
		return def;
	}
}
