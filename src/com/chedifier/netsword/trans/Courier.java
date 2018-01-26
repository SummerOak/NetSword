package com.chedifier.netsword.trans;

import java.io.DataInputStream;
import java.io.OutputStream;

import com.chedifier.netsword.IOUtils;
import com.chedifier.netsword.Log;
import com.chedifier.netsword.StringUtils;

public class Courier {
	
	private static final String TAG = "Courier";
	
	private final int B = 0xFF;
	
	private IProguarder mProguarder = new ShiftProguarder();
	
	public boolean writeParcel(Parcel parcel,OutputStream os) {
		Log.i(TAG, "write parcel origin: size = " + parcel.size() + " content: " + StringUtils.toRawString(parcel.getData(), parcel.size()));
		byte[] data = mProguarder.encode(parcel.mData,0,parcel.mSize);
		Log.i(TAG, "write parcel encoded: " + StringUtils.toRawString(data,data.length));
		int r = data.length;
		int s = 0,t = 0;
		while(r > 0) {
			t = r>B?B:r;
			if(IOUtils.write(os, new byte[]{(byte)(t&0xFF)}, 1) == 1 && IOUtils.write(os,data,s,t) == t) {
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
			if(IOUtils.write(os, new byte[] {0}, 1) == 1) {
				Log.i(TAG, "write parcel end tail succ.");
				return true;
			}
			
			Log.i(TAG, "write parcel end tail failed.");
			return false;
		}
		
		Log.i(TAG, "write parcel succ.");
		return true;
	}
	
	public Parcel readParcel(DataInputStream is) {
		Parcel p = new Parcel();
		byte[] buffer = new byte[B];
		do {
			int l;
			if(IOUtils.read(is, buffer, 1) == 1) {
				l = buffer[0];
				Log.i(TAG, "read parcel l = " + l);
				if(l == 0) {
					return decode(p);
				}
				
				if(l > 0 && IOUtils.read(is, buffer, l) == l) {
					Log.i(TAG, "read parcel block succ l="+l + " block: " +StringUtils.toRawString(buffer, l));
					p.append(buffer,0,l);
					
					if(l < B) {
						return decode(p);
					}
				}else {
					Log.i(TAG, "read parcel block failed");
					break;
				}
			}else {
				Log.i(TAG, "read parcel len failed");
				break;
			}
			
		}while(true);
		
		return null;
	}
	
	private Parcel decode(Parcel parcel) {
		Parcel decode = new Parcel();
		Log.i(TAG, "read parcel encode: " + StringUtils.toRawString(parcel.getData(), parcel.size()));
		decode.append(mProguarder.decode(parcel.mData, 0, parcel.mSize));
		Log.i(TAG, "read parcel decode: size = " + decode.size() + " content: " + StringUtils.toRawString(decode.getData(), decode.size()));
		return decode;
	}
	
}
