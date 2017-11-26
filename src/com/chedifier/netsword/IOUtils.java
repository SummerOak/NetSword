package com.chedifier.netsword;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

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
	
	public static int read(DataInputStream ins,byte[] data,int length) {
		int read = 0;
		int rt = 0;
		try {
			while ((rt = ins.read(data, rt, length - read)) > 0 && read < length) {
				read += rt;
			}
		} catch (IOException e) {
			ExceptionHandler.handleException(e);
		}
		return read;
	}
	
	public static final int write(OutputStream os,byte[] data,int length) {
		if(os != null) {
			try {
				os.write(data, 0, length);
				return length;
			} catch (IOException e) {
				ExceptionHandler.handleException(e);
			}
		}
		
		return 0;
	}
	
}
