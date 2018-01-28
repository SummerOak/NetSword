package com.chedifier.netsword.socks5;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import com.chedifier.netsword.Result;
import com.chedifier.netsword.base.ExceptionHandler;
import com.chedifier.netsword.base.IOUtils;
import com.chedifier.netsword.base.JobScheduler;
import com.chedifier.netsword.base.JobScheduler.Job;
import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.ObjectPool;
import com.chedifier.netsword.base.ObjectPool.IConstructor;
import com.chedifier.netsword.socks5.AbsS5Stage.ICallback;

public class SProxy {

	private final String TAG;

	private int mPort = 8887;
	private ServerSocket mSocket = null;
	private boolean mIsLocal;
	
//	private String mSServerHost = "47.90.206.185";
	private String mSServerHost = "127.0.0.1";
	private int mSServerPort = 8888;
	
	private ObjectPool<ConnHandler> mConnHandlerPool;

	public SProxy(int port,boolean isLocal) {
		mPort = port;
		mIsLocal = isLocal;
		TAG = "SProxy." + (isLocal?"local":"server");
		mConnHandlerPool = new ObjectPool<ConnHandler>(new IConstructor<ConnHandler>() {

			@Override
			public ConnHandler newInstance(Object... params) {
				return new ConnHandler((Socket)params[0]);
			}

			@Override
			public void initialize(ConnHandler e, Object... params) {
				e.mConnection = (Socket)params[0];
			}
		}, 20);
		
		JobScheduler.init();
	}

	public Result start() {
		if (mSocket != null) {
			Log.d(TAG, "already listening");
			return Result.E_LOCAL_SOCKET_ALREADY_LISTENING;
		}

		try {
			mSocket = new ServerSocket(mPort);
			Log.r(TAG, "create slocal success.");
			
			while (mSocket != null) {
				Log.d(TAG, "listening " + mPort + "...");
				Socket conn = null;
				try {
					conn = mSocket.accept();
					JobScheduler.schedule(mConnHandlerPool.obtain(conn));
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

	private class ConnHandler extends Job{
		
		Socket mConnection;
		
		private ConnHandler(Socket conn) {
			super("LOCAL_CONN_HADNDLER");
			mConnection = conn;
		}
		
		@Override
		public void run() {
			Log.r(TAG, "receive an conntion " + mConnection.getInetAddress().getHostAddress());
			Socket server = null;
			if(mIsLocal) {
				long connServerCost = System.currentTimeMillis();
				Log.d(TAG, "connecting to proxy " + mSServerHost + " " + mSServerPort);
				server = bindSServer();
				Log.i(TAG, "conn server " + (server==null?"failed":"succ") + " cost: " + (System.currentTimeMillis() - connServerCost));
			}
			
			SocketContext context = new SocketContext(mConnection,server);
			Result handleResult = new S5VerifyStage(context,mIsLocal,new ICallback() {
				
				@Override
				public void onResult(Result result) {
					if(result == null) {
						return;
					}
					
					switch(result) {
						case E_S5_TRANS_END:{
							Log.t(TAG, "trans has end, destroy context.");
							context.destroy();
							break;
						}
					}
				}
			}).handle();
			
			if(handleResult != Result.SUCCESS) {
				Log.t(TAG, "conn handle failed, destroy context." + handleResult);
				context.destroy();
			}
			
			mConnHandlerPool.release(this);
		}
	}

}
