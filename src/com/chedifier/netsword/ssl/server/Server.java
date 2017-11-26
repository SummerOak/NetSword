package com.chedifier.netsword.ssl.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import com.chedifier.netsword.ExceptionHandler;
import com.chedifier.netsword.Log;
import com.chedifier.netsword.ssl.Configuration;

public class Server {
	private static final String TAG = "Server";

	private SSLContext mSSLContext;
	private SSLServerSocket sslServerSocket;
	private final Executor executor;

	public Server() {
		Properties p = Configuration.getConfig();
		Integer serverListenPort = Integer.valueOf(p.getProperty("server_port"));
		Integer serverThreadPoolSize = Integer.valueOf(p.getProperty("serverThreadPoolSize"));
		Integer serverRequestQueueSize = Integer.valueOf(p.getProperty("serverRequestQueueSize"));
		Integer authority = Integer.valueOf(p.getProperty("authority"));

		executor = Executors.newFixedThreadPool(serverThreadPoolSize);

		mSSLContext = Auth.getSSLContext();
		if(mSSLContext == null) {
			Log.i(TAG, "get ssl context failed");
			return;
		}
		
		try {
			SSLServerSocketFactory sslServerSocketFactory = mSSLContext.getServerSocketFactory();
			sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket();
		}catch(Throwable t) {
			ExceptionHandler.handleException(t);
			Log.i(TAG, "create server socket failed");
			return;
		}
		
		String[] pwdsuits = sslServerSocket.getSupportedCipherSuites();
		sslServerSocket.setEnabledCipherSuites(pwdsuits);

		sslServerSocket.setUseClientMode(false);
		if (authority.intValue() == 2) {
			sslServerSocket.setNeedClientAuth(true);
		} else {
			sslServerSocket.setWantClientAuth(true);
		}

		try {
			sslServerSocket.setReuseAddress(true);
			sslServerSocket.setReceiveBufferSize(128 * 1024);
			sslServerSocket.setPerformancePreferences(3, 2, 1);
			sslServerSocket.bind(new InetSocketAddress(serverListenPort), serverRequestQueueSize);
		} catch (Throwable e) {
			ExceptionHandler.handleException(e);
			Log.i(TAG, "server start failed");
			return;
		}
		
		Log.i(TAG, "Server start up! server port is: " + serverListenPort);
	}

	public void service() {
		while (true) {
			SSLSocket socket = null;
			try {
				Log.i(TAG, "Wait for client request!");
				socket = (SSLSocket) sslServerSocket.accept();
				Log.i(TAG, "Get client request!");

				Runnable job = new Job(socket);
				executor.execute(job);
			} catch (Exception e) {
				Log.i(TAG, "server run exception");
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
}
