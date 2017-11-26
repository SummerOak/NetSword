package com.chedifier.netsword.ssl.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.chedifier.netsword.ExceptionHandler;
import com.chedifier.netsword.IOUtils;
import com.chedifier.netsword.Log;
import com.chedifier.netsword.ssl.Configuration;
import com.chedifier.netsword.ssl.SocketIO;

public class Client implements HandshakeCompletedListener{
	private static final String TAG = "Client";

	private SSLContext sslContext;
	private int mServerPort = 0;
	private String mServerHost = null;
	private SSLSocket mConnection;
	
	private Properties mConfiguration;
	
	private boolean mInitSuccess = false;
	private boolean mWorking = false;
	private ServerSocket mLocalSocket;
	
	private Executor mExecutor = null;

	public Client() {
		try {
			mConfiguration = Configuration.getConfig();

			sslContext = Auth.getSSLContext();
			SSLSocketFactory factory = (SSLSocketFactory) sslContext.getSocketFactory();
			mConnection = (SSLSocket) factory.createSocket();
			String[] pwdsuits = mConnection.getSupportedCipherSuites();
			mConnection.setEnabledCipherSuites(pwdsuits);
			mConnection.setUseClientMode(true);

			mServerHost = mConfiguration.getProperty("server_host");
			mServerPort = Integer.valueOf(mConfiguration.getProperty("server_port", "0"));
			Log.i(TAG, "connecting to " + mServerHost + ":" + mServerPort);
			SocketAddress address = new InetSocketAddress(mServerHost, mServerPort);
			mConnection.connect(address, 0);
			mConnection.addHandshakeCompletedListener(this);
			
			mExecutor = Executors.newFixedThreadPool(Integer.valueOf(mConfiguration.getProperty("local_executor_pool_size","10")));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public boolean start() {
		if(mWorking) {
			Log.i(TAG, "client is already listening");
			return true;
		}
		
		if(!mInitSuccess) {
			Log.i(TAG, "failed to connect to server.");
			return false;
		}
		
		if(!buildLocalListener()) {
			Log.i(TAG, "build local listener failed");
			return false;
		}
		
		mWorking = true;
		int failedTime = 0;
		while(mWorking) {
			try {
				Socket conn = mLocalSocket.accept();
				failedTime = 0;
				mExecutor.execute(new Worker(conn, mConnection));
			} catch (IOException e) {
				ExceptionHandler.handleException(e);
				
				
				Log.i(TAG, "accept local conn failed,wait ");
				waitSecs(2);
				failedTime++;
			}
		}
		
		return false;
	}
	
	private void waitSecs(int sec) {
		try {
			Thread.sleep(sec*1000L);
		} catch (InterruptedException e1) {
			ExceptionHandler.handleException(e1);
		}
	}
	
	private boolean buildLocalListener() {
		if(mLocalSocket != null) {
			
		}
		try {
			Properties p = Configuration.getConfig();
			int port = Integer.valueOf(p.getProperty("localPort", "8001"));
			Log.i(TAG, "connecting to " + mServerHost + ":" + port);
			mLocalSocket = new ServerSocket(port);
		} catch (Exception e) {
			ExceptionHandler.handleException(e);
			return false;
		}
		return true;
	}
	
	private static class Worker implements Runnable{
		
		private Socket localRequest;
		private SSLSocket serverConnction;
		private final int BUFFER_SIZE = 1024;
		
		public Worker(Socket req,SSLSocket server) {
			this.localRequest = req;
			this.serverConnction = server;
		}

		@Override
		public void run() {
		
			DataInputStream ins =  SocketIO.getDataInput(localRequest);
			DataOutputStream output = SocketIO.getDataOutput(serverConnction);
			try {
				if(ins != null) {
					byte[] buffer = new byte[BUFFER_SIZE];
					int length = 0;
					while((length = IOUtils.read(ins, buffer, BUFFER_SIZE)) > 0) {
						int w = IOUtils.write(output, buffer, length);
					}
					
					output.flush();
				}
			}catch (Throwable t) {
				ExceptionHandler.handleException(t);
			}finally {
				IOUtils.safeClose(ins);
				IOUtils.safeClose(output);
				IOUtils.safeClose(localRequest);
			}
		}
	
	}
	

	public void request() {
		try {
			String encoding = mConfiguration.getProperty("socketStreamEncoding");
			

			DataOutputStream output = SocketIO.getDataOutput(mConnection);
			String user = "name";
			byte[] bytes = user.getBytes(encoding);
			int length = bytes.length;
			int pwd = 123;

			output.write(length);
			output.write(bytes);
			output.write(pwd);

			DataInputStream input = SocketIO.getDataInput(mConnection);
			length = input.readShort();
			bytes = new byte[length];
			input.read(bytes);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				mConnection.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void handshakeCompleted(HandshakeCompletedEvent arg0) {
		Log.i(TAG, "Handshake finished successfully");
		mInitSuccess = true;
	}
}
