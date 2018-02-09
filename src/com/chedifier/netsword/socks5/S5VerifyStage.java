package com.chedifier.netsword.socks5;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.StringUtils;
import com.chedifier.netsword.cipher.Cipher;
import com.chedifier.netsword.cipher.Cipher.DecryptResult;

public class S5VerifyStage extends AbsS5Stage{
	
	public S5VerifyStage(SSockChannel context,boolean isLocal,ICallback callback) {
		super(context,isLocal,callback);
	}
	
	@Override
	public void start() {
		Log.r(getTag(), "S5VerifyStage start >>>");
		super.start();
	}

	@Override
	public AbsS5Stage next() {
		return new S5ConnStage(this);
	}
	
	private int verify(byte[] data,int offset,int len) {
		if(data != null) {
			if(len > 2) {
				if((data[offset]&0xFF) == (0x05)) {
					int mths = data[offset+1]&0xFF;
					if(mths > 0) {
						if(len == (mths + 2)) {
							return 1;
						}
					}else {
						return -1;
					}
				}else {
					return -1;
				}
			}
		}
		
		return 0;
	}

	@Override
	public void onSourceOpts(int opts) {
		if((opts&SelectionKey.OP_READ) > 0) {
			ByteBuffer buffer = getChannel().getSrcInBuffer();
			if(isLocal()) {
				Log.d(getTag(), "recv verify from client:" + StringUtils.toRawString(buffer.array(),buffer.position()));
				int verifyResult = verify(buffer.array(),0,buffer.position());
				if(verifyResult > 0) {
					Log.d(getTag(), "recv verify success.");
					if(getChannel().relay(true, true) != buffer.position()) {
						Log.e(getTag(), "relay verify info to server failed.");
					}
				}else if(verifyResult < 0){
					Log.e(getTag(), "verify socks5 methos failed.");
					notifyError(Result.E_S5_VERIFY_FAILED);
					return;
				}
			}else {
				DecryptResult decResult = Cipher.decrypt(buffer.array(), 0, buffer.position());
				if(decResult != null && decResult.origin != null && decResult.origin.length > 0 && decResult.decryptLen > 0) {
					byte[] data = decResult.origin;
					Log.d(getTag(), "recv verify data from local: " + StringUtils.toRawString(data));
					int verifyResult = verify(data, 0, data.length);
					if(verifyResult > 0) {
						Log.d(getTag(), "verify success.");
						
						byte[] back = Cipher.encrypt(new byte[] {0x05,0x00});
						if(getChannel().writeToBuffer(false, back) == back.length) {
							getChannel().cutBuffer(buffer, decResult.decryptLen);
							forward();
							return;
						}else {
							Log.e(getTag(), "send verify msg to remote failed.");
						}
					}else if(verifyResult < 0){
						Log.e(getTag(), "verify socks5 methos failed.");
						notifyError(Result.E_S5_VERIFY_FAILED);
						return;
					}
				}
			}
			
			return;
		}
		
//		Log.e(TAG, "unexpected opts " + opts + " from src.");
	}

	@Override
	public void onDestOpts(int opts) {
		if(isLocal()) {
			if((opts&SelectionKey.OP_READ) > 0) {
				ByteBuffer buffer = getChannel().getDestInBuffer();
				DecryptResult decrypt = Cipher.decrypt(buffer.array(), 0, buffer.position());
				if(decrypt != null && decrypt.decryptLen > 0) {
					Log.d(getTag(), "recv verify info back from server: " + StringUtils.toRawString(decrypt.origin));
					if(getChannel().writeToBuffer(false, decrypt.origin) == decrypt.origin.length) {
						getChannel().cutBuffer(buffer, decrypt.decryptLen);
						forward();
						return;
					}else {
						Log.e(getTag(), "send verify info to server failed.");
					}
				}
				
				return;
			}
		}
		
//		Log.e(TAG, "receive unexpected ops." + opts);
	}
	
	@Override
	public void onSocketBroken() {
		notifyError(Result.E_S5_SOCKET_ERROR_VERIFY);
	}
	
}
