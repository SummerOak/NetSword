package com.chedifier.netsword.socks5;

import java.util.HashMap;
import java.util.Map;

public class Configuration {
	
	
	private static Map<String,Object> sConfigurations = new HashMap<>();
	
	private static boolean sInited = false;
	public static final String KEY_BLOCK_SIZE = "KEY_BLOCK_SIZE";
	public static final String KEY_CHUNK_SIZE = "KEY_CHUNK_SIZE";
	public static final String KEY_BUFFER_SIZE = "KEY_BUFFER_SIZE";
	
	
	public synchronized static void init() {
		if(!sInited) {
			sConfigurations.put(KEY_BLOCK_SIZE, 255);
			sConfigurations.put(KEY_CHUNK_SIZE, 255<<3);
			sConfigurations.put(KEY_BUFFER_SIZE, 255<<6);
			sInited = true;
		}
	}
	
	public static int getConfigurationInt(String key,int def) {
		Object o = sConfigurations.get(key);
		if(o instanceof Integer) {
			return (int)o;
		}
		
		return def;
	}
}
