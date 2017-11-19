package com.chedifier.netsword;

import java.io.Closeable;
import java.io.IOException;

public class IOUtils {

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
