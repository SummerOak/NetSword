package com.chedifier.netsword.socks5;

import java.io.DataInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import com.chedifier.netsword.ExceptionHandler;
import com.chedifier.netsword.IOUtils;
import com.chedifier.netsword.Log;
import com.chedifier.netsword.NetUtils;
import com.chedifier.netsword.Result;
import com.chedifier.netsword.StringUtils;

public class S5ConnStage extends AbsS5Stage{
	private String TAG = "S5ConnStage";
	
	private ConnInfo mConnInfo;
	private ConnInfo mConnInfoServer;
	
	public S5ConnStage(AbsS5Stage stage) {
		super(stage);
	}

	@Override
	public Result forward() {
		return new S5TransStage(this).handle();
	}

	@Override
	public Result handle() {
		Result result;
		mConnInfo = new ConnInfo();
		if((result = readConnInfo(getContext().getClientInputStream(),mConnInfo)) != Result.SUCCESS){
			Log.i(TAG, "parse conn info failed.");
			return result;
		}
		
		if(isLocal()) {
			Log.i(TAG, "sending conn request to server...");
			if((result = writeConn(getContext().getServerOutputStream(), mConnInfo)) != Result.SUCCESS) {
				Log.i(TAG, "relay conn info to server failed.");
				return result;
			}
			
			if((result = readConnInfo(getContext().getServerInputStream(), mConnInfoServer)) != Result.SUCCESS) {
				Log.i(TAG, "read conn info from server failed.");
				return result;
			}
			
			if((result = writeConn(getContext().getClientOutputStream(), mConnInfoServer)) != Result.SUCCESS) {
				Log.i(TAG, "write conn info to client failed.");
				return result;
			}
			
			Log.i(TAG, "conn request send to proxy succ.");
		}else {
			if(mConnInfo.addrInfo.ip != null) {
				mConnInfo.netAddr = NetUtils.resolveAddrByIP(mConnInfo.addrInfo.ip);
			}else if(mConnInfo.addrInfo.domain != null) {
				mConnInfo.netAddr = NetUtils.resolveAddrByDomain(mConnInfo.addrInfo.domain);
			}
			
			if(mConnInfo.netAddr == null) {
				Log.i(TAG, "resolve remote addr failed.");
				return Result.E_S5_CONN_SEND_SERVER;
			}
			
			Socket destSocket;
			if((destSocket = connectDest(mConnInfo, 10)) == null) {
				Log.i(TAG, "connect to remote failed.");
				return Result.E_S5_CONN_SEND_SERVER;
			}
			
			getContext().updateServerSocket(destSocket);
			
			if((result = writeConn(getContext().getClientOutputStream(), mConnInfo)) != Result.SUCCESS) {
				Log.i(TAG, "write conn info to local failed.");
				return result;
			}
		}
		
		return forward();
	}
	
	private Socket connectDest(ConnInfo connInfo,int timeout) {
		Log.i(TAG, "connecting dest: " + connInfo.netAddr.getHostName() + " " + connInfo.netAddr.getHostAddress() + "  " + connInfo.addrInfo.port);
		Socket socket = new Socket();
		SocketAddress address = new InetSocketAddress(connInfo.netAddr, connInfo.addrInfo.port);
		
		try {
			socket.connect(address, timeout);
		} catch (Throwable e) {
			ExceptionHandler.handleException(e);
			return null;
		}finally {
			IOUtils.safeClose(socket);
			socket = null;
		}
		
		return null;
	}
	
	private Result readConnInfo(DataInputStream is, ConnInfo connInfo) {
		final int L = 1024;
		byte[] data = new byte[L];
		if(IOUtils.read(is, data, 4) != 4) {
			Log.i(TAG, "read connection failed.");
			return Result.E_S5_CONN_READ_HEAD;
		}
		
		Log.i(TAG, "receive conn: " + StringUtils.toRawString(data, 4));
		
		if(data[0] != 0x05) {
			Log.i(TAG, "not socks5");
			return Result.E_S5_CONN_VER;
		}
		
		connInfo.connCmd = data[1];
		AddrInfo addrInfo = new AddrInfo();
		connInfo.addrInfo = addrInfo;
		addrInfo.addrtp = data[3];
		switch(addrInfo.addrtp) {
			case 0x01:{
				addrInfo.ip = new byte[4];
				if(IOUtils.read(is, addrInfo.ip, 4) != 4) {
					Log.i(TAG, "read ipv4 failed.");
					return Result.E_S5_CONN_READ_IPV4;
				}
				
				Log.i(TAG, "ipv4: " + StringUtils.toRawString(addrInfo.ip, 4));
				break;
			}
				
			case 0x03:{
				if(IOUtils.read(is,data,0,1) != 1) {
					Log.i(TAG, "read domain length failed.");
					return Result.E_S5_CONN_READ_DOMAIN;
				}
				
				int len = data[0];
				if(IOUtils.read(is,data, 0,len) != len) {
					Log.i(TAG, "read domain failed.");
					return Result.E_S5_CONN_READ_DOMAIN;
				}
				
				addrInfo.domain = StringUtils.toString(data, len);
				Log.i(TAG, "domain: " + addrInfo.domain);
				
				break;
			}
			case 0x04:{
				addrInfo.ip = new byte[16];
				if(IOUtils.read(is,addrInfo.ip, 16) != 16) {
					Log.i(TAG, "read ipv6 failed.");
					return Result.E_S5_CONN_READ_IPV6;
				}
				Log.i(TAG, "ipv6: " + StringUtils.toRawString(data, 16));
				
				break;
			}
		}
		
		
		if(IOUtils.read(is,data, 2) != 2) {
			Log.i(TAG, "read port failed.");
			return Result.E_S5_CONN_READ_PORT;
		}
		
		addrInfo.port = ((data[0]) << 8) | (data[1] & 0xFF);
		
		Log.i(TAG, "port: " + StringUtils.toRawString(data, 2) + "  " + addrInfo.port);
		
		return Result.SUCCESS;
	}
	
	private Result writeConn(OutputStream os,ConnInfo connInfo) {
		AddrInfo addrInfo = connInfo.addrInfo;
		byte addrtp = 0x00;
		if(addrInfo.ip != null) {
			addrtp = (byte) (addrInfo.ip.length == 4? 0x01:(addrInfo.ip.length == 16?0x04:0x00));
		}else if(addrInfo.domain != null && addrInfo.domain.length() < Byte.MAX_VALUE) {
			addrtp = 0x03;
		}
		
		if(addrtp == 0x00) {
			Log.i(TAG, "get addr type failed.");
			return Result.E_S5_CONN_READ_IPV4;
		}
		
		if(IOUtils.write(os, new byte[] {0x05,connInfo.connCmd,0x00,addrtp}, 4) != 4) {
			Log.i(TAG, "write conn info head failed.");
			return Result.E_S5_CONN_READ_HEAD;
		}
		
		switch(addrtp) {
			case 0x01:{
				if(IOUtils.write(os, addrInfo.ip, 4) != 4) {
					Log.i(TAG, "write conn addr ipv4 failed.");
					return Result.E_S5_CONN_READ_IPV4;
				}
				break;
			}
			
			case 0x03:{
				if(IOUtils.write(os, new byte[] {(byte)addrInfo.domain.length()},1) != 1) {
					Log.i(TAG, "write conn addr domain len failed.");
					return Result.E_S5_CONN_READ_IPV4;
				}
				
				if(IOUtils.write(os, addrInfo.domain.getBytes(),addrInfo.domain.length()) != addrInfo.domain.length()) {
					Log.i(TAG, "write conn addr domain failed.");
					return Result.E_S5_CONN_READ_IPV4;
				}
				
				break;
			}
			
			case 0x04:{
				if(IOUtils.write(os, addrInfo.ip, 16) != 16) {
					Log.i(TAG, "write conn addr ipv6 failed.");
					return Result.E_S5_CONN_READ_IPV4;
				}
				break;
			}
		}
		
		if(IOUtils.write(os, new byte[] {(byte)((addrInfo.port>>8) & 0xff),((byte)(addrInfo.port & 0xff))}, 2) != 2) {
			Log.i(TAG, "write port failed");
			return Result.E_S5_CONN_READ_PORT;
		}
		
		return Result.SUCCESS;
	}
	
	private final class ConnInfo{
		private byte connCmd;
		private AddrInfo addrInfo;
		private InetAddress netAddr;
	}
	
	private final class AddrInfo{
		private byte[] ip;
		private String domain;
		private byte addrtp;
		
		private int port;
	}
	
	

}
