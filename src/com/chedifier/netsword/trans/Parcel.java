package com.chedifier.netsword.trans;

public class Parcel extends BBuffer{
	
	private int mExpectedBytesLeft = 1;
	
	public int expectedBytesLeft() {
		return mExpectedBytesLeft;
	}
	
	public int append(byte[] data,int offset,int len) {
		int r = super.append(data,offset,len);
		
		return r;
	}
	
}
