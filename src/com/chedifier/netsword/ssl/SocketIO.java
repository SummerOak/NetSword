package com.chedifier.netsword.ssl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import com.chedifier.netsword.ExceptionHandler;

public class SocketIO {
	public static DataInputStream getDataInput(Socket socket) {
		DataInputStream input;
		try {
			input = new DataInputStream(socket.getInputStream());
		} catch (Throwable e) {
			ExceptionHandler.handleException(e);
			return null;
		}
		return input;
	}

	public static DataOutputStream getDataOutput(Socket socket) {
		DataOutputStream out = null;
		try {
			out = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			ExceptionHandler.handleException(e);
			return null;
		}
		return out;
	}

}
