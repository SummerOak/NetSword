package com.chedifier.netsword.socks5;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import com.chedifier.netsword.base.Log;

public class SServerTest {
	
	public static void main(String[] args){
		
		SProxy l = new SProxy(8888,false);
		l.start();
		
//		Log.t("test", "open... ");
//		InetSocketAddress addr = new InetSocketAddress("btrace.video.qq.com", 80);
//		try {
//			Log.t("test", "open " + addr);
//			SocketChannel sc = SocketChannel.open(addr);
//			Log.t("test", "open returned " + sc);
//		} catch (IOException e) {
//			Log.t("test", "" + e.getMessage());
//			e.printStackTrace();
//		}
//		
//		Log.t("test", "open ... " + addr);
		
		
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
