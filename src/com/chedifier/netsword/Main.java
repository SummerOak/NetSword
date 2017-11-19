package com.chedifier.netsword;

import com.chedifier.netsword.local.Local;

public class Main {
	
	public static final String TAG = "NetSword";
	
	public static void main(String[] args){
		Local local = new Local(8888);
		Result r = local.start();
		if(r == Result.SUCCESS) {
			local.sendRemote("hello vps!");
		}
		
		Log.i(TAG, "local " + r.getMessage());
	}

}
