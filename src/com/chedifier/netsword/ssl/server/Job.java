package com.chedifier.netsword.ssl.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Properties;

import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.SocketIO;
import com.chedifier.netsword.ssl.Configuration;

public class Job implements Runnable {

	private static final String TAG = "Job";

	private Socket socket;

	public Job(Socket socket) {
		this.socket = socket;
	}

	public void run() {
		Properties p = Configuration.getConfig();
		String encoding = p.getProperty("socketStreamEncoding");

		DataInputStream input = null;
		DataOutputStream output = null;
		try {
			input = SocketIO.getDataInput(socket);

			int length = input.read();
			byte[] bytes = new byte[length];
			int read = 0;
			int rt = 0;
			while ((rt = input.read(bytes, rt, length - read)) > 0 && read < length) {
				read += rt;
			}

			int pwd = input.read();
			String user = new String(bytes, encoding).trim();
			String result = "login failed";
			if (read == length) {
				if (null != user && !user.equals("") && user.equals("name") && pwd == 123) {
					result = "login success";
				}
			}

			Log.i(TAG, "request user:" + user);
			Log.i(TAG, "request pwd:" + pwd);

			output = SocketIO.getDataOutput(socket);

			bytes = result.getBytes(encoding);
			length = (short) bytes.length;
			output.writeShort(length);
			output.write(bytes);

			Log.i(TAG, "response info:" + result);
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "business thread run exception");
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
				Log.i(TAG, "server socket close error");
			}
		}
	}
}
