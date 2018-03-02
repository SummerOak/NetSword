package com.chedifier.netsword.cipher;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.chedifier.netsword.base.ArrayUtils;
import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.StringUtils;
import com.chedifier.netsword.memory.ByteBufferPool;

public class Cipher {
	private static final String TAG = "Cipher";
	
	private static List<IProguarder> sPs = new ArrayList<>();
	
	static{
		sPs.add(new ShiftProguarder());
		sPs.add(new PaddingProguarder());
	}
	
	private static final int BLOCK_SIZE = 255;
	
	/**
	 * encrypt origin to result 
	 * @param origin
	 * @param offset
	 * @param len
	 * @param chunkSize
	 * @param result
	 * @return the len of data be encrypted
	 */
	public static int encrypt(byte[] origin,int offset ,int len,int chunkSize,ByteBuffer outBuffer) {
		int s = 0;
		if(origin != null && ArrayUtils.isValidateRange(origin.length, offset, len) && outBuffer != null) {
			
			int estLen = estimateEncryptLen(len,chunkSize);
			if(estLen > outBuffer.remaining()) {
				Log.e(TAG, "encrypt failed,not enought buffer to store result");
				return s;
			}
			
			ByteBuffer encodedChunk = ByteBufferPool.obtain(estLen);
			ByteBuffer packedChunk = ByteBufferPool.obtain(estimatePackLen(estLen));
			while(s < len) {
				int l = len - s;
				if(l > chunkSize) {
					l = chunkSize;
				}
				
				encodedChunk.clear();
				boolean encodeResult = proguard(sPs, origin, offset+s, l, encodedChunk);
				if(!encodeResult) {
					Log.e(TAG, "encrypt>> block failed." + StringUtils.toRawString(origin,offset+s,l));
					break;
				}
				
				packedChunk.clear();
				boolean packResult = pack(encodedChunk.array(),0,encodedChunk.position(),packedChunk);
				if(packResult) {
					packedChunk.flip();
					outBuffer.put(packedChunk);
					s += l;
				}else {
					Log.e(TAG, "encrypt>>>pack chunk failed:" + StringUtils.toRawString(encodedChunk.array(),0,encodedChunk.position()));
					break;
				}
			}
			
			ByteBufferPool.recycle(packedChunk);
			ByteBufferPool.recycle(encodedChunk);
			
//			Log.t(TAG, "encrypt>> data: " + "len " + result.length + " > " + StringUtils.toRawString(result));
			return s;
		}else {
			Log.e(TAG, "encrypt>> invalidate input.");
		}
		
		return s;
	}
	
	/**
	 * 
	 * @param packs
	 * @param offset
	 * @param len
	 * @param chunkSize
	 * @param outBuffer
	 * @return the len of date be decrypted in packs
	 */
	public static int decrypt(byte[] packs,int offset,int len,int chunkSize,ByteBuffer outBuffer) {
		if(packs == null || !ArrayUtils.isValidateRange(packs.length, offset, len)) {
			Log.e(TAG, "decrypt>>> invalidate input.");
			return 0;
		}
		
		if(outBuffer != null && outBuffer.remaining() < estimateDecryptLen(len, chunkSize)) {
			Log.e(TAG, "decrypt failed,not enought buffer to store result");
			return 0;
		}
		
		ByteBuffer chunk = ByteBufferPool.obtain(estimateEncryptLen(chunkSize, chunkSize));
		ByteBuffer decodedChunk = ByteBufferPool.obtain(len);
		
		int l = 0;//location of packs
		int tl = 0;
		boolean completed = true;
		while(l < len) {
			int d = 0;completed = true;
			chunk.clear();
			while(l < len) {
				d = (packs[offset+l]&0xFF);
				
				Log.d(TAG, "sChunkSize " + chunkSize + " l " + l + " b " + chunk.position() + " d " + d + " len " + len + " packs.len " + packs.length + " chunk.len " + chunk.capacity());
				
				if(++l+d > len) {
					completed = false;
					break;
				}
				
				if(d == 0) {
					break;
				}
				
				chunk.put(packs,l+offset,d);
				l += d;
				if(d < BLOCK_SIZE) {
					break;
				}
			}
			
			if(!completed) {
				Log.d(TAG, "chunk not completed.");
				break;
			}
			
			if(chunk.position() > 0) {
				decodedChunk.clear();
				boolean deProguardResult = deProguard(sPs, chunk.array(), 0, chunk.position(), decodedChunk);
				if(!deProguardResult) {
					Log.e(TAG, "decrypt>>> decode block failed: " + StringUtils.toRawString(chunk.array(),0,chunk.position()));
					break;
				}else {
					decodedChunk.flip();
					outBuffer.put(decodedChunk);
					tl = l;
					continue;
				}
			}
			
			break;
		}
		
		ByteBufferPool.recycle(chunk);
		ByteBufferPool.recycle(decodedChunk);
		
		if(!completed && tl <= 0) {
			return 0;
		}
		
		if(tl > 0) {
			return tl;
		}
		
		Log.e(TAG, "decrypt>>> decode packs failed. " + " processed " + l + "," + tl + " total len " + len);
		
		return 0;
	}
	
	private static int estimatePackLen(int len) {
		return (len/BLOCK_SIZE) + 1 + len;
	}
	
	private static boolean pack(byte[] origin,int offset,int len,ByteBuffer outBuffer) {
		if(origin != null && ArrayUtils.isValidateRange(origin.length, offset, len)) {
			int ll = estimatePackLen(len);
			if(outBuffer.remaining() < ll) {
				return false;
			}
			
			int l = len;
			int i = 0;
			while(l > 0) {
				int b = l>BLOCK_SIZE?BLOCK_SIZE:l;
				outBuffer.put((byte)(b&0xFF));
				outBuffer.put(origin, offset+len-l, b);
				l -= b;
				i += b + 1;
			}
			
			if(i<ll) {
				outBuffer.put((byte)0);
			}
			
			return true;
		}
		
		return false;
	}
	
	private static boolean deProguard(List<IProguarder> ps,byte[] origin,int offset,int len,ByteBuffer outBuffer) {
		if(ps == null || outBuffer == null) {
			return false;
		}
		
		boolean result = true;
		int l = len;
		ByteBuffer buffer = null;
		for(int i=ps.size()-1;i>=0;i--) {
			IProguarder p = ps.get(i);
			if(i == 0) {
				if(buffer == null) {
					result = result && p.decode(origin, offset, len, outBuffer);
				}else {
					result = result && p.decode(buffer.array(), 0, buffer.position(), outBuffer);
					ByteBufferPool.recycle(buffer);
				}
			}else {				
				ByteBuffer next = ByteBufferPool.obtain(p.estimateDecodeLen(l));
				if(buffer == null) {
					result = result && p.decode(origin, offset, len, next);
				}else {
					result = result && p.decode(buffer.array(), 0,buffer.position(),next);
					ByteBufferPool.recycle(buffer);
				}
				buffer = next;
			}
			
			if(!result) {
				break;
			}
		}
		
		if(!result) {
			Log.e(TAG, "deProguard failed.");
		}
		
		Log.i(TAG, "deProguard_origin: " + StringUtils.toRawString(origin,offset,len));
		Log.i(TAG, "deProguard: " + StringUtils.toRawString(outBuffer.array(),0,outBuffer.position()));
		
		return result;
	}
	
	private static boolean proguard(List<IProguarder> ps,byte[] origin,int offset,int len,ByteBuffer outBuffer) {
		if(ps == null || outBuffer == null) {
			return false;
		}
		
		boolean result = true;
		int l = len;
		ByteBuffer buffer = null;
		for(int i=0;i<ps.size();i++) {
			IProguarder p = ps.get(i);
			if(i == ps.size() -1) {
				if(buffer == null) {
					result = result && p.encode(origin, offset, len, outBuffer);
				}else {
					result = result && p.encode(buffer.array(), 0, buffer.position(), outBuffer);
					ByteBufferPool.recycle(buffer);
				}
			}else {				
				ByteBuffer next = ByteBufferPool.obtain(p.estimateEncodeLen(l));
				if(buffer == null) {
					result = result && p.encode(origin, offset, len, next);
				}else {
					result = result && p.encode(buffer.array(), 0,buffer.position(),next);
					ByteBufferPool.recycle(buffer);
				}
				buffer = next;
			}
			
			if(!result) {
				break;
			}
		}
		
		if(!result) {
			Log.e(TAG, "deProguard failed.");
		}
		
		return result;
	}
	
	private static int estimateProguardLen(List<IProguarder> ps,int len) {
		int rlt = len;
		if(ps != null) {
			for(IProguarder p:ps) {
				rlt = p.estimateEncodeLen(rlt);
			}
		}
		return rlt;
	}
	
	private static int estimateDeProguardLen(List<IProguarder> ps,int len) {
		int rlt = len;
		if(ps != null) {
			for(int i=ps.size()-1;i>=0;i--) {
				rlt = ps.get(i).estimateDecodeLen(rlt);
			}
		}
		return rlt;
	}
	
	public static int encrypt(byte[] origin,int chunkSize,ByteBuffer outBuffer) {
		return encrypt(origin,0,origin.length,chunkSize,outBuffer);
	}
	
	public static int decrypt(byte[] code,int chunkSize,ByteBuffer outBuffer) {
		return decrypt(code, 0, code.length,chunkSize,outBuffer);
	}
	
	public static int estimateEncryptLen(int len,int chunkSize) {
		int nc = len/chunkSize + 1;
		int nb = chunkSize/BLOCK_SIZE + 1;
		return len + (nc<<8) + nc + nb;
	}
	
	public static int estimateDecryptLen(int len,int chunkSize) {
		return len;
	}
	
	public static class DecryptResult{
		public byte[] origin;
		public int decryptLen;
	}
}
