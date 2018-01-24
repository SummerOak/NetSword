package com.chedifier.netsword;

import com.chedifier.netsword.local.SLocal;
import com.chedifier.netsword.server.SServer;

public class Main {
	
	public static final String TAG = "NetSword";
	
	public static void main(String[] args){
		
		printArgs(args);
		
		if(args != null && args.length >= 1) {
			if("s".equals(args[0])){
				SServer server = new SServer(8888);
				server.start();
				return;
			}
		}
		
		SLocal client = new SLocal(8888);
		client.start();
	}
	
	private static final void printArgs(String[] args) {
		if(args == null) {			
			Log.i(TAG, "args is null");
		}else {
			StringBuilder sb = new StringBuilder(128);
			for(int i=0;i<args.length;i++) {
				sb.append(args[i]).append(" ");
			}
			
			Log.i(TAG, "args: " + sb.toString());
		}
	}

}
