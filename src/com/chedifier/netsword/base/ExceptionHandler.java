package com.chedifier.netsword.base;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionHandler {
	private static final String TAG = "ExceptionHandler";
	
	public static final void handleException(Throwable t) {
		if(t != null) {
			Log.e(TAG, getStackTraceString(t));
		}
	}
	
	public static final void handleFatalException(Throwable t) {
		if(t != null) {
			Log.e(TAG, getStackTraceString(t));
		}
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
}
