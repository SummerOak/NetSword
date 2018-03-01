package com.chedifier.netsword.base;

import java.io.Closeable;
import java.io.IOException;

public class IOUtils {
	private static final String TAG = "NetSword.io";

	public static final void safeClose(Closeable i){
		if(i != null) {
			try {
				i.close();
			} catch (IOException e) {
				ExceptionHandler.handleException(e);
			}
		}
	}
}
