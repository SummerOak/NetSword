package com.chedifier.netsword;

import java.net.InetAddress;

public class NetUtils {

	public static String getIPv4String(byte[] ip) {
		if(ip != null && ip.length >= 4) {
			return "" + ip[0] + "." + ip[1] + "." + ip[2] + "." + ip[3];
		}
		
		return "?";
	}
	
	public static InetAddress resolveAddrByDomain(String domain) {
		try {
			return InetAddress.getByName("www.example.com");
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
	
}
