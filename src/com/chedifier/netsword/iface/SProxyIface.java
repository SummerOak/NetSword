package com.chedifier.netsword.iface;

import com.chedifier.netsword.base.JobScheduler;
import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.StringUtils;
import com.chedifier.netsword.cipher.Cipher;
import com.chedifier.netsword.crash.CrashHandler;
import com.chedifier.netsword.socks5.Configuration;
import com.chedifier.netsword.socks5.SProxy;

public class SProxyIface {
	private static final String TAG = "SProxyIface";
	
	private SProxy mProxy; 

	public static SProxyIface start(String cfgFilePath,IProxyListener l,int forceServerOrLocal) {
		return new SProxyIface(cfgFilePath, l, forceServerOrLocal);
		
	}
	
	private SProxy startSProxy(boolean isServer,IProxyListener l) {
		if (isServer) {
			int port = Configuration.getConfigInt(Configuration.SERVER_PORT, 8668);
			mProxy = SProxy.createServer(port,l);
		} else {
			String server_host = Configuration.getConfig(Configuration.SERVER_ADDR, "");
			int server_port = Configuration.getConfigInt(Configuration.SERVER_PORT, 0);
			int port = Configuration.getConfigInt(Configuration.LOCAL_PORT, 8667);

			if (StringUtils.isEmpty(server_host) || server_port == 0) {
				Log.e(TAG, "server host or port is not configed correct,check settings.txt.");
				return null;
			}

			mProxy = SProxy.createLocal(port, server_host, server_port,l);
		}
		
		if(mProxy != null) {
			new Thread("SProxy") {
				@Override
				public void run() {
					mProxy.start();
				}
			}.start();
			
		}
		
		return mProxy;
	}
	
	public static void stop(SProxyIface proxy) {
		if(proxy != null && proxy.mProxy != null) {
			proxy.mProxy.stop();
		}
	}
	
	private SProxyIface(String cfgFilePath,IProxyListener l,int forceServerOrLocal) {
		CrashHandler.init();
		Configuration.init(cfgFilePath);
		
		Log.setLogLevel(Configuration.getConfigInt(Configuration.LOG_LEVL, 0));
		Log.setLogDir(Configuration.getConfig(Configuration.LOG_PATH, Configuration.DEFAULT_LOG_PATH));
		
		JobScheduler.init();
		Cipher.init();

		boolean isServer = false;
		if(forceServerOrLocal > 0) {
			isServer = true;
		}else if(forceServerOrLocal < 0) {
			isServer = false;
		}else {
			isServer = Configuration.getConfigInt(Configuration.IS_SERVER, 0) == 1;
		}
		
		startSProxy(isServer,l);

		Log.dumpLog2File();
	}
	
	public static final class STATE{
		public static final int INIT 		= 0;
		public static final int VERIFY 		= 1;
		public static final int CONN 		= 2;
		public static final int TRANS 		= 3;
		public static final int TERMINATE 	= 4;
	}
}
