package com.chedifier.netsword.local;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
	
//	private String mSServerHost = "47.90.206.185";
	private String mSServerHost = "127.0.0.1";
	private int mSServerPort = 8888;
	
	private Executor mExecutor = null;

	public SLocal(int port) {
		mPort = port;
	}

	public Result start() {
		if (mSocket != null) {
			return Result.E_LOCAL_SOCKET_ALREADY_LISTENING;
		}

		try {
			mSocket = new ServerSocket(mPort);
			Log.r(TAG, "create slocal success.");
			
			mExecutor = Executors.newFixedThreadPool(20);
			
			while (mSocket != null) {
				Log.d(TAG, "listening " + mPort + "...");
				Socket conn = null;
				try {
					conn = mSocket.accept();
					mExecutor.execute(new ConnHandler(conn));
				}catch (Throwable t) {
					ExceptionHandler.handleException(t);
					IOUtils.safeClose(conn);
				}
			}
		} catch (Throwable e) {
			ExceptionHandler.handleException(e);
			return Result.E_LOCAL_SOCKET_BUILD_FAILED;
		}finally {
			IOUtils.safeClose(mSocket);
		}

		return Result.SUCCESS;
	}

	private Socket bindSServer() {
		Socket server = new Socket(Proxy.NO_PROXY);
		SocketAddress address = new InetSocketAddress(mSServerHost, mSServerPort);
		try {
			
			server.connect(address, 10000);
		} catch (IOException e) {
			ExceptionHandler.handleException(e);
			return null;
		}
		
		return server;
	}

	private class ConnHandler implements Runnable{
		
		Socket mConnection;
		
		private ConnHandler(Socket conn) {
			mConnection = conn;
		}
		
		@Override
		public void run() {
			Log.r(TAG, "receive an conntion " + mConnection.getInetAddress().getHostAddress());
			long connServerCost = System.currentTimeMillis();
			Log.d(TAG, "connecting to proxy " + mSServerHost + " " + mSServerPort);
			Socket server = bindSServer();
			Log.i(TAG, "conn server cost: " + (System.currentTimeMillis() - connServerCost));
			if(server != null) {
				Log.r(TAG, "conn proxy server succ.");
				new S5VerifyStage(new SocketContext(mConnection,server),true).handle();
			}else {
				Log.e(TAG, "conn proxy server failed. ");
			}
			
		}
	}

}
