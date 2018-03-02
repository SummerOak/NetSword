package com.chedifier.netsword.iface;

import com.chedifier.netsword.base.JobScheduler;
import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.StringUtils;
import com.chedifier.netsword.crash.CrashHandler;
import com.chedifier.netsword.external.ExternalCmdHandler;
import com.chedifier.netsword.socks5.Configuration;
import com.chedifier.netsword.socks5.SProxy;

public class SProxyIface {
	private static final String TAG = "SProxyIface";
	
	private static volatile boolean sInited = false;
	
	private static volatile SProxy sProxy = null;
	
	public static void init(String cfgFilePath) {
		if(!sInited) {
			CrashHandler.init();
			Configuration.init(cfgFilePath);
			
			sInited = true;
		}
	}

	public synchronized static void start(IProxyListener l,int forceServerOrLocal) {
		if(!sInited) {
			Log.e(TAG, "start SProxy failed,please invoke init first.");
			return;
		}
		
		if(sProxy != null) {
			Log.e(TAG, "SProxy already started,can not be start again!");
			return;
		}
		
		new SProxyIface(l, forceServerOrLocal);
	}
	
	public synchronized static void stop(String reason) {
		if(sProxy != null) {			
			sProxy.stop(reason);
		}
	}
	
	public static void sendExternCommand(boolean isLocal,int cmd,String params,String user,String password) {
		ExternalCmdHandler.sendCommand(isLocal, cmd, params, user, password);
	}
	
	private SProxy startSProxy(boolean isServer,IProxyListener l) {
		if(sProxy != null) {
			return sProxy;
		}
		
		if (isServer) {
			int port = Configuration.getConfigInt(Configuration.SERVER_PORT, 8668);
			sProxy = SProxy.createServer(port,l);
		} else {
			String server_host = Configuration.getConfig(Configuration.SERVER_ADDR, "");
			int server_port = Configuration.getConfigInt(Configuration.SERVER_PORT, 0);
			int port = Configuration.getConfigInt(Configuration.LOCAL_PORT, 8667);

			if (StringUtils.isEmpty(server_host) || server_port == 0) {
				Log.e(TAG, "server host or port is not configed correct,check settings.txt.");
				return null;
			}

			sProxy = SProxy.createLocal(port, server_host, server_port,l);
		}
		
		if(sProxy != null) {
			new Thread("SProxy") {
				@Override
				public void run() {
					sProxy.start();
				}
			}.start();
			
		}
		
		return sProxy;
	}
	
	private SProxyIface(IProxyListener l,int forceServerOrLocal) {
		Log.setLogLevel(Configuration.getConfigInt(Configuration.LOG_LEVL, 0));
		Log.setLogDir(Configuration.getConfig(Configuration.LOG_PATH, Configuration.DEFAULT_LOG_PATH));
		
		JobScheduler.init();
		
		ExternalCmdHandler.start();

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
