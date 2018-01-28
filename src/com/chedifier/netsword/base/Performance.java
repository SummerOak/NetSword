package com.chedifier.netsword.base;

import java.util.concurrent.ConcurrentHashMap;

public class Performance {
	private static final String TAG = "Performance";
	
	private static ConcurrentHashMap<String, Long> sTags = new ConcurrentHashMap<>();
	
	public static void start(String tag) {
		if(StringUtils.isEmpty(tag)) {
			return;
		}
		
		sTags.put(tag, System.currentTimeMillis());
	}
	
	public static void end(String tag) {
		if(StringUtils.isEmpty(tag)) {
			return;
		}
		
		Long T = sTags.remove(tag);
		if(T != null) {
			long now = System.currentTimeMillis();
			Log.i(TAG,tag + " cost: " + (now - T) + "ms");
		}
	
	}
	
}
