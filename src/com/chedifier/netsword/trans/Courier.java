package com.chedifier.netsword.trans;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.chedifier.netsword.base.IOUtils;
import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.StringUtils;

public class Courier {
	
	private static final String TAG = "Courier";
	
	private final int B = 0xFF;
	
	private IProguarder mProguarder = new ShiftProguarder();
	
	public boolean writeParcel(BBuffer parcel,SocketChannel socketChannel) {
		Log.i(TAG, "write parcel origin: size = " + parcel.size() + " content: " + StringUtils.toRawString(parcel.getData(), parcel.size()));
		byte[] data = mProguarder.encode(parcel.mData,0,parcel.mSize);
		Log.i(TAG, "write parcel encoded: " + StringUtils.toRawString(data,data.length));
		int r = data.length;
		int s = 0,t = 0;
		while(r > 0) {
			t = r>B?B:r;
			if(IOUtils.writeSocketChannel(socketChannel, ByteBuffer.wrap(new byte[]{(byte)(t&0xFF)})) == 1 
					&& IOUtils.writeSocketChannel(socketChannel, ByteBuffer.wrap(data,s,t)) == t) {
				Log.i(TAG, "write parcel block succ. l = " + t + " block: " + StringUtils.toRawString(data, s,t));
				s += t;
			}else {
				Log.i(TAG, "write parcel block failed. l = " + t + " block: " + StringUtils.toRawString(data, s,t));
				break;
			}
			
			r -= t;
		}
		
		if(r != 0) {
			Log.e(TAG,"send parcel failed.");
			return false;
		}
		
		if(t == B) {
			if(IOUtils.writeSocketChannel(socketChannel, ByteBuffer.wrap(new byte[] {0})) == 1) {
				Log.i(TAG, "write parcel end tail succ.");
				return true;
			}
			
			Log.i(TAG, "write parcel end tail failed.");
			return false;
		}
		
		Log.i(TAG, "write parcel succ.");
		return true;
	}
	
	public boolean readParcel(SocketChannel socketChannel,Parcel parcel) {
		if(parcel == null) {
			return false;
		}
		
		if(parcel.expectedBytesLeft() == 0) {
			return true;
		}
		
		byte[] data = new byte[B];
		ByteBuffer buffer = ByteBuffer.wrap(data);
		int r = 0;
		if((r=IOUtils.readSocketChannel(socketChannel, buffer)) > 0) {
			parcel.append(data,0,r);
		}
		
		return parcel.expectedBytesLeft() == 0;
	}
	
	private BBuffer decode(BBuffer parcel) {
		BBuffer decode = new BBuffer();
		Log.i(TAG, "read parcel encode: " + StringUtils.toRawString(parcel.getData(), parcel.size()));
		decode.append(mProguarder.decode(parcel.mData, 0, parcel.mSize));
		Log.i(TAG, "read parcel decode: size = " + decode.size() + " content: " + StringUtils.toRawString(decode.getData(), decode.size()));
		return decode;
	}
	
}
