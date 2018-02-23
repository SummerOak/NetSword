package com.chedifier.netsword.base;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class NetUtils {

	public static String getIPv4String(byte[] ip) {
		if(ip != null && ip.length >= 4) {
			return "" + ip[0] + "." + ip[1] + "." + ip[2] + "." + ip[3];
		}
		
		return "?";
	}
	
	public static SocketChannel bindSServer(InetSocketAddress netAddr) {
		SocketChannel server = null;
		try {
			server = SocketChannel.open();
			server.configureBlocking(false);
			server.connect(netAddr);
		} catch (Throwable e) {
			ExceptionHandler.handleException(e);
		}

		return server;
	}
	
	public static InetAddress resolveAddrByDomain(String domain) {
		try {
			return InetAddress.getByName(domain);
		} catch (Throwable e) {
			ExceptionHandler.handleException(e);
		}
		
		return null;
	}
	
	public static InetAddress resolveAddrByIP(byte[] ip) {
		try {
			return InetAddress.getByAddress(ip);
		} catch (Throwable e) {
			ExceptionHandler.handleException(e);
		}
		
		return null;
	}
	
	public static final String getOpsDest(int ops) {
		String dest = "";
		if((ops&SelectionKey.OP_ACCEPT) > 0) {
			dest += "accept&";
		}
		if((ops&SelectionKey.OP_CONNECT) > 0) {
			dest += "connect&";
		}
		
		if((ops&SelectionKey.OP_READ) > 0) {
			dest += "read&";
		}
		if((ops&SelectionKey.OP_WRITE) > 0) {
			dest += "write&";
		}
		
		if(StringUtils.isEmpty(dest)) {
			dest = "null";
		}
		
		return dest;
	}
	
}
