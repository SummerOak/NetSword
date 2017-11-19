package com.chedifier.netsword;

import com.chedifier.netsword.local.Local;

public class Main {
	
	public static final String TAG = "NetSword";
	
	public static void main(String[] args){
		Result r = new Local(8888).start();
		
		Log.i(TAG, "local " + r.getMessage());
	}

}
