package com.chedifier.netsword.socks5;

import com.chedifier.netsword.Result;

public abstract class AbsS5Stage {

	protected SocketContext mContext;
	private boolean mIsLocal;
	
	public AbsS5Stage(SocketContext context,boolean isClient) {
		mContext = context;
		mIsLocal = isClient;
		
	}
	
	public AbsS5Stage(AbsS5Stage stage) {
		this.mContext = stage.mContext;
		this.mIsLocal = stage.mIsLocal;
	}
	
	protected SocketContext getContext() {
		return mContext;
	}
	
	public boolean isLocal() {
		return mIsLocal;
	}
	
	public abstract Result forward();

	public abstract Result handle();
}
