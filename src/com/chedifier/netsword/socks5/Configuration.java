package com.chedifier.netsword.socks5;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import com.chedifier.netsword.base.StringUtils;

public class Configuration {
	private static Properties sConfig;
	
	public static final String IS_SERVER 	= "is_server";
	public static final String SERVER_ADDR 	= "server_address";
	public static final String SERVER_PORT 	= "server_port";
	public static final String LOCAL_PORT 	= "local_port";
	
	public static final String CHUNKSIZE 	= "chunk_size";
	public static final String BUFFER_SIZE 	= "buffer_size";
	
	public static final String LOG_PATH 		= "log_directory";
	public static final String LOG_LEVL 		= "log_level";
	
	private static final String SETTING_PATH = "./Socks5/settings.txt";
	public static final int DEFAULT_CHUNKSIZE = 1<<20;
	public static final int DEFAULT_BUFFERSIZE = DEFAULT_CHUNKSIZE<<1;
	
	public static final String DEFAULT_LOG_PATH = "./Socks5/Log";
	
	public synchronized static void init(String cfgPath) {
		try {
			if (null == sConfig) {
				File configFile = new File(SETTING_PATH);
				if (configFile.exists() && configFile.isFile() && configFile.canRead()) {
					InputStream input = new FileInputStream(configFile);
					sConfig = new Properties();
					sConfig.load(input);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			sConfig = new Properties();
			sConfig.setProperty("is_server", "0");
			sConfig.setProperty("server_addr", "0");
			sConfig.setProperty("server_port", "0");
			sConfig.setProperty("local_port", "0");
			sConfig.setProperty("block_size", "0");
			sConfig.setProperty("log_path", "0");
		}
	}

	public static synchronized int getConfigInt(String key,int def) {
		if(sConfig == null) {
			return 0;
		}
		
		return StringUtils.parseInt(sConfig.getProperty(key), def);
	}
	
	public static synchronized String getConfig(String key,String def) {
		if(sConfig == null) {
			return null;
		}
		
		return sConfig.getProperty(key,def);
	}
}
