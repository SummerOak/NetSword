package com.chedifier.netsword;

public class StringUtils {

	public static String toRawString(byte[] data,int length) {
		return toRawString(data,0,length);
	}
	
	public static String toRawString(byte[] data,int offset,int length) {
		if(data != null && data.length >= length + offset) {
			StringBuilder sb = new StringBuilder(length << 1);
			for(int i=offset;i<length + offset;i++) {
				sb.append(String.valueOf(data[i] & 0xFF) + "|");
			}
			
			return sb.toString();
		}
		
		return "";
	}
	
	public static String toString(byte[] data,int length) {
		return toString(data,0,length);
	}
	
	public static String toString(byte[] data,int offset,int length) {
		if(data != null && data.length >= length + offset) {
			StringBuilder sb = new StringBuilder(length << 1);
			for(int i=offset;i<length;i++) {
				sb.append((char)(data[i] & 0xFF));
			}
			
			return sb.toString();
		}
		
		return "";
	}
	
}
