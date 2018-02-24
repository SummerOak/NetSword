package com.chedifier.netsword.base;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.chedifier.netsword.base.JobScheduler.Job;
import com.chedifier.netsword.socks5.Configuration;

public class Log {
	
	private static int sLogLevel = 0;
	private static String sLogDir;
	private static final String DEF_DIR = Configuration.DEFAULT_LOG_PATH;
	
	private static final int MAX_SIZE = 100;
	private static ArrayList<String> sCache = new ArrayList<>(MAX_SIZE);
	private static long sLastDumpTime = 0L;
	private static SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss-SS");
	private static Date sDate = new Date();
	
	public static final void setLogDir(String dir) {
		sLogDir = dir;
	}
	
	public static final void setLogLevel(int level) {
		sLogLevel = level;
	}
	
	private static final String getTimeFormat() {
		return getTimeFormat(System.currentTimeMillis());
	}
	
	private static final String getTimeFormat(long time) {
		sDate.setTime(time);
		return sDateFormat.format(sDate);
	}

	public static final void i(String tag,String content) {
		if(sLogLevel > 2) {
			System.out.println(getTimeFormat() + " : " + "I> tid[" + Thread.currentThread().getId() + "] " + tag + " >> " + content);
		}
	}
	
	public static final void d(String tag,String content) {
		if(sLogLevel > 1) {
			System.out.println(getTimeFormat() + " : " + "D> tid[" + Thread.currentThread().getId() + "] " + tag + " >> " + content);
		}
	}
	
	public static final void e(String tag,String content) {
		String s = getTimeFormat() + " : " + "E> tid[" + Thread.currentThread().getId() + "] " + tag + " >> " + content;
		System.out.println(s);
		addLog(s);
	}
	
	public static final void r(String tag,String content) {
		if(sLogLevel >= 0) {
			String s = getTimeFormat() + " : " + "R> tid[" + Thread.currentThread().getId() + "] " + tag + " >> " + content;
			System.out.println(s);
			addLog(s);
		}
	}
	
	public static final void t(String tag,String content) {
		System.out.println(getTimeFormat() + " : " + "T> tid[" + Thread.currentThread().getId() + "] " + tag + " >> " + content);
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
		
		long now = System.currentTimeMillis();
		long time = sLastDumpTime;
		if((now-sLastDumpTime) > 3600) {
			time = now;
		}
		
		Date date = new Date(time);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-hh-mm");
		
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
					FileUtils.writeString2File(getLogFilePath(), sb.toString());
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
