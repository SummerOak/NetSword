package com.chedifier.netsword.socks5;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.chedifier.netsword.base.ExceptionHandler;
import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.NetUtils;
import com.chedifier.netsword.base.StringUtils;
import com.chedifier.netsword.cipher.Cipher;
import com.chedifier.netsword.cipher.Cipher.DecryptResult;

public class S5ConnStage extends AbsS5Stage{
	private ConnInfo mConnInfo = new ConnInfo();
	public S5ConnStage(AbsS5Stage stage) {
		super(stage);
		
	}
	
	@Override
	public void start() {
		Log.r(getTag(), "S5ConnStage start>>>");
		super.start();
	}

	@Override
	public AbsS5Stage next() {
		return new S5TransStage(this);
	}
	
	@Override
	public void onSourceOpts(int opts) {
		Log.d(getTag(), "onSourceOpts " + opts + " " + isLocal());
		if((opts&SelectionKey.OP_READ) > 0) {
			ByteBuffer buffer = getChannel().getSrcInBuffer();
			if(isLocal()) {
				Log.i(getTag(),"recv conn info: " + StringUtils.toRawString(buffer.array(), buffer.position()));
				int result = buildConnInfo(mConnInfo,buffer.array(), 0, buffer.position());
				if(result > 0){
					Log.d(getTag(), "recv conn info success. " + mConnInfo);
					
					if((mConnInfo.connCmd&0xFF) != 0x01 && (mConnInfo.connCmd&0xFF) != 0x02) {
						Log.e(getTag(), "this conn cmd is not support now: " + mConnInfo.connCmd);
						notifyError(Result.E_S5_SOCKET_ERROR_CONN);
						return;
					}
					
					byte[] data = Cipher.encrypt(buffer.array(), 0, buffer.position());
					if(getChannel().writeToBuffer(true, data) == data.length) {
						buffer.clear();
					}else {
						Log.e(getTag(), "send conn info to server failed.");
					}
				}else {
					Log.e(getTag(), "build conn info failed.");
					notifyError(Result.E_S5_CONN_BUILD_CONN_INFO_FAILED);
					return;
				}
			}else {
				Log.d(getTag(), "decrypt buffer: " + StringUtils.toRawString(buffer.array(),0,buffer.position()));
				DecryptResult decResult = Cipher.decrypt(buffer.array(), 0, buffer.position());
				if(decResult != null && decResult.origin != null && decResult.origin.length > 0 && decResult.decryptLen > 0) {
					byte[] origin = decResult.origin;
					Log.i(getTag(),"recv conn info: " + StringUtils.toRawString(origin));
					int buildConnInfoResult = buildConnInfo(mConnInfo,origin, 0, origin.length);
					if(buildConnInfoResult > 0) {
						Log.d(getTag(), "build conn info success.");
						InetSocketAddress remoteAddr = buildRemoteAddress(mConnInfo);
						Log.d(getTag(), "build remote address: " + remoteAddr);
						if(remoteAddr != null) {
							Log.d(getTag(), "bind to remote " + remoteAddr);
							SocketChannel remoteChannel = NetUtils.bindSServer(remoteAddr);
							Log.d(getTag(), "bind to remote return " + remoteChannel);
							if(remoteChannel != null) {
								getChannel().setDest(remoteChannel);
								ByteBuffer rep = ByteBuffer.wrap(new byte[1024]);
								byte addrType = mConnInfo.addrInfo.addrtp;
								rep.put(new byte[]{0x05,0x00,0x00,addrType});
								if(addrType == 0x03) {									
									rep.put((byte)(mConnInfo.addrInfo.addr.length&0xFF));
								}
								rep.put(mConnInfo.addrInfo.addr);
								rep.put(mConnInfo.addrInfo._port);
								byte[] data = Cipher.encrypt(rep.array(),0,rep.position());
								
								if(getChannel().writeToBuffer(false, data) == data.length) {
									getChannel().cutBuffer(buffer, decResult.decryptLen);
									forward();
									return;
								}
							}else {
								Log.e(getTag(), "bind remote failed: " + remoteAddr);
								notifyError(Result.E_S5_CONN_BIND_REMOTE);
								return;
							}
						}else {
							Log.e(getTag(), "receive wrong connect request.");
							notifyError(Result.E_S5_CONN_BIND_REMOTE);
							return;
						}
					}else if(buildConnInfoResult < 0) {
						Log.e(getTag(), "build conn info failed.");
						notifyError(Result.E_S5_CONN_BUILD_CONN_INFO_FAILED);
						return;
					}
				}
			}
			
			return;
		}
		
//		Log.e(getTag(), "unexpected opts " + opts + " from src.");
	}
	
	private InetSocketAddress buildRemoteAddress(ConnInfo connInfo) {
		byte[] addr = connInfo.addrInfo.addr;
		int port = connInfo.addrInfo.port;
		InetSocketAddress netAddr = null;
		if(connInfo.addrInfo.addrtp == 0x03) {
			netAddr = new InetSocketAddress(StringUtils.toString(addr, addr.length), port);
		}else {
			try {
				netAddr = new InetSocketAddress(InetAddress.getByAddress(addr), port);
			} catch (Throwable e) {
				ExceptionHandler.handleException(e);
			}
		}
		return netAddr;
	}

	@Override
	public void onDestOpts(int opts) {
		if(isLocal()) {
			if((opts&SelectionKey.OP_READ) > 0) {
				ByteBuffer buffer = getChannel().getDestInBuffer();
				Log.i(getTag(),"recv conn from server: " + StringUtils.toRawString(buffer.array(), buffer.position()));
				DecryptResult decrypt = Cipher.decrypt(buffer.array(), 0, buffer.position());
				if(decrypt != null && decrypt.origin != null && decrypt.origin.length > 0 && decrypt.decryptLen > 0) {
					if(getChannel().writeToBuffer(false, decrypt.origin) == decrypt.origin.length) {
						getChannel().cutBuffer(buffer, decrypt.decryptLen);
						
						forward();
					}
				}
				
				return;
			}
		}
		
//		Log.e(getTag(), "unexpected opts " + opts + " from src.");
	}
	
	@Override
	public void onSocketBroken() {
		notifyError(Result.E_S5_SOCKET_ERROR_CONN);
	}

	private int buildConnInfo(ConnInfo connInfo,byte[] data,int offset,int len) {
		int p = offset;
		
		p += 4;
		if(len > p) {
			if((data[0]&0xFF) != 0x05) {
				Log.e(getTag(), "not socks5 protocal");
				return -1;
			}
			
			AddrInfo addrInfo = null;
			
			byte connCmd = data[1];
			byte addrType = data[3];
			switch(addrType) {
				case 0x01:{
					if((len-p) == 4 + 2) {
						addrInfo = new AddrInfo();
						addrInfo.addrtp = 0x01;
						addrInfo.addr = new byte[4];
						
						System.arraycopy(data, 4, addrInfo.addr, 0, 4);
						p += 4;
						break;
					}
					return 0;
				}
				case 0x03:{
					if((len-p) > 1) {
						int domainL = data[p];
						p += 1;
						
						if((len-p) == domainL+2) {
							addrInfo = new AddrInfo();
							addrInfo.addrtp = 0x03;
							addrInfo.addr = new byte[domainL];
							System.arraycopy(data, p, addrInfo.addr, 0, domainL);
							p += domainL;
							break;
						}
					}
					
					return 0;
				}
				case 0x04:{
					if((len-p) == 6 + 2) {
						addrInfo = new AddrInfo();
						addrInfo.addrtp = 0x04;
						addrInfo.addr = new byte[6];
						
						System.arraycopy(data, 6, addrInfo.addr, 0, 6);
						p += 6;
						break;
					}
					return 0;
				}
				
				default:{
					Log.e(getTag(), "wrong address type.");
					return -1;
				}
					
			}
			
			if(addrInfo != null && len == (p+2)) {
				byte[] _port = new byte[2];
				_port[0] = data[p];_port[1] = data[p+1];
				addrInfo._port = _port;
				addrInfo.port = ((data[p]) << 8) | (data[p+1] & 0xFF);
				mConnInfo.addrInfo = addrInfo;
				mConnInfo.connCmd = connCmd;
				
				return 1;
			}
		}
		
		return 0;
	}

	private final class ConnInfo{
		private byte connCmd;
		private AddrInfo addrInfo;
		private InetAddress netAddr;
		
		@Override
		public String toString() {
			return "conn: " + (connCmd&0xFF) + " addr " + addrInfo + " resolved: " + netAddr;
		}
	}
	
	private final class AddrInfo{
		private byte[] addr;
		private byte addrtp;
		
		private int port;
		private byte[] _port;
		
		@Override
		public String toString() {
			return "ip: " + (addr == null? "null":
					(addrtp==0x03?StringUtils.toRawString(addr, addr.length):
						StringUtils.toString(addr, addr.length))) + 
					" port " + port;
		}
	}
	
}
