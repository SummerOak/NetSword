/**
 * none thread safe
 */

package com.chedifier.netsword.socks5.version;

import java.nio.charset.StandardCharsets;

import com.chedifier.netsword.base.ArrayUtils;
import com.chedifier.netsword.base.ExceptionHandler;
import com.chedifier.netsword.base.Log;

public class Parcel {
	private static final String TAG = "Parcel";
	
	private byte[] buffer;
	private int position = 0;
	private int limit = 0;
	private static final int HEAD = 4;
	
	public static Parcel createEmptyParcel() {
		return new Parcel();
	}
	
	public static Parcel createParcelWithData(byte[] data,int offset,int len) {
		if(data == null || len <= HEAD || !ArrayUtils.isValidateRange(data.length, offset, len)) {
			return null;
		}
		
		int size = 0;
		try {
			size = readInt(data, offset);
		} catch (Exception e) {
			ExceptionHandler.handleException(e);
			return null;
		}
		
		if(size != len-4) {
			return null;
		}
		
		return new Parcel(data, offset, len);
	}
	
	private Parcel() {
		position = limit = HEAD;
	}
	
	private Parcel(byte[] data,int offset,int len) {
		if(data!=null && len > 0 && ArrayUtils.isValidateRange(data.length, offset, len)) {
			buffer = new byte[len];
			System.arraycopy(data, offset, buffer, 0, len);
			position = len;
			limit = len;
		}
	}
	
	public void flip() {
		position = HEAD;
	}
	
	public void writeInt(int data) {
		ensureCapacity(4);
		writeInt(buffer, position, data);
		position += 4;
	}
	
	public int readInt() throws Exception{
		if(position+4<=limit) {
			int value = (buffer[position+0]&0xFF)
					|((buffer[position+1]&0xFF)<<8)
					|((buffer[position+2]&0xFF)<<16)
					|((buffer[position+3]&0xFF)<<24);
			position += 4;
			return value;
		}else {
			throw new Exception("read int failed.");
		}
	}
	
	public int readInt(int defValue) {
		try {
			return readInt();
		}catch(Exception e) {
			ExceptionHandler.handleException(e);
		}
		
		return defValue;
	}
	
	public void writeString(String data) {
		Log.t(TAG, "writeString:" + data);
		byte[] raw = data==null?null:data.getBytes(StandardCharsets.UTF_8);
		int len = raw==null?0:raw.length;
		writeInt(len);
		if(len > 0) {
			writeBytes(raw, 0, len);
		}
	}
	
	public String readString(String defValue) {
		int len = -1;
		try {			
			len = readInt();
		}catch (Exception e) {
			ExceptionHandler.handleException(e);
			Log.t(TAG, "readString failed:" + e.getMessage());
			return defValue;
		}
		
		if(len > 0) {
			byte[] raw = readBytes(len);
			if(raw != null) {
				String value = new String(raw,StandardCharsets.UTF_8);
				Log.t(TAG, "readString success:" + value);
				return value;
			}else {
				position -= 4;
				return defValue;
			}
		}else if(len == 0){
			return "";
		}else {
			position -= 4;
			return defValue;
		}
	}
	
	public void writeBytes(byte[] data,int offset,int len) {
		if(data != null && len > 0 && ArrayUtils.isValidateRange(data.length, offset, len)) {
			ensureCapacity(len);
			System.arraycopy(data, offset, buffer, position, len);
			position += len;
		}
	}
	
	public byte[] readBytes(int len) {
		byte[] ret = null;
		if(position + len <= limit) {
			ret = new byte[len];
			System.arraycopy(buffer, position, ret, 0, len);
			position += len;
		}
		return ret;
	}
	
	public byte[] getBytes() {
		if(position > 0) {			
			writeInt(buffer,0,position-HEAD);
			byte[] r = new byte[position];
			System.arraycopy(buffer, 0, r, 0, position);
			return r;
		}
		return null;
	}
	
	private static boolean writeInt(byte[] arr,int offset,int data) {
		if(arr != null && ArrayUtils.isValidateRange(arr.length, offset, 4)) {
			arr[offset] = (byte)(data&0xFF);
			arr[offset+1] = (byte)((data>>8)&0xFF);
			arr[offset+2] = (byte)((data>>16)&0xFF);
			arr[offset+3] = (byte)((data>>24)&0xFF);
			return true;
		}
		
		return false;
	}
	
	private static int readInt(byte[] arr,int offset) throws Exception{
		if(arr != null && ArrayUtils.isValidateRange(arr.length, offset, 4)) {
			int value = arr[0]|(arr[1]<<8)|(arr[2]<<16)|(arr[3]<<24);
			return value;
		}
		
		throw new Exception("read integer from arr failed");
	}
	
	private void ensureCapacity(int len) {
		len += HEAD;
		if(len > 0) {
			if(buffer == null) {
				buffer = new byte[len];
				position = HEAD;limit = len;
			}
			
			if(buffer.length - position < len) {
				byte[] t = new byte[buffer.length + len];
				System.arraycopy(buffer, 0, t, 0, position);
				buffer = t;
				limit = buffer.length;
			}
		}
		
	}
	
	public interface Parcelable{
		Parcel parcel();
		Parcelable parcel(Parcel parcel);
	}
	
}
