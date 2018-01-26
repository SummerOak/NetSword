package com.chedifier.netsword.local;

import com.chedifier.netsword.Log;
import com.chedifier.netsword.StringUtils;
import com.chedifier.netsword.trans.IProguarder;
import com.chedifier.netsword.trans.ShiftProguarder;

public class LocalTest {
	
	public static void main(String[] args){
		
		SLocal l = new SLocal(8887);
		l.start();
		
		
//		byte[] o = new byte[] {-127,0x01,0x00};
//		Log.e("test", "origin: " + StringUtils.toRawString(o, o.length));
//		IProguarder p = new ShiftProguarder();
//		byte[] e = p.encode(o);
//		Log.e("test", "encode: " + StringUtils.toRawString(e, e.length));
//		
//		byte[] d = p.decode(e);
//		Log.e("test", "decode: " + StringUtils.toRawString(d, d.length));
	}
}
