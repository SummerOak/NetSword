package com.chedifier.netsword.socks5;

import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.NetUtils;
import com.chedifier.netsword.iface.Error;
import com.chedifier.netsword.socks5.SSockChannel.IChannelEvent;

public abstract class AbsS5Stage implements IChannelEvent{

	protected SSockChannel mChannel;
	private ICallback mCallback;
	private boolean mIsLocal;
	private int mConnId;
	
	public AbsS5Stage(SSockChannel context,boolean isLocal,ICallback callback) {
		mChannel = context;
		mIsLocal = isLocal;
		mCallback = callback;
		mChannel.setListener(this);
	}
	
	public AbsS5Stage(AbsS5Stage stage) {
		this.mChannel = stage.mChannel;
		this.mChannel.setListener(this);
		this.mCallback = stage.mCallback;
		this.mIsLocal = stage.mIsLocal;
		this.mConnId = stage.mConnId;
	}
	
	protected final String getTag() {
		return getClass().getName() + "_c"+mConnId;
	}
	
	public void setConnId(int id) {
		mConnId = id;
	}
	
	public int getConnId() {
		return mConnId;
	}
	
	protected boolean isLocal() {
		return mIsLocal;
	}
	
	public void start() {
		
	}
	
	@Override
	public void onRelayFailed(Error result) {
		
	}
	
	@Override
	public void onSrcOpsUpdate(int ops) {
		Log.t(getTag(), "onSrcOpsUpdate " + ops + ": " + NetUtils.getOpsDest(ops));
		if(mCallback != null) {
			mCallback.onSrcOpsUpdate(ops);
		}
	}
	
	@Override
	public void onDestOpsUpdate(int ops) {
		Log.t(getTag(), "onDestOpsUpdate " + ops + ": " + NetUtils.getOpsDest(ops));
		if(mCallback != null) {
			mCallback.onDestOpsUpdate(ops);
		}
	}
	
	protected void forward() {
		AbsS5Stage next = next();
		if(next != null) {
			next.start();
		}
	}
	
	public abstract AbsS5Stage next();

	protected SSockChannel getChannel() {
		return mChannel;
	}
	
	protected ICallback getCallback() {
		return mCallback;
	}
	
	protected void notifyState(int newState,Object... params) {
		if(mCallback != null) {
			mCallback.onStateChange(newState, params);
		}
	}

	protected void notifyError(Error result) {
		if(mCallback != null) {
			mCallback.onError(result);
		}
	}
	
	public static interface ICallback{
		void onStateChange(int newState,Object... params);
		void onError(Error result);
		void onConnInfo(String ip,String domain,int port);
		void onSrcOpsUpdate(int ops);
		void onDestOpsUpdate(int ops);
		
	}
}
