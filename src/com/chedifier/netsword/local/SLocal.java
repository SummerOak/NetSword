package com.chedifier.netsword.local;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import com.chedifier.netsword.ExceptionHandler;
import com.chedifier.netsword.IOUtils;
import com.chedifier.netsword.Log;
import com.chedifier.netsword.Result;
import com.chedifier.netsword.socks5.S5VerifyStage;
import com.chedifier.netsword.socks5.SocketContext;

public class SLocal {

	private static final String TAG = "NetSword.slocal";

	private int mPort = 8887;
	private ServerSocket mSocket = null;
	
	private String mSServerHost = "47.90.206.185";
//	private String mSServerHost = "127.0.0.1";
	private int mSServerPort = 8888;

	public SLocal(int port) {
		mPort = port;
	}

	public Result start() {
		if (mSocket != null) {
			return Result.E_LOCAL_SOCKET_ALREADY_LISTENING;
		}

		try {
			mSocket = new ServerSocket(mPort);
			Log.i(TAG, "build socket success.");
			
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

	private Socket bindSServer() {
		Socket server = new Socket();
		SocketAddress address = new InetSocketAddress(mSServerHost, mSServerPort);
		try {
			server.connect(address, 10);
		} catch (IOException e) {
			ExceptionHandler.handleException(e);
			return null;
		}
		
		return server;
	}

	private class ConnHandler extends Thread{
		
		Socket mConnection;
		
		private ConnHandler(Socket conn) {
			mConnection = conn;
		}
		
		@Override
		public void run() {
			Socket server = bindSServer();
			if(server != null) {
				new S5VerifyStage(new SocketContext(mConnection,server),true).handle();
			}else {
				Log.i(TAG, "bind server failed.");
			}
			
		}
	}

}
