package com.chedifier.netsword.ssl;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;

import com.chedifier.netsword.Log;

public class MyHandshakeCompletedListener implements HandshakeCompletedListener {

	private static final String TAG = "MyHandshakeCompletedListener";

	public void handshakeCompleted(HandshakeCompletedEvent arg0) {
		Log.i(TAG, "Handshake finished successfully");
	}
}