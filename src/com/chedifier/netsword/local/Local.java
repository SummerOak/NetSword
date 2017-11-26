package com.chedifier.netsword.local;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import com.chedifier.netsword.ExceptionHandler;
import com.chedifier.netsword.IOUtils;
import com.chedifier.netsword.Log;
import com.chedifier.netsword.Result;

public class Local {
	
	private static final String TAG = "Local";
	
	private int mPort = 8888;
	private ServerSocket mSocket = null;
	
	private int mRemotePort = 8888;
	private String mRemoteHost = "47.90.206.185";
//	private String mRemoteHost = "localhost";
	
	public Local(int port) {
		mPort = 8888;
	}
	
	public Result start() {
		if(mSocket != null) {
			return Result.E_LOCAL_SOCKET_ALREADY_LISTENING;
		}
		
		try {
			mSocket = new ServerSocket(mPort);
			Log.i(TAG, "build socket success.");
			
			new Thread(new Runnable() {
				
				Socket conn = null;
				InputStream in = null;
				InputStreamReader ir = null;
				BufferedReader br = null;
			
				@Override
				public void run() {
					try {
						while(mSocket != null) {
							Log.i(TAG, "listening...");
							conn = mSocket.accept();
							Log.i(TAG, "receiving...");
							if(conn != null) {
								in = conn.getInputStream();
								ir = new InputStreamReader(in);
//								br = new BufferedReader(ir);
						
								
								int c = -1;
								while((c = ir.read()) != -1) {
									Log.i(TAG, "received: " + (char)(c));
								}
							}
						}
						
					}catch(Throwable t) {
						ExceptionHandler.handleException(t);
					}finally {
						IOUtils.safeClose(in);
						IOUtils.safeClose(ir);
						IOUtils.safeClose(conn);
						IOUtils.safeClose(mSocket);
					}
				}
			}).start();
		} catch (Throwable e) {
			ExceptionHandler.handleException(e);
			IOUtils.safeClose(mSocket);
			return Result.E_LOCAL_SOCKET_BUILD_FAILED;
		}
		
		return Result.SUCCESS;
	}
	
	public Result sendRemote(String message) {
		Log.i(TAG, "sendRemote " + message);
		try {
			
			Socket client = new Socket(mRemoteHost, mRemotePort);
			OutputStream os = client.getOutputStream();
			PrintWriter pw = new PrintWriter(os);
			pw.print(message);
			pw.flush();
			client.shutdownOutput();
			
		} catch (UnknownHostException e) {
			ExceptionHandler.handleException(e);
		} catch (IOException e) {
			ExceptionHandler.handleException(e);
		}
		
		return Result.SUCCESS;
	}


}
