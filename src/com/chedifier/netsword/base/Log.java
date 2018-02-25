package com.chedifier.netsword.base;

import java.io.File;
import java.util.ArrayList;

import com.chedifier.netsword.base.JobScheduler.Job;
import com.chedifier.netsword.socks5.Configuration;
import com.chedifier.netsword.socks5.SProxy;

public class Log {
	
	private static int sLogLevel = 0;
	private static String sLogDir;
	private static final String DEF_DIR = Configuration.DEFAULT_LOG_PATH;
	
	private static final int MAX_SIZE = 100;
	private static final int LOG_TIME_ZONE = 3600*1000;//seperate logs by time,put logs have same time zone together.
	private static ArrayList<String> sCache = new ArrayList<>(MAX_SIZE);
	private static volatile long sLastDumpTime = 0L;
	
	public static final void setLogDir(String dir) {
		sLogDir = dir;
	}
	
	public static final void setLogLevel(int level) {
		sLogLevel = level;
	}

	public static final void i(String tag,String content) {
		if(sLogLevel > 2) {
			System.out.println(DateUtils.getCurrentDate() + " : " + "I> tid[" + Thread.currentThread().getId() + "] " + tag + " >> " + content);
		}
	}
	
	public static final void d(String tag,String content) {
		if(sLogLevel > 1) {
			System.out.println(DateUtils.getCurrentDate() + " : " + "D> tid[" + Thread.currentThread().getId() + "] " + tag + " >> " + content);
		}
	}
	
	public static final void e(String tag,String content) {
		String s = DateUtils.getCurrentDate() + " : " + "E> tid[" + Thread.currentThread().getId() + "] " + tag + " >> " + content;
		System.out.println(s);
		addLog(s);
	}
	
	public static final void r(String tag,String content) {
		if(sLogLevel >= 0) {
			String s = DateUtils.getCurrentDate() + " : " + "R> tid[" + Thread.currentThread().getId() + "] " + tag + " >> " + content;
			System.out.println(s);
			addLog(s);
		}
	}
	
	public static final void t(String tag,String content) {
		System.out.println(DateUtils.getCurrentDate() + " : " + "T> tid[" + Thread.currentThread().getId() + "] " + tag + " >> " + content);
	}
	
	private static final void addLog(String s) {
		synchronized (sCache) {
			sCache.add(s);
		}
		
		if(sCache.size() >= MAX_SIZE) {
			dumpLog2File();
		}
	}
	
	private static synchronized final String getLogFilePath() {
		
		long now = System.currentTimeMillis();
		if((now-sLastDumpTime) > LOG_TIME_ZONE) {
			sLastDumpTime = now;
		}
		long time = sLastDumpTime;
		
		Log.t("jjjdj", "time: " + time + " sLastDumpTime "+ sLastDumpTime);
		
		String dir = DEF_DIR;
		if(!StringUtils.isEmpty(sLogDir)) {
			dir = sLogDir;
		}
		
		if(!sLogDir.endsWith(File.separator)) {
			dir += File.separator + SProxy.getBirthDay() + File.separator;
		}
		
		if(!initLogDir(dir)) {
			Log.e("Log", "init log path failed.");
			return null;
		}
		
		return dir + DateUtils.getDate(time) + ".txt";
	}
	
	private static final boolean initLogDir(String sDir) {
		File dir = new File(sDir);
		if(!dir.exists()) {
			if(!dir.mkdirs()) {
				return false;
			}
		}else if(!dir.isDirectory()) {
			return false;
		}
		
		return true;
	}
	
	public static final void dumpLog2File() {
		dumpLog2File(null);
	}
	
	public static final void dumpLog2File(final ICallback cb) {
		
		JobScheduler.schedule(new Job("log-dumper") {
			
			@Override
			public void run() {
				StringBuilder sb = null;
				synchronized (sCache) {
					if(!sCache.isEmpty()) {
						sb = new StringBuilder(1024);
						for(String s:sCache) {
							sb.append(s).append("\n\r");
						}
						
						sCache.clear();
					}
				}
				
				if(sb != null && sb.length() > 0) {
					String path = getLogFilePath();
					Log.t("jjjdj", "path: " + path);
					FileUtils.writeString2File(path, sb.toString());
				}
				
				if(cb != null) {
					cb.onDumpFinish();
				}
			}
		});
		
	}
	
	public interface ICallback{
		void onDumpFinish();
	}
	
}
