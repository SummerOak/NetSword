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
import com.chedifier.netsword.trans.Courier;
import com.chedifier.netsword.trans.Parcel;

public class S5ConnStage extends AbsS5Stage{
	private String TAG = "S5ConnStage";
	
	private ConnInfo mConnInfo;
	private ConnInfo mConnInfoServer;
	private Courier mCourier;
	
	public S5ConnStage(AbsS5Stage stage) {
		super(stage);
		
		mCourier = new Courier();
	}

	@Override
	public Result forward() {
		return new S5TransStage(this).handle();
	}

	@Override
	public Result handle() {
		Log.r(TAG, ">>>>>>");
		
		Result result;
		if(isLocal()) {
			mConnInfo = new ConnInfo();
			
			//1. read from client
			if((result = readConnInfo(getContext().getClientInputStream(),mConnInfo)) != Result.SUCCESS){
				Log.e(TAG, "read conn info from client failed.");
				return result;
			}
			
			//2. send to proxy server
			Log.i(TAG, "sending conn request to server...");
			if((result = writeConnParcel(getContext().getServerOutputStream(), mConnInfo)) != Result.SUCCESS) {
				Log.e(TAG, "relay conn info to proxy server failed.");
				return result;
			}
			
			//3. read response from proxy server
			mConnInfoServer = new ConnInfo();
			if((result = readParcelConnInfo(getContext().getServerInputStream(), mConnInfoServer)) != Result.SUCCESS) {
				Log.e(TAG, "read conn info from proxy server failed.");
				return result;
			}
			
			//4. write conn back to client
			if((result = writeConn(getContext().getClientOutputStream(), mConnInfoServer)) != Result.SUCCESS) {
				Log.e(TAG, "write conn info to client failed.");
				return result;
			}
			
			Log.d(TAG, "conn request send to proxy succ.");
		}else {
			mConnInfo = new ConnInfo();
			
			//1. read from slocal
			if((result = readParcelConnInfo(getContext().getClientInputStream(),mConnInfo)) != Result.SUCCESS){
				Log.e(TAG, "read conn info from local failed.");
				return result;
			}
			
			if(mConnInfo.addrInfo.ip != null) {
				mConnInfo.netAddr = NetUtils.resolveAddrByIP(mConnInfo.addrInfo.ip);
			}else if(mConnInfo.addrInfo.domain != null) {
				mConnInfo.netAddr = NetUtils.resolveAddrByDomain(mConnInfo.addrInfo.domain);
			}
			
			if(mConnInfo.netAddr == null) {
				Log.i(TAG, "resolve remote addr failed.");
				return Result.E_S5_CONN_SEND_SERVER;
			}
			
			Log.d(TAG, "resolve addr: " + mConnInfo.netAddr.getHostName() + " " + mConnInfo.netAddr.getHostAddress() + " port " + mConnInfo.addrInfo.port);
			
			//2. connect to remote server
			Socket destSocket;
			if((destSocket = connectDest(mConnInfo, 100000)) == null) {
				Log.e(TAG, "connect to remote failed.");
				return Result.E_S5_CONN_SEND_SERVER;
			}
			
			getContext().updateServerSocket(destSocket);
			
			//3. write back to slocal
			if((result = writeConnParcel(getContext().getClientOutputStream(), mConnInfo)) != Result.SUCCESS) {
				Log.e(TAG, "write conn info to local failed.");
				return result;
			}
		}
		
		return forward();
	}
	
	private Socket connectDest(ConnInfo connInfo,int timeout) {
		Log.d(TAG, "connecting dest: " + connInfo.netAddr.getHostName() + " " + connInfo.netAddr.getHostAddress() + "  " + connInfo.addrInfo.port);
		Socket socket = new Socket();
		SocketAddress address = new InetSocketAddress(connInfo.netAddr, connInfo.addrInfo.port);
		
		try {
			socket.connect(address, timeout);
		} catch (Throwable e) {
			ExceptionHandler.handleFatalException(e);
			IOUtils.safeClose(socket);
			socket = null;
		}
		
		return socket;
	}
	
	private Result readConnInfo(DataInputStream is, ConnInfo connInfo) {
		final int L = 1024;
		byte[] data = new byte[L];
		if(IOUtils.read(is, data, 4) != 4) {
			Log.i(TAG, "read conn head failed.");
			return Result.E_S5_CONN_READ_HEAD;
		}
		
		Log.i(TAG, "receive conn: " + StringUtils.toRawString(data, 4));
		
		if(data[0] != 0x05) {
			Log.i(TAG, "conn req ver not socks5");
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
					Log.e(TAG, "conn read ipv4 failed.");
					return Result.E_S5_CONN_READ_IPV4;
				}
				
				Log.i(TAG, "ipv4: " + StringUtils.toRawString(addrInfo.ip, 4));
				break;
			}
				
			case 0x03:{
				if(IOUtils.read(is,data,0,1) != 1) {
					Log.e(TAG, "conn read domain length failed.");
					return Result.E_S5_CONN_READ_DOMAIN;
				}
				
				int len = data[0];
				if(IOUtils.read(is,data, 0,len) != len) {
					Log.e(TAG, "read domain failed.");
					return Result.E_S5_CONN_READ_DOMAIN;
				}
				
				addrInfo.domain = StringUtils.toString(data, len);
				Log.i(TAG, "domain: " + addrInfo.domain);
				
				break;
			}
			case 0x04:{
				addrInfo.ip = new byte[16];
				if(IOUtils.read(is,addrInfo.ip, 16) != 16) {
					Log.e(TAG, "conn read ipv6 failed.");
					return Result.E_S5_CONN_READ_IPV6;
				}
				Log.i(TAG, "ipv6: " + StringUtils.toRawString(data, 16));
				
				break;
			}
		}
		
		if(IOUtils.read(is,data, 2) != 2) {
			Log.e(TAG, "conn read port failed.");
			return Result.E_S5_CONN_READ_PORT;
		}
		
		addrInfo.port = ((data[0]) << 8) | (data[1] & 0xFF);
		
		Log.i(TAG, "port: " + StringUtils.toRawString(data, 2) + "  " + addrInfo.port);
		
		return Result.SUCCESS;
	}
	
	private Result readParcelConnInfo(DataInputStream is, ConnInfo connInfo) {
		Parcel parcel = mCourier.readParcel(is);
		if(parcel == null) {
			Log.i(TAG, "read parcel conn info failed.");
			return Result.E_S5_CONN_READ_HEAD;
		}
		
		byte[] data = parcel.getData();
		int len = parcel.size();
		Log.i(TAG, "receive conn: " + StringUtils.toRawString(data, len));
		
		if(data[0] != 0x05) {
			Log.i(TAG, "conn req ver not socks5");
			return Result.E_S5_CONN_VER;
		}
		
		int p = 0;
		connInfo.connCmd = data[1];
		AddrInfo addrInfo = new AddrInfo();
		connInfo.addrInfo = addrInfo;
		addrInfo.addrtp = data[3];
		p+=4;
		
		switch(addrInfo.addrtp) {
			case 0x01:{
				if(len < 4+4) {
					Log.e(TAG, "conn read ipv4 failed.");
					return Result.E_S5_CONN_READ_IPV4;
				}
				
				addrInfo.ip = new byte[4];
				System.arraycopy(data, p, addrInfo.ip, 0, 4);
				p+=4;
				Log.i(TAG, "ipv4: " + StringUtils.toRawString(addrInfo.ip, 4));
				break;
			}
				
			case 0x03:{
				if(len < 4) {
					Log.e(TAG, "read domain failed.");
					return Result.E_S5_CONN_READ_DOMAIN;
				}
				
				int domainLen = data[p];
				if(len < p + 1 + domainLen) {
					Log.e(TAG, "read domain failed.");
					return Result.E_S5_CONN_READ_DOMAIN;
				}
				
				addrInfo.domain = StringUtils.toString(data, 4+1,domainLen);
				p+=(domainLen+1);
				Log.i(TAG, "domain: " + addrInfo.domain);
				
				break;
			}
			case 0x04:{
				if(len < p+16) {
					Log.e(TAG, "conn read ipv6 failed.");
					return Result.E_S5_CONN_READ_IPV6;
				}
				
				addrInfo.ip = new byte[16];
				System.arraycopy(data, p, addrInfo.ip, 0, 16);
				p+=16;
				Log.i(TAG, "ipv6: " + StringUtils.toRawString(addrInfo.ip, 16));
				break;
			}
		}
		
		if(len < p+2) {
			Log.e(TAG, "conn read port failed.");
			return Result.E_S5_CONN_READ_PORT;
		}
		
		addrInfo.port = ((data[p]&0xFF) << 8) | (data[p+1] & 0xFF);
		
		Log.i(TAG, "port: " + StringUtils.toRawString(data, p,2) + "  " + addrInfo.port);
		
		return Result.SUCCESS;
	}
	
	private Result writeConnParcel(OutputStream os,ConnInfo connInfo) {
		Parcel parcel = new Parcel();
		AddrInfo addrInfo = connInfo.addrInfo;
		byte addrtp = 0x00;
		if(addrInfo.ip != null) {
			addrtp = (byte) (addrInfo.ip.length == 4? 0x01:(addrInfo.ip.length == 16?0x04:0x00));
		}else if(addrInfo.domain != null && addrInfo.domain.length() < Byte.MAX_VALUE) {
			addrtp = 0x03;
		}
		
		if(addrtp == 0x00) {
			Log.i(TAG, "write conn,invalidate head");
			return Result.E_S5_CONN_INVALIDATE_HEAD;
		}
		
		parcel.append(new byte[] {0x05,connInfo.connCmd,0x00,addrtp});
		switch(addrtp) {
			case 0x01:
			case 0x04:{
				parcel.append(addrInfo.ip);
				break;
			}
			
			case 0x03:{
				parcel.append(new byte[] {(byte)addrInfo.domain.length()});
				parcel.append(addrInfo.domain.getBytes());
				break;
			}
		}
		
		parcel.append(new byte[] {(byte)((addrInfo.port>>8) & 0xff),((byte)(addrInfo.port & 0xff))});
		if(!mCourier.writeParcel(parcel, os)) {
			Log.e(TAG, "write conn parcel failed.");
			return Result.E_S5_CONN_WIRTE_HEAD;
		}
		
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
			Log.i(TAG, "write conn,invalidate head");
			return Result.E_S5_CONN_INVALIDATE_HEAD;
		}
		
		if(IOUtils.write(os, new byte[] {0x05,connInfo.connCmd,0x00,addrtp}, 4) != 4) {
			Log.e(TAG, "write conn info head failed.");
			return Result.E_S5_CONN_WIRTE_HEAD;
		}
		
		switch(addrtp) {
			case 0x01:{
				if(IOUtils.write(os, addrInfo.ip, 4) != 4) {
					Log.e(TAG, "write conn addr ipv4 failed.");
					return Result.E_S5_CONN_WRITE_IPV4;
				}
				break;
			}
			
			case 0x03:{
				if(IOUtils.write(os, new byte[] {(byte)addrInfo.domain.length()},1) != 1) {
					Log.i(TAG, "write conn addr domain len failed.");
					return Result.E_S5_CONN_WRITE_DOMAIN;
				}
				
				if(IOUtils.write(os, addrInfo.domain.getBytes(),addrInfo.domain.length()) != addrInfo.domain.length()) {
					Log.e(TAG, "write conn addr domain failed.");
					return Result.E_S5_CONN_WRITE_DOMAIN;
				}
				
				break;
			}
			
			case 0x04:{
				if(IOUtils.write(os, addrInfo.ip, 16) != 16) {
					Log.e(TAG, "write conn addr ipv6 failed.");
					return Result.E_S5_CONN_WRITE_IPV6;
				}
				break;
			}
		}
		
		if(IOUtils.write(os, new byte[] {(byte)((addrInfo.port>>8) & 0xff),((byte)(addrInfo.port & 0xff))}, 2) != 2) {
			Log.e(TAG, "write port failed");
			return Result.E_S5_CONN_WRITE_PORT;
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
