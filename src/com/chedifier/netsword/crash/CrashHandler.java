package com.chedifier.netsword.crash;

import java.lang.Thread.UncaughtExceptionHandler;

import com.chedifier.netsword.base.ExceptionHandler;
import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.Log.ICallback;
import com.chedifier.netsword.socks5.SProxy;

public class CrashHandler {
	private static final String TAG = "CrashHandler";
	
	public static final void init() {
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				Log.e(TAG, "FATAL died, thread: " + (t==null?"":t.getName()) 
						+ " reason: " + (e==null?"":e.getMessage()) 
						+ " stack: " + ExceptionHandler.getStackTraceString(e));
				
				Log.dumpBeforeExit(new ICallback() {
					
					@Override
					public void onDumpFinish() {
						System.exit(1);
					}
				});
			}
		});
	}

}
