package com.chedifier.netsword.cipher;

import java.nio.ByteBuffer;

import com.chedifier.netsword.base.ArrayUtils;
import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.StringUtils;

public class ShiftProguarder implements IProguarder{
	
	private static final String TAG = "ShiftProguarder";

	@Override
	public boolean encode(byte[] origin,ByteBuffer outBuffer) {
		if(origin == null || outBuffer == null) {
			return false;
		}
		
		return encode(origin,0,origin.length,outBuffer);
	}

	@Override
	public boolean decode(byte[] encode,ByteBuffer outBuffer) {
		if(encode == null) {
			return false;
		}
		
		return decode(encode,0,encode.length,outBuffer);
	}

	@Override
	public boolean encode(byte[] origin, int offset, int len,ByteBuffer outBuffer) {
		if(outBuffer == null || origin == null || !ArrayUtils.isValidateRange(origin.length, offset, len)) {
			return false;
		}
		
		if(outBuffer.remaining() < len+1) {
			return false;
		}
		
		byte s = (byte)(1 + (int)(Math.random() * 7));
		byte m = (byte)(((1<<s)-1) << (8-s));
		Log.i(TAG,"shift " + s + " msk " + m);
		
		outBuffer.put(s);
		for(int i=0;i < len && ((offset + i) < origin.length);i++) {
			outBuffer.put((byte)(((origin[offset+i]&0xFF)<<s) | ((origin[offset+i]&m&0xFF)>>(8-s))));
		}
		
		return true;
	}

	@Override
	public boolean decode(byte[] encode, int offset, int len,ByteBuffer outBuffer) {
		if(outBuffer == null || encode == null || !ArrayUtils.isValidateRange(encode.length, offset, len)) {
			Log.e(TAG, "decode>> invalid input.");
			return false;
		}
		
		if(outBuffer.remaining() < len-1) {
			Log.e(TAG, "decode>> not enought out buffer to store decode data");
			return false;
		}
		
		byte s = encode[offset];
		byte m = (byte)(((1<<s)-1));
		Log.i(TAG,"shift " + s + " msk " + m);
		for(int i=1;i<len&&((offset+i)<encode.length);i++) {
			outBuffer.put((byte)(((encode[offset+i]&0xFF)>>s)|((encode[offset+i]&m&0xFF)<<(8-s))));
		}
		
		Log.i(TAG, "decode: " + StringUtils.toRawString(outBuffer.array(),0,outBuffer.position()));
		
		return true;
	}
	
	@Override
	public int estimateDecodeLen(int len) {
		return len-1;
	}
	
	@Override
	public int estimateEncodeLen(int len) {
		return len+1;
	}

}
