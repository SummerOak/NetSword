package com.chedifier.netsword.memory;

public class ByteArray {
	public static final int MAX_SIZE = 1<<23;//8M
	
	private final int mSize;
	private final byte[] mData;
	private int mLen;
	
	private ByteArray(int size) {
		if(size > 0 && size <= MAX_SIZE) {
			this.mSize = size;
			this.mData = new byte[mSize];
		}else {
			mSize = 0;
			mData = null;
		}
	}
	
	public void set(int index,byte value) {
		if(0<=index && index<mSize) {
			mData[index] = value;
		}
	}
	
	/**
	 * get the data at index
	 * @param index
	 * @return the value at index, maybe 0 if the index given is not in range.
	 */
	public byte get(int index) {
		if(0<=index&&index<mSize) {
			return mData[index];
		}
		
		return 0;
	}
	
	/**
	 * set length of available data
	 * @param len
	 */
	public void setLen(int len) {
		this.mLen = len;
	}
	
	public int getLen() {
		return mLen;
	}

}
