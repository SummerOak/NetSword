package com.chedifier.netsword.socks5;

public class SSLocalTest {
	
	public static void main(String[] args){
		
		SProxy l = new SProxy(8887,true);
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
