package com.chedifier.netsword.cipher;

public class PaddingProguarder implements IProguarder{

	private TYPE mType = TYPE.HEAD;
	
	public PaddingProguarder() {
		
	}
	
	@Override
	public byte[] encode(byte[] origin) {
		if(origin != null) {			
			return encode(origin,0,origin.length);
		}
		
		return null;
	}

	@Override
	public byte[] decode(byte[] encode) {
		if(encode != null) {
			return decode(encode,0,encode.length);
		}
		
		return null;
	}

	@Override
	public byte[] encode(byte[] origin, int offset, int len) {
		if(origin == null || len <= 0 || offset < 0 || origin.length <= 0 || (origin.length < (len + offset))) {
			return origin;
		}
		
		byte[] result;
		switch(mType) {
			case HEAD:{
				int p = 1 + (int)(Math.random() * 8);
				result = new byte[p+len+1];
				result[0] = (byte)(p & 0xFF);
				for(int i=0;i<p;i++) {
					result[i+1] = (byte)(Math.random() * 256);
				}
				
				System.arraycopy(origin, offset, result, p+1, len);
				
				return result;
			}
		}
		return null;
	}

	@Override
	public byte[] decode(byte[] encode, int offset, int len) {
		if(encode == null || offset < 0 || len <= 1 || encode.length <= 0 || (encode.length < (len + offset))) {
			return encode;
		}
		
		byte[] result = null;
		
		switch(mType) {
			case HEAD:{
				int p = encode[offset]&0xFF;
				if(p >= 0 && p < len) {
					result = new byte[len - p - 1];
					System.arraycopy(encode, offset + p + 1, result, 0, len-p-1);
				}
				
				return result;
			}
		}
		
		return null;
	}
	
	public enum TYPE{
		HEAD,
	}
	
}
