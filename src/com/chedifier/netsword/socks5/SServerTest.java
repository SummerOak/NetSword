package com.chedifier.netsword.socks5;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import com.chedifier.netsword.base.Log;

public class SServerTest {
	
	public static void main(String[] args){
		
		SProxy l = new SProxy(8888,false);
		l.start();
		
//		InetSocketAddress addr = new InetSocketAddress("www.baidu.com", 443);
//		try {
//			Log.d("test", "open " + addr);
//			SocketChannel sc = SocketChannel.open(addr);
//			Log.d("test", "open returned " + sc);
//		} catch (IOException e) {
//			Log.d("test", "" + e.getMessage());
//			e.printStackTrace();
//		}
//		
//		Log.d("test", "open ... " + addr);
		
		
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
