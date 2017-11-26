package com.chedifier.netsword;

import com.chedifier.netsword.ssl.client.Client;
import com.chedifier.netsword.ssl.server.Server;

public class Main {
	
	public static final String TAG = "NetSword";
	
	public static void main(String[] args){
		
		if(args != null && args.length >= 1) {
			if("s".equals(args[0])){
				Server server = new Server();
				server.service();
				return;
			}
		}
		
		Client client = new Client();
		client.start();
	}

}
