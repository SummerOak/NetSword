package com.chedifier.netsword.trans;

import com.chedifier.netsword.ArrayUtils;
import com.chedifier.netsword.Log;

public class Parcel {

	protected int mSize = 0;
	protected byte[] mData = null;
	
	public Parcel() {
		this(8);
	}
	
	public Parcel(int capacity) {
		if(capacity > 0) {
			extendIfNeeded(capacity);
		}
	}
	
	public int append(byte[] data,int offset,int len) {
		if(data == null || !ArrayUtils.isValidateRange(data.length, offset, len)) {
			return 0;
		}
		
		extendIfNeeded(len);
		
		Log.i("test", "ttl " + mData.length + " dataLen " + data.length + " size " + mSize +  " offset " + offset + " len " + len);
		System.arraycopy(data, offset, mData, mSize, len);
		mSize += len;
		return len;
	}
	
	public int append(byte[] data) {
		if(data == null) {
			return 0;
		}
		
		return append(data,0,data.length);
	}
	
	public byte[] getData() {
		return mData;
	}
	
	public int size() {
		return mSize;
	}
	
	private void extendIfNeeded(int len) {
		if(mData == null) {
			mData = new byte[len<<1];
		}else if((mData.length - mSize) < len) {
			byte[] t = mData;
			mData = new byte[(mSize + len)<<1];
			System.arraycopy(t, 0, mData, 0, t.length);
		}
	}
	
}
