package com.chedifier.netsword;

import com.chedifier.netsword.base.JobScheduler;
import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.StringUtils;
import com.chedifier.netsword.crash.CrashHandler;
import com.chedifier.netsword.socks5.Configuration;
import com.chedifier.netsword.socks5.SProxy;

public class Socks5 {
	
	private static final String TAG = "SOCKS5";
	
	public static void main(String[] args){
		
		CrashHandler.init();
		JobScheduler.init();
		
		Log.setLogLevel(Configuration.getConfigInt(Configuration.LOG_LEVL, 0));
		Log.setLogDir(Configuration.getConfig(Configuration.LOG_PATH, "./socks5/log"));
		
		Log.d("", ((String)null).concat(""));
	
		startSProxy();
		
		Log.dumpLog2File();
	}
	
	private static void startSProxy() {
		boolean isServer = Configuration.getConfigInt(Configuration.IS_SERVER, 1) == 1;
		if(isServer) {
			int port = Configuration.getConfigInt(Configuration.SERVER_PORT, 8668);
			SProxy.createServer(port).start();
		}else {
			String server_host = Configuration.getConfig(Configuration.SERVER_ADDR,"");
			int server_port = Configuration.getConfigInt(Configuration.SERVER_PORT, 0);
			int port = Configuration.getConfigInt(Configuration.LOCAL_PORT, 8667);
			
			if(StringUtils.isEmpty(server_host) || server_port == 0) {
				Log.e(TAG, "server host or port is not configed correct,check settings.txt.");
				return;
			}
			
			SProxy.createLocal(port, server_host, server_port).start();
		}
	}
}
