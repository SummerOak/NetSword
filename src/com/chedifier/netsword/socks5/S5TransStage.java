package com.chedifier.netsword.socks5;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.chedifier.netsword.ExceptionHandler;
import com.chedifier.netsword.IOUtils;
import com.chedifier.netsword.Log;
import com.chedifier.netsword.Result;
import com.chedifier.netsword.trans.Courier;
import com.chedifier.netsword.trans.Parcel;

public class S5TransStage extends AbsS5Stage{
	private final String TAG = "S5TransStage";
	
	
	private Courier mCourier;

	public S5TransStage(AbsS5Stage stage) {
		super(stage);
		
		mCourier = new Courier();
	}

	@Override
	public Result forward() {
		return Result.SUCCESS;
	}

	@Override
	public Result handle() {
		Log.r(TAG, ">>>>>>");
		
		if(!isLocal()) {
			new Thread(new Transporter(Transporter.TO_PARCEL,getContext().getServerInputStream(), getContext().getClientOutputStream())).start();
			new Transporter(Transporter.FROM_PARCEL,getContext().getClientInputStream(), getContext().getServerOutputStream()).run();
		}else {
			new Thread(new Transporter(Transporter.FROM_PARCEL,getContext().getServerInputStream(), getContext().getClientOutputStream())).start();
			new Transporter(Transporter.TO_PARCEL,getContext().getClientInputStream(), getContext().getServerOutputStream()).run();
		}
		return Result.SUCCESS;
	}
	
	private class Transporter implements Runnable{
		final int L = 2048;
		byte[] data = new byte[L];
		byte type;
		static final byte FROM_PARCEL = 0x01;
		static final byte TO_PARCEL = 0x02;
		
		private DataInputStream from;
		private OutputStream to;
		
		public Transporter(byte type,DataInputStream from,OutputStream to) {
			this.from = from;
			this.to = to;
			
			this.type = type;
		}
		
		@Override
		public void run() {
			try {
				
				while(true) {
					
					if((type & FROM_PARCEL) > 0) {
						Parcel parcel = mCourier.readParcel(from);
						if(parcel == null) {
							break;
						}
						
						if((type & TO_PARCEL) > 0) {
							if(!mCourier.writeParcel(parcel, to)) {
								break;
							}
						}else {
							if(IOUtils.write(to, parcel.getData(), parcel.size()) != parcel.size()) {
								Log.e(TAG, "send data failed");
								break;
							}
						}
					}else {
						int read = 0;
						if((read = from.read(data,0,L)) <= 0) {
							Log.e(TAG, "read 0");
							break;
						}
						
						if((type & TO_PARCEL) > 0) {
							Parcel p = new Parcel();
							p.append(data,0,read);
							if(!mCourier.writeParcel(p, to)) {
								Log.e(TAG, "send parcel failed");
								break;
							}
							
						}else {
							if(IOUtils.write(to, data, read) != read) {
								Log.e(TAG, "send data failed");
								break;
							}
						}
					}
				}
			} catch (IOException e) {
				ExceptionHandler.handleException(e);
			}
			
		}
	}
}
