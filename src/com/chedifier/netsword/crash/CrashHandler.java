package com.chedifier.netsword.crash;

import java.lang.Thread.UncaughtExceptionHandler;

import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.Log.ICallback;

public class CrashHandler {
	private static final String TAG = "CrashHandler";
	
	public static final void init() {
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				Log.e(TAG, "thread: " + t.getName() + "reason: " + e.getMessage() + " stack: " + Log.getStackTraceString(e));
				Log.dumpLog2File(new ICallback() {
					
					@Override
					public void onDumpFinish() {
						System.exit(1);
					}
				});
			}
		});
	}

}
