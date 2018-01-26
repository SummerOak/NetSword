package com.chedifier.netsword.socks5;

import com.chedifier.netsword.IOUtils;
import com.chedifier.netsword.Log;
import com.chedifier.netsword.Result;
import com.chedifier.netsword.StringUtils;
import com.chedifier.netsword.trans.Courier;
import com.chedifier.netsword.trans.Parcel;

public class S5VerifyStage extends AbsS5Stage{

	private static final String TAG = "S5VerifyStage";
	
	private Courier mCourier;

	public S5VerifyStage(SocketContext context,boolean isLocal) {
		super(context,isLocal);
		
		mCourier = new Courier();
	}

	@Override
	public Result forward() {
		return new S5ConnStage(this).handle();
	}

	@Override
	public Result handle() {
		Result result = null;
		if(isLocal()) {
			if((result = handleLocal()) != Result.SUCCESS) {
				return result;
			}
		}else if((result = handleServer()) != Result.SUCCESS){
			return result;
		}
		
		return forward();
	}
	
	private Result handleServer() {
		
		Parcel parcel = mCourier.readParcel(getContext().getClientInputStream());
		if(parcel == null) {
			Log.e(TAG, "socks5 verify failed.");
			return Result.E_S5_VERIFY_READ_HEAD;
		}
		
		byte[] cData = parcel.getData();
		Log.i(TAG, "recv client greeting: " + StringUtils.toRawString(cData, 0, 2));

		if(cData[0] != 0x05) {
			Log.e(TAG, "socks5 verify failed.it is not socks5 protocol");
			return Result.E_S5_VERIFY_VER;
		}
		
		//feedback to local
		Parcel p = new Parcel();
		p.append(new byte[] {0x05,0x00});
		if(!mCourier.writeParcel(p, getContext().getClientOutputStream())) {
			Log.e(TAG, "socks5 verify failed in feedback to local.");
			return Result.E_S5_VERIFY_SEND_LOCAL;
		}
		
		return Result.SUCCESS;
	}
	
	private Result handleLocal() {
		Log.r(TAG, "handleLocal >>>>>>");
		final int L = 1024;
		byte[] cData = new byte[L];
		
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
		
		//3. relay to proxy server
		int verifyLength = 2+cData[1];
		Parcel parcel = new Parcel();
		parcel.append(cData,0,verifyLength);
		if(!mCourier.writeParcel(parcel,getContext().getServerOutputStream())) {
			Log.e(TAG, "socks5 verify failed.verify methods wrong");
			return Result.E_S5_VERIFY_SEND_PROXY;
		}
		
		//4. read feedback from server
		parcel = mCourier.readParcel(getContext().getServerInputStream());
		if(parcel == null) {
			Log.e(TAG, "socks5 verify failed. proxy server return false data.");
			return Result.E_S5_VERIFY_READ_PROXY;
		}
		
		Log.i(TAG, "recv from server: " + parcel);
		
		//5. relay feedback from server to client
		if(IOUtils.write(getContext().getClientOutputStream(),parcel.getData(), 2) != 2) {
			Log.e(TAG, "socks5 verify failed. write proxy server return false data.");
			return Result.E_S5_VERIFY_SEND_LOCAL;
		}
		return Result.SUCCESS;
	}

}
