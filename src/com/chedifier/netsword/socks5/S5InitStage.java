package com.chedifier.netsword.socks5;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.StringUtils;
import com.chedifier.netsword.cipher.Cipher;
import com.chedifier.netsword.iface.Error;
import com.chedifier.netsword.iface.SProxyIface;
import com.chedifier.netsword.memory.ByteBufferPool;
import com.chedifier.netsword.socks5.version.InitFeedback;
import com.chedifier.netsword.socks5.version.InitInfo;
import com.chedifier.netsword.socks5.version.Parcel;

public class S5InitStage extends AbsS5Stage{
	
	private static final InitInfo.V1 sInitInfo = new InitInfo.V1();
	{
		sInitInfo.versionCode = 1001;
		sInitInfo.versionName = "v1.0";
		sInitInfo.userName = "testUser";
		sInitInfo.password = "testPwd";
	}
	
	private boolean mInitFailed = false;

	public S5InitStage(SSockChannel channel,boolean isLocal,ICallback callback) {
		super(channel,isLocal,callback);
	}

	@Override
	public void start() {
		Log.r(getTag(), "S5InitStage start>>>");
		super.start();
		
		if(!isLocal()) {//client reading ops cannot open at this stage
			getChannel().updateOps(true, true, SelectionKey.OP_READ);
		}
		
		notifyState(SProxyIface.STATE.INIT);
	}

	@Override
	public AbsS5Stage next() {
		return new S5VerifyStage(this);
	}
	
	@Override
	public void onSourceOpts(int opts) {
		if(!isLocal()) {
			if((opts&SelectionKey.OP_READ) > 0) {
				//step 2: proxy server read and check init info;
				ByteBuffer buffer = getChannel().getSrcInBuffer();
				int infoLen = buffer.position();
				ByteBuffer decOutBuffer = ByteBufferPool.obtain(Cipher.estimateDecryptLen(infoLen,getChannel().getChunkSize()));
				if(decOutBuffer == null) {
					Log.e(getTag(), "obtain decOutBuffer failed");
					return;
				}
				
				int dl = Cipher.decrypt(buffer.array(), 0, infoLen,getChannel().getChunkSize(),decOutBuffer);
				Log.t(getTag(), "recv initInfo1:" + StringUtils.toRawString(buffer.array(),infoLen));
				if(dl > 0) {
					Log.t(getTag(), "recv initInfo:" + StringUtils.toRawString(decOutBuffer.array(),decOutBuffer.position()));
					Error result = verifyInitInfo(decOutBuffer.array(),0,decOutBuffer.position());
					if(result == Error.SUCCESS) {
						//step 3: proxy server verify init info successed, send feedback to local
						byte[] feedback = getFeedback(result);
						ByteBuffer outBuffer = ByteBufferPool.obtain(Cipher.estimateEncryptLen(feedback.length, getChannel().getChunkSize()));
						int el = Cipher.encrypt(feedback,getChannel().getChunkSize(),outBuffer);
						if(el > 0) {
							outBuffer.flip();
							int l = outBuffer.remaining();
							if(getChannel().writeToBuffer(false, outBuffer) == l) {
								getChannel().cutBuffer(buffer, dl);
								forward();//finished init info check in server,move on
							}
						}
						ByteBufferPool.recycle(outBuffer);
					}else if(result == Error.E_S5_SOCKET_PARCEL_NOT_FINISH) {
						Log.d(getTag(), "uncomplete parcel.");
					}else {
						Log.e(getTag(), "recv init info successed, init failed,sending failed feedback to local.");
						getChannel().cutBuffer(buffer, dl);
						byte[] feedback = getFeedback(result);
						ByteBuffer outBuffer = ByteBufferPool.obtain(Cipher.estimateEncryptLen(feedback==null?0:feedback.length, getChannel().getChunkSize()));
						if(outBuffer != null) {
							int l = Cipher.encrypt(feedback,getChannel().getChunkSize(),outBuffer);
							if(l > 0) {
								outBuffer.flip();
								int ll = outBuffer.remaining();
								if(getChannel().writeToBuffer(false, outBuffer) == ll) {
									getChannel().cutBuffer(buffer, dl);
									mInitFailed = true;
								}
							}
							ByteBufferPool.recycle(outBuffer);
						}else {
							Log.e(getTag(), "obtain out buffer failed.");
						}
					}
				}
				ByteBufferPool.recycle(decOutBuffer);
			}else if((opts&SelectionKey.OP_WRITE) > 0) {
				if(mInitFailed && getChannel().getSrcOutBuffer().position() <= 0) {
					Log.d(getTag(), "init failed and feedback had send back to local, close channel.");
					notifyError(Error.E_S5_SOCKET_ERROR_INIT);
				}
			}
		}
	}

	@Override
	public void onDestOpts(int opts) {
		if(isLocal()) {
			if((opts&SelectionKey.OP_CONNECT) > 0){
				//step 1: when proxy server connected, send init info to proxy server
				byte[] initInfo1 = buildInitInfo();
				ByteBuffer outBuffer = ByteBufferPool.obtain(Cipher.estimateEncryptLen(initInfo1==null?0:initInfo1.length, getChannel().getChunkSize()));
				int l = Cipher.encrypt(initInfo1,getChannel().getChunkSize(),outBuffer);
				if(l > 0) {
					Log.t(getTag(), "send initInfo1:" + StringUtils.toRawString(initInfo1));
					Log.t(getTag(), "send initInfo:" + StringUtils.toRawString(outBuffer.array(),0,outBuffer.position()));
					outBuffer.flip();
					int ll = outBuffer.remaining();
					if(getChannel().writeToBuffer(true, outBuffer) == ll) {
						Log.d(getTag(), "send init info to server success.");
					}else {
						Log.e(getTag(), "send init info to server failed.");
					}
				}else {
					Log.e(getTag(), "illegal init info");
					notifyError(Error.E_S5_SOCKET_ERROR_INIT);
				}
				ByteBufferPool.recycle(outBuffer);
			}else if((opts&SelectionKey.OP_READ) > 0) {
				//step 4: local receive feedback
				ByteBuffer buffer = getChannel().getDestInBuffer();
				int infoLen = buffer.position();
				ByteBuffer decOutBuffer = ByteBufferPool.obtain(Cipher.estimateDecryptLen(infoLen,getChannel().getChunkSize()));
				if(decOutBuffer == null) {
					Log.e(getTag(), "obtain out buffer for decrypt failed.");
					return;
				}
				int dl = Cipher.decrypt(buffer.array(), 0, infoLen,getChannel().getChunkSize(),decOutBuffer);
				if(dl > 0) {
					InitFeedback.V1 feedback = new InitFeedback.V1();
					Error result = parseFeedback(decOutBuffer.array(),0,decOutBuffer.position(),feedback);
					if(result == Error.SUCCESS) {
						if(feedback.error == Error.SUCCESS) {
							getChannel().cutBuffer(buffer, dl);
							forward();//check init info successed in local , move on
						}else if(feedback.error == Error.E_S5_SOCKET_ERROR_INIT) {
							Log.e(getTag(), "feedback recv successed, init error.");
							notifyError(feedback.error);
						}
					}else if(result == Error.E_S5_SOCKET_PARCEL_NOT_FINISH){
						Log.d(getTag(), "build init feedback failed,uncomplete parcel.");
					}else {
						Log.e(getTag(), "feedback recv successed, init error.");
						notifyError(feedback.error);
					}
				}
				
				ByteBufferPool.recycle(decOutBuffer);
			}
		}
		
	}
	
	private byte[] buildInitInfo() {
		return sInitInfo.parcel().getBytes();
	}
	
	private byte[] getFeedback(Error result) {
		InitFeedback.V1 feedback = new InitFeedback.V1();
		feedback.error = result;
		feedback.extra = result.getMessage();
		return feedback.parcel().getBytes();
	}
	
	private Error parseFeedback(byte[] raw,int offset,int len,InitFeedback.V1 out) {
		Log.t(getTag(), "parse feedback: " + StringUtils.toRawString(raw));
		if(raw == null || raw.length <= 0) {
			return null;
		}
		
		Parcel parcel = Parcel.createParcelWithData(raw, offset, len);
		if(parcel == null) {
			return Error.E_S5_SOCKET_PARCEL_NOT_FINISH;
		}
		
		if(out.parcel(parcel) == out) {
			return Error.SUCCESS;
		}
		
		return Error.E_S5_SOCKET_ERROR_INIT;
	}
	
	private Error verifyInitInfo(byte[] info,int offset,int len) {
		Log.t(getTag(), "init info: " + StringUtils.toRawString(info,offset,len));
		if(info == null || info.length <= 0) {
			return Error.E_S5_SOCKET_ERROR_INIT;
		}
		
		Parcel parcel = Parcel.createParcelWithData(info, offset, len);
		if(parcel == null) {
			return Error.E_S5_SOCKET_PARCEL_NOT_FINISH;
		}
		InitInfo.V1 initInfo = new InitInfo.V1();
		if(initInfo.parcel(parcel) == initInfo) {
			return Error.SUCCESS;
		}
		
		return Error.E_S5_SOCKET_ERROR_INIT;
	}

	@Override
	public void onSocketBroken(Error result) {
		notifyError(result);
		notifyError(Error.E_S5_SOCKET_ERROR_INIT);
	}

}
