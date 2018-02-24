package com.chedifier.netsword.socks5;

import java.nio.channels.SelectionKey;

import com.chedifier.netsword.base.ObjectPool;
import com.chedifier.netsword.base.ObjectPool.IConstructor;
import com.chedifier.netsword.iface.Error;

public class AcceptorWrapper {
	
	private static ObjectPool<AcceptorW> sWAcceptorPool;
	private static boolean sInited = false;
	
	public static synchronized void init() {
		if(sInited) {
			return;
		}
		sWAcceptorPool = new ObjectPool<AcceptorW>(new IConstructor<AcceptorW>() {
			
			@Override
			public AcceptorW newInstance(Object... params) {
				return new AcceptorW((IAcceptor)params[0]);
			}

			@Override
			public void initialize(AcceptorW e, Object... params) {
				e.mA = (IAcceptor)params[0];
			}
			
		}, 50);
		sInited = true;
	}
	
	public static IAcceptor wrapper(IAcceptor a) {
		return sWAcceptorPool.obtain(a);
	}
	
	private static class AcceptorW implements IAcceptor{
		private IAcceptor mA;
		
		private AcceptorW(IAcceptor acceptor) {
			this.mA = acceptor;
		}
		
		public Error accept(SelectionKey selKey,int opt) {
			Error res = mA.accept(selKey,opt);
			sWAcceptorPool.release(this);
			return res;
		}
	}
	
	public static interface IAcceptor {
		public Error accept(SelectionKey selKey,int opts);
	}
	
}
