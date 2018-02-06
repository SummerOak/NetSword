package com.chedifier.netsword;

import java.util.regex.Pattern;

import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.StringUtils;
import com.chedifier.netsword.socks5.SProxy;

public class Main {
	
	public static final String TAG = "NetSword";
	
	public static void main(String[] args){
		
		printArgs(args);
		
		boolean isServer = false;
		int debugLevel = 0;
		if(args != null && args.length >= 1) {
			for(int i=0;i<args.length;i++) {
				String[] kv = args[i].split(Pattern.quote("="));
				if(kv != null && kv.length == 2) {
					if(kv[0].equals("dl")) {
						debugLevel = StringUtils.parseInt(kv[1], debugLevel);
					}
				}else {
					if(args[i].equalsIgnoreCase("s")) {
						isServer = true;
					}
				}
			}
		}
		Log.setLogLevel(debugLevel);
		
		if(isServer) {
			new SProxy(8888,false).start();
		}else {
			new SProxy(8887,true).start();
		}
	}
	
	private static final void printArgs(String[] args) {
		if(args == null) {			
			Log.e(TAG, "args is null");
		}else {
			StringBuilder sb = new StringBuilder(128);
			for(int i=0;i<args.length;i++) {
				sb.append(args[i]).append(" ");
			}
			
			Log.d(TAG, "args: " + sb.toString());
		}
	}

}
