package com.chedifier.netsword.cipher;

import java.nio.ByteBuffer;

import com.chedifier.netsword.base.Log;

public class PaddingProguarder implements IProguarder{
	private static final String TAG = "PaddingProguarder";
	
	
	private static final int MAX_PADDING = 255;
	private TYPE mType = TYPE.HEAD;
	
	public PaddingProguarder() {
		
	}
	
	@Override
	public boolean encode(byte[] origin,ByteBuffer outBuffer) {
		if(origin != null) {			
			return encode(origin,0,origin.length,outBuffer);
		}
		
		return false;
	}

	@Override
	public boolean decode(byte[] encode,ByteBuffer outBuffer) {
		if(encode != null) {
			return decode(encode,0,encode.length,outBuffer);
		}
		
		return false;
	}

	@Override
	public boolean encode(byte[] origin, int offset, int len,ByteBuffer outBuffer) {
		if(outBuffer == null || origin == null || len <= 0 || offset < 0 || origin.length <= 0 || (origin.length < (len + offset))) {
			return false;
		}
		
		switch(mType) {
			case HEAD:{
				int p = 1 + (int)(Math.random() * MAX_PADDING);
				if(outBuffer.remaining() < p+len+1) {
					return false;
				}
				outBuffer.put((byte)(p & 0xFF));
				for(int i=0;i<p;i++) {
					outBuffer.put((byte)(Math.random() * 256));
				}
				
				outBuffer.put(origin,offset,len);
				
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean decode(byte[] encode, int offset, int len,ByteBuffer outBuffer) {
		if(outBuffer == null || encode == null || offset < 0 || len <= 1 || encode.length <= 0 || (encode.length < (len + offset))) {
			Log.e(TAG, "decode>> invalid input.");
			return false;
		}
		
		switch(mType) {
			case HEAD:{
				int p = encode[offset]&0xFF;
				if(p >= 0 && p < len) {
					if(outBuffer.remaining() < (len-p-1)) {
						return false;
					}
					outBuffer.put(encode,offset+p+1,len-p-1);
					return true;
				}
				
				return false;
			}
		}
		
		return false;
	}
	
	@Override
	public int estimateDecodeLen(int len) {
		return len;
	}
	
	@Override
	public int estimateEncodeLen(int len) {
		return len+MAX_PADDING;
	}
	
	public enum TYPE{
		HEAD,
	}
	
}
