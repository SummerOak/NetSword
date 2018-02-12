package com.chedifier.netsword.socks5;

import java.nio.channels.SelectionKey;

import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.iface.Result;
import com.chedifier.netsword.iface.SProxyIface;

public class S5TransStage extends AbsS5Stage{
	
	
	public S5TransStage(AbsS5Stage stage) {
		super(stage);
		
	}
	
	@Override
	public void start() {
		Log.r(getTag(), "S5TransStage start>>>");
		super.start();
		notifyState(SProxyIface.STATE.TRANS);
	}

	@Override
	public AbsS5Stage next() {
		return null;
	}

	@Override
	public void onSourceOpts(int opts) {
		if((opts&SelectionKey.OP_READ) > 0) {
			getChannel().relay(true, isLocal());
		}
	}
	
	@Override
	public void onDestOpts(int opts) {
		if((opts&SelectionKey.OP_READ) > 0) {
			getChannel().relay(false, !isLocal());
		}
		
	}

	@Override
	public void onSocketBroken(Result result) {
		notifyError(result);
		notifyError(Result.E_S5_SOCKET_ERROR_TRANS);
	}

}
