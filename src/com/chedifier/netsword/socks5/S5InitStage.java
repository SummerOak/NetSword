package com.chedifier.netsword.socks5;

import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.iface.Result;
import com.chedifier.netsword.iface.SProxyIface;

public class S5InitStage extends AbsS5Stage{

	public S5InitStage(SSockChannel channel,boolean isLocal,ICallback callback) {
		super(channel,isLocal,callback);
	}

	@Override
	public void start() {
		Log.r(getTag(), "S5InitStage start>>>");
		super.start();
		
		notifyState(SProxyIface.STATE.INIT);
		
		forward();
	}

	@Override
	public AbsS5Stage next() {
		return new S5VerifyStage(this);
	}
	
	@Override
	public void onSourceOpts(int opts) {
		
	}

	@Override
	public void onDestOpts(int opts) {
		
	}

	@Override
	public void onSocketBroken(Result result) {
		
	}

}
