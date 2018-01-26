package com.chedifier.netsword;

public class ArrayUtils {

	public static final boolean isValidateRange(int ttlen,int offset,int len) {
		if(ttlen <= 0 || offset <0 || len <= 0 || (offset + len) > ttlen) {
			return false;
		}
		
		return true;
	}
	
}
