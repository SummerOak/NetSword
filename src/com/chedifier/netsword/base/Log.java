package com.chedifier.netsword.base;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.chedifier.netsword.base.JobScheduler.Job;

public class Log {
	
	private static int sLogLevel = 0;
	private static String sLogDir;
	private static final String DEF_DIR = "./Socks5/Log/";
	
	private static final int MAX_SIZE = 20;
	private static ArrayList<String> sCache = new ArrayList<>(MAX_SIZE);
	private static long sLastDumpTime = 0L;
	
	public static final void setLogDir(String dir) {
		sLogDir = dir;
	}
	
	public static final void setLogLevel(int level) {
		sLogLevel = 0;
	}
	
	private static final String getTimeFormat() {
		return getTimeFormat(System.currentTimeMillis());
	}
	
	private static final String getTimeFormat(long time) {
		long min = time/(1000*60);
		long sec = (time%(1000*60)) / 1000;
		long mils = (time%1000);
		return min + "." + sec + "." + mils;
	}

	public static final void i(String tag,String content) {
		if(sLogLevel > 2) {
			System.out.println("I> tid[" + Thread.currentThread().getId() + "] " + getTimeFormat() + " : " + tag + " >> " + content);
		}
	}
	
	public static final void d(String tag,String content) {
		if(sLogLevel > 1) {
			System.out.println("D> tid[" + Thread.currentThread().getId() + "] " + getTimeFormat() + " : " + tag + " >> " + content);
		}
	}
	
	public static final void e(String tag,String content) {
		String s = "E> tid[" + Thread.currentThread().getId() + "] " + getTimeFormat() + " : " + tag + " >> " + content;
		System.out.println(s);
		addLog(s);
	}
	
	public static final void r(String tag,String content) {
		if(sLogLevel >= 0) {
			String s = "R> tid[" + Thread.currentThread().getId() + "] " + getTimeFormat() + " : " + tag + " >> " + content;
			System.out.println(s);
			addLog(s);
		}
	}
	
	public static final void t(String tag,String content) {
		System.out.println("T> tid[" + Thread.currentThread().getId() + "] " + getTimeFormat() + " : " + tag + " >> " + content);
	}
	
	public static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }

        // This is to reduce the amount of log spew that apps do in the non-error
        // condition of the network being unavailable.
        Throwable t = tr;
        while (t != null) {
            t = t.getCause();
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, false);
        tr.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
	
	private static final void addLog(String s) {
		synchronized (sCache) {
			sCache.add(s);
		}
		
		if(sCache.size() >= MAX_SIZE) {
			dumpLog2File();
		}
	}
	
	private static final String getLogFilePath() {
		Log.r("333", "getLogFilePath1");
		long now = System.currentTimeMillis();
		long time = sLastDumpTime;
		if((now-sLastDumpTime) > 3600) {
			time = now;
		}
		
		Date date = new Date(time);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-mm-dd-hh-mm");
		
		Log.r("333", "getLogFilePath2");
		
		if(StringUtils.isEmpty(sLogDir)) {
			if(!initLogDir(DEF_DIR)) {
				Log.e("Log", "init log path failed.");
				return null;
			}
			sLogDir = DEF_DIR;
		}else if(!initLogDir(sLogDir)) {
			if(!initLogDir(DEF_DIR)) {
				Log.e("Log", "init log path failed.");
				return null;
			}
			
			sLogDir = DEF_DIR;
		}
		
		Log.r("333", "getLogFilePath3");
		
		if(!sLogDir.endsWith(File.separator)) {
			sLogDir += File.separator;
		}
		
		return sLogDir + df.format(date) + ".txt";
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
					FileUtils.writeString2File(getLogFilePath(), sb.toString());
				}
			}
		});
		
			
	}
	
}
