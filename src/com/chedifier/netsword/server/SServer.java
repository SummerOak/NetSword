package com.chedifier.netsword.server;

import java.net.ServerSocket;
import java.net.Socket;

import com.chedifier.netsword.ExceptionHandler;
import com.chedifier.netsword.IOUtils;
import com.chedifier.netsword.Log;
import com.chedifier.netsword.Result;
import com.chedifier.netsword.socks5.S5VerifyStage;
import com.chedifier.netsword.socks5.SocketContext;

public class SServer {
	private static final String TAG = "NetSword.sserver";

	private ServerSocket mSocket;
	private int mPort = 8888;
	
	public SServer(int port) {
		mPort = port;
	}
	
	public Result start() {
		if (mSocket != null) {
			return Result.E_LOCAL_SOCKET_ALREADY_LISTENING;
		}

		try {
			mSocket = new ServerSocket(mPort);
			Log.i(TAG, "build server success.");

			new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						while (mSocket != null) {
							Log.i(TAG, "listening...");
							Socket conn = mSocket.accept();
							new ConnHandler(conn).start();
						}
						
					} catch (Throwable t) {
						ExceptionHandler.handleException(t);
					} finally {
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
	
	private class ConnHandler extends Thread{
		
		Socket mConnection;
		
		private ConnHandler(Socket conn) {
			mConnection = conn;
		}
		
		@Override
		public void run() {
			new S5VerifyStage(new SocketContext(mConnection,null),false).handle();
		}
	}
	
}
