package com.chedifier.netsword.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
	
	private Executor mExecutor = null;
	
	public SServer(int port) {
		mPort = port;
	}
	
	public Result start() {
		if (mSocket != null) {
			Log.d(TAG, "already listening");
			return Result.E_LOCAL_SOCKET_ALREADY_LISTENING;
		}

		try {
			mSocket = new ServerSocket(mPort);
			Log.r(TAG, "create sserver success.");
			
			mExecutor = Executors.newFixedThreadPool(20);
			
			while (mSocket != null) {
				Log.d(TAG, "listening on " + mPort + "...");
				Socket conn = null;
				try {
					conn = mSocket.accept();
					mExecutor.execute(new ConnHandler(conn));
				} catch (Throwable t) {
					ExceptionHandler.handleException(t);
				} finally {
					IOUtils.safeClose(conn);
				}
			}
		} catch (Throwable e) {
			ExceptionHandler.handleException(e);
			return Result.E_LOCAL_SOCKET_BUILD_FAILED;
		} finally {
			IOUtils.safeClose(mSocket);
		}

		return Result.SUCCESS;
		
	}
	
	private class ConnHandler implements Runnable{
		
		Socket mConnection;
		
		private ConnHandler(Socket conn) {
			mConnection = conn;
		}
		
		@Override
		public void run() {
			Log.r(TAG, "receive a conn.");
			new S5VerifyStage(new SocketContext(mConnection,null),false).handle();
		}
	}
	
}
