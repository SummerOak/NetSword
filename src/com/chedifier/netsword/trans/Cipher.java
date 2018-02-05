package com.chedifier.netsword.trans;

import java.nio.ByteBuffer;

import com.chedifier.netsword.base.ArrayUtils;
import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.StringUtils;
import com.chedifier.netsword.socks5.Configuration;

public class Cipher {
	private static final String TAG = "Cipher";
	private static IProguarder sP = new ShiftProguarder();
	private static int sBlockSize = Configuration.getConfigurationInt(Configuration.KEY_BLOCK_SIZE, 255);
	private static int sChunkSize = Configuration.getConfigurationInt(Configuration.KEY_CHUNK_SIZE,255<<2);
	
	public static byte[] encrypt(byte[] origin,int offset ,int len) {
		if(origin != null && ArrayUtils.isValidateRange(origin.length, offset, len)) {
			
			ByteBuffer buffer = ByteBuffer.allocate(len<<2);
			int s = 0;
			while(s < len) {
				int l = len - s;
				if(l > sChunkSize) {
					l = sChunkSize;
				}
				
				byte[] encrypted = sP.encode(origin, offset+s, l);
				if(encrypted == null || encrypted.length <= 0) {
					Log.e(TAG, "encrypt>> block failed." + StringUtils.toRawString(origin,offset+s,l));
					return null;
				}
				
				byte[] pack = pack(encrypted);
				if(pack != null && pack.length > 0) {
					buffer.put(pack);
				}else {
					Log.e(TAG, "encrypt>>>pack chunk failed:" + StringUtils.toRawString(encrypted));
				}
				
				s += l;
			}
			
			byte[] result = new byte[buffer.position()];
			System.arraycopy(buffer.array(), 0, result, 0, result.length);
			return result;
		}else {
			Log.e(TAG, "encrypt>> invalidate input.");
		}
		
		return null;
	}
	
	public static DecryptResult decrypt(byte[] packs,int offset,int len) {
		if(packs == null || !ArrayUtils.isValidateRange(packs.length, offset, len)) {
			Log.e(TAG, "decrypt>>> invalidate input.");
			return null;
		}
		
		ByteBuffer buffer = ByteBuffer.allocate(len);
		byte[] data = new byte[sChunkSize/sBlockSize + sChunkSize + 1];
		
		int l = 0;//location of packs
		while(l < len) {
			int d = 0;int b = 0;
			while(l < len) {
				d = (packs[offset+l]&0xFF);
				if(l+d >= len) {
					break;
				}
				
//				Log.d(TAG, "sChunkSize " + sChunkSize + " l " + l + " b " + b + " d " + d + " len " + len);
				System.arraycopy(packs, ++l + offset, data, b, d);
				l += d; b += d;
				if(d < sBlockSize) {
					break;
				}
			}
			
			if(b > 0 && d < sBlockSize) {
				byte[] temp = sP.decode(data, 0, b);
				if(temp == null || temp.length <= 0) {
					Log.e(TAG, "decrypt>>> decode block failed: " + StringUtils.toRawString(data,0,b));
					return null;
				}
				
				buffer.put(temp);
				continue;
			}
			
			break;
		}
		
		if(buffer.position() > 0) {
			DecryptResult result = new DecryptResult();
			result.decryptLen = l;
			result.origin = new byte[buffer.position()];
			System.arraycopy(buffer.array(), 0, result.origin, 0, result.origin.length);
			return result;
		}
		
		Log.e(TAG, "decrypt>>> decode packs failed. " + StringUtils.toRawString(packs,offset,len));
		
		return null;
	}
	
	private static byte[] pack(byte[] origin) {
		if(origin != null) {
			return pack(origin,0,origin.length);
		}
		return null;
	}
	
	private static byte[] pack(byte[] origin,int offset,int len) {
		if(origin != null && ArrayUtils.isValidateRange(origin.length, offset, len)) {
			byte[] packed = new byte[(len/sBlockSize) + 1 + len];
			int l = len;
			int i = 0;
			while(l > 0) {
				int b = l>sBlockSize?sBlockSize:l;
				packed[i++] = (byte)(b&0xFF);
				System.arraycopy(origin, offset+len-l, packed, i, b);
				l -= b;
				i += b;
			}
			
			return packed;
		}
		
		return null;
	}
	
	public static byte[] encrypt(byte[] origin) {
		return encrypt(origin,0,origin.length);
	}
	
	public static DecryptResult decrypt(byte[] code) {
		return decrypt(code, 0, code.length);
	}
	
	public static class DecryptResult{
		public byte[] origin;
		public int decryptLen;
	}
}
