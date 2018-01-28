package com.chedifier.netsword.socks5;

import com.chedifier.netsword.Result;

public abstract class AbsS5Stage {

	protected SocketContext mContext;
	private boolean mIsLocal;
	private ICallback mCallback;
	
	public AbsS5Stage(SocketContext context,boolean isLocal,ICallback callback) {
		mContext = context;
		mIsLocal = isLocal;
		mCallback = callback;
	}
	
	public AbsS5Stage(AbsS5Stage stage) {
		this.mContext = stage.mContext;
		this.mIsLocal = stage.mIsLocal;
		this.mCallback = stage.mCallback;
	}
	
	protected SocketContext getContext() {
		return mContext;
	}
	
	public boolean isLocal() {
		return mIsLocal;
	}
	
	public abstract Result forward();

	public abstract Result handle();
	
	protected void sendResultBack(Result result) {
		if(mCallback != null) {
			mCallback.onResult(result);
		}
	}
	
	
	public static interface ICallback{
		void onResult(Result result);
	}
}
