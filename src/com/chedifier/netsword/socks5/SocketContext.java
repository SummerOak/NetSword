package com.chedifier.netsword.socks5;

import java.io.DataInputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.chedifier.netsword.base.IOUtils;
import com.chedifier.netsword.base.SocketIO;

public class SocketContext {
	
	private Socket mServer;
	private Socket mClient;
	
	protected DataInputStream mCIS;
	protected OutputStream mCOS;
	protected DataInputStream mSIS;
	protected OutputStream mSOS;

	public SocketContext(Socket client,Socket server) {
		mServer = server;
		mClient = client;
		
		mSIS = SocketIO.getDataInput(mServer);
		mCIS = SocketIO.getDataInput(mClient);
		mSOS = SocketIO.getDataOutput(mServer);
		mCOS = SocketIO.getDataOutput(mClient);
	}
	
	public DataInputStream getClientInputStream() {
		return mCIS;
	}
	
	public OutputStream getClientOutputStream() {
		return mCOS;
	}
	
	public DataInputStream getServerInputStream() {
		return mSIS;
	}
	
	public OutputStream getServerOutputStream() {
		return mSOS;
	}
	

	public void updateClientSocket(Socket socket) {
		mClient = socket;
		mCIS = SocketIO.getDataInput(mClient);
		mCOS = SocketIO.getDataOutput(mClient);
	}
	
	public void updateServerSocket(Socket socket) {
		mServer = socket;
		mSIS = SocketIO.getDataInput(mServer);
		mSOS = SocketIO.getDataOutput(mServer);
	}
	
	public void destroy() {
		IOUtils.safeClose(mCIS);
		IOUtils.safeClose(mSIS);
		IOUtils.safeClose(mCOS);
		IOUtils.safeClose(mSOS);
		IOUtils.safeClose(mServer);
		IOUtils.safeClose(mClient);
	}
	
}
