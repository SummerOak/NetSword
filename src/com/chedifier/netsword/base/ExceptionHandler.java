package com.chedifier.netsword.base;

public class ExceptionHandler {
	private static final String TAG = "ExceptionHandler";
	
	public static final void handleException(Throwable t) {
		if(t != null) {
			Log.t(TAG, "" + t.toString());
//			t.printStackTrace();
		}
	}
	
	public static final void handleFatalException(Throwable t) {
		if(t != null) {
			Log.t(TAG, "" + t.toString());
//			t.printStackTrace();
		}
	}
}
