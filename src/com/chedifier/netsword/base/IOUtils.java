package com.chedifier.netsword.base;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

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
	
	public static int readSocketChannel(SocketChannel socketChannel,ByteBuffer buffer) {
		try {
			int read = socketChannel.read(buffer);
			return read;
		} catch (Throwable e) {
			ExceptionHandler.handleException(e);
		}
		return -1;
	}
	
	public static int writeSocketChannel(SocketChannel socketChannel,ByteBuffer buffer) {
		try {
			int w = socketChannel.write(buffer);
			return w;
		} catch (Throwable e) {
			ExceptionHandler.handleException(e);
		}
		return -1;
	}
	
	public static int read(DataInputStream ins,byte[] data,int offset,int length) {
		int read = 0;
		int rt = 0;
		try {
			while ((rt = ins.read(data, offset, length - read)) > 0 && read < length) {
				read += rt;
				offset += rt;
			}
		} catch (IOException e) {
			ExceptionHandler.handleException(e);
		}
		return read;
	}
	
	public static int read(DataInputStream ins,byte[] data,int length) {
		return read(ins,data,0,length);
	}
	
	public static final int write(OutputStream os,byte[] data,int length) {
		return write(os,data,0,length);
	}
	
	public static final int write(OutputStream os,byte[] data,int offset,int length) {
		if(os != null) {
			try {
				os.write(data, offset, length);
				return length;
			} catch (IOException e) {
				ExceptionHandler.handleException(e);
			}
		}
		
		return 0;
	}
	
	public static final int relay(Socket src,Socket dst) {
		Log.i(TAG, "relay...");
		int size = 0;
		try {
			DataInputStream is = new DataInputStream(src.getInputStream());
			Log.i(TAG, "relay 1");
			OutputStream os = dst.getOutputStream();
			Log.i(TAG, "relay 2");
			final int L = 1024;
			byte[] buffer = new byte[L];
			int r = 0,w = 0;
			while((r = read(is,buffer,L)) > 0) {
				Log.i(TAG, "relay<<<" + StringUtils.toRawString(buffer, r));
				size += (w = write(os, buffer, r));
				Log.i(TAG, "relay>>>" + StringUtils.toRawString(buffer, w));
				if(w != r) {
					break;
				}
			}
			
			
		} catch (IOException e) {
			ExceptionHandler.handleException(e);
		}
		
		return size;
	}
	
	
	
}
