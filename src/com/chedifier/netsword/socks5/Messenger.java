package com.chedifier.netsword.socks5;

import com.chedifier.netsword.base.JobScheduler;
import com.chedifier.netsword.base.JobScheduler.Job;
import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.StringUtils;
import com.chedifier.netsword.iface.IProxyListener;

public class Messenger {
	
	private static final String TAG = "Messenger";
	
	public static void notifyMessage(final IProxyListener l,final int msgId,final Object... params) {
		Log.d(TAG, "notifyMessage: " + msgId + StringUtils.toString(params));
		if(l != null) {
			JobScheduler.schedule(new Job("Messenger#msg"+msgId) {
				@Override
				public void run() {
					Log.d(TAG, "notifyMessage-run: " + msgId + StringUtils.toString(params));
					l.onMessage(msgId, params);
				}
			});
		}
	}
}
