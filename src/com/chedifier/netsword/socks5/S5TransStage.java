package com.chedifier.netsword.socks5;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.chedifier.netsword.ExceptionHandler;
import com.chedifier.netsword.Log;
import com.chedifier.netsword.Result;
import com.chedifier.netsword.StringUtils;

public class S5TransStage extends AbsS5Stage{
	private final String TAG = "S5TransStage";

	public S5TransStage(AbsS5Stage stage) {
		super(stage);
	}

	@Override
	public Result forward() {
		return Result.SUCCESS;
	}

	@Override
	public Result handle() {
		Log.r(TAG, ">>>>>>");
		
		if(!isLocal()) {
			new Thread(new Transporter(getContext().getServerInputStream(), getContext().getClientOutputStream())).start();
		}
		new Transporter(getContext().getClientInputStream(), getContext().getServerOutputStream()).run();
		return Result.SUCCESS;
	}
	
	private class Transporter implements Runnable{
		final int L = 2048;
		byte[] data = new byte[L];
		
		private DataInputStream from;
		private OutputStream to;
		
		public Transporter(DataInputStream from,OutputStream to) {
			this.from = from;
			this.to = to;
		}
		
		@Override
		public void run() {
			try {
				int read = 0;
				while((read = from.read(data,0,L)) > 0) {
					Log.d(TAG, "trans[" + read + "]: " + StringUtils.toString(data, read));
					to.write(data,0,read);
				}
			} catch (IOException e) {
				ExceptionHandler.handleException(e);
			}
			
		}
	}
}
