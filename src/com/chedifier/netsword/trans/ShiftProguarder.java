package com.chedifier.netsword.trans;

import com.chedifier.netsword.base.ArrayUtils;
import com.chedifier.netsword.base.Log;

public class ShiftProguarder implements IProguarder{
	
	private static final String TAG = "ShiftProguarder";

	@Override
	public byte[] encode(byte[] origin) {
		if(origin == null) {
			return origin;
		}
		
		return encode(origin,0,origin.length);
	}

	@Override
	public byte[] decode(byte[] encode) {
		if(encode == null) {
			return encode;
		}
		
		return decode(encode,0,encode.length);
	}

	@Override
	public byte[] encode(byte[] origin, int offset, int len) {
		if(origin == null || !ArrayUtils.isValidateRange(origin.length, offset, len)) {
			return origin;
		}
		
		byte[] result = new byte[len + 1];
		byte s = 6;//(byte)(1 + (int)(Math.random() * 7));
		byte m = (byte)(((1<<s)-1) << (8-s));
		Log.i(TAG,"shift " + s + " msk " + m);
		
		result[0] = s;
		for(int i=0;i < len && ((offset + i) < origin.length);i++) {
			result[i+1] = (byte)(((origin[offset+i]&0xFF)<<s) | ((origin[offset+i]&m&0xFF)>>(8-s)));
		}
		
		return result;
	}

	@Override
	public byte[] decode(byte[] encode, int offset, int len) {
		if(encode == null || !ArrayUtils.isValidateRange(encode.length, offset, len)) {
			return null;
		}
		
		byte s = encode[offset];
		byte m = (byte)(((1<<s)-1));
		Log.i(TAG,"shift " + s + " msk " + m);
		byte[] result = new byte[len-1];
		for(int i=1;i<len&&((offset+i)<encode.length);i++) {
			result[i-1] = (byte)(((encode[offset+i]&0xFF)>>s)|((encode[offset+i]&m&0xFF)<<(8-s)));
		}
		
		return result;
	}

}
