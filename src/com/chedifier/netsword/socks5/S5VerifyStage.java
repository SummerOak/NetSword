package com.chedifier.netsword.socks5;

import com.chedifier.netsword.IOUtils;
import com.chedifier.netsword.Log;
import com.chedifier.netsword.Result;
import com.chedifier.netsword.StringUtils;

public class S5VerifyStage extends AbsS5Stage{

	private static final String TAG = "S5VerifyStage";

	public S5VerifyStage(SocketContext context,boolean isLocal) {
		super(context,isLocal);
	}

	@Override
	public Result forward() {
		return new S5ConnStage(this).handle();
	}

	@Override
	public Result handle() {
		
		final int L = 1024;
		byte[] cData = new byte[L];
		byte[] sData = new byte[L];
		
		//1. read first 2 bytes from client
		if(IOUtils.read(getContext().getClientInputStream(),cData, 2) != 2) {
			Log.e(TAG, "socks5 verify failed.");
			return Result.E_S5_VERIFY_READ_HEAD;
		}
		
		Log.i(TAG, "recv client greeting: " + StringUtils.toRawString(cData, 0, 2));
		
		if(cData[0] != 0x05) {
			Log.e(TAG, "socks5 verify failed.it is not socks5 protocol");
			return Result.E_S5_VERIFY_VER;
		}
		
		//2. read all supported verify methods from client
		if(cData[1] > 0) {
			if(IOUtils.read(getContext().getClientInputStream(), cData,2, cData[1]) != cData[1]) {
				Log.e(TAG, "socks5 verify failed.verify methods wrong");
				return Result.E_S5_VERIFY_METHOD_LEN_READ;
			}
		}
		
		Log.i(TAG, "send verify methods: " + StringUtils.toRawString(cData, 2, cData[1]));
		
		if(isLocal()) {
			//3. relay to proxy server
			int verifyLength = 2+cData[1];
			if(IOUtils.write(getContext().getServerOutputStream(), cData, verifyLength) != verifyLength) {
				Log.e(TAG, "socks5 verify failed.verify methods wrong");
				return Result.E_S5_VERIFY_SEND_PROXY;
			}
			
			//4. read feedback from server
			if(IOUtils.read(getContext().getServerInputStream(),sData, 2) != 2) {
				Log.e(TAG, "socks5 verify failed. proxy server return false data.");
				return Result.E_S5_VERIFY_READ_PROXY;
			}
			
			Log.i(TAG, "recv from server: " + StringUtils.toRawString(sData, 2));
			
			//5. relay feedback from server to client
			if(IOUtils.write(getContext().getClientOutputStream(),sData, 2) != 2) {
				Log.e(TAG, "socks5 verify failed. write proxy server return false data.");
				return Result.E_S5_VERIFY_SEND_LOCAL;
			}
		}else {
			//3. relay to proxy server
			if(IOUtils.write(getContext().getClientOutputStream(), new byte[] {0x05,0x00}, 2) != 2) {
				Log.e(TAG, "socks5 verify failed.verify methods wrong");
				return Result.E_S5_VERIFY_SEND_LOCAL;
			}
		}
		
		return forward();
	}

}
