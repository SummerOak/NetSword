package com.chedifier.netsword.socks5;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.chedifier.netsword.Result;
import com.chedifier.netsword.base.ExceptionHandler;
import com.chedifier.netsword.base.IOUtils;
import com.chedifier.netsword.base.JobScheduler;
import com.chedifier.netsword.base.JobScheduler.Job;
import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.StringUtils;
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
			JobScheduler.schedule(new Transporter(Transporter.TO_PARCEL,getContext().getServerInputStream(), 
					getContext().getClientOutputStream(),"remote2proxy"));
			JobScheduler.schedule(new Transporter(Transporter.FROM_PARCEL,getContext().getClientInputStream(), 
					getContext().getServerOutputStream(),"proxy2remote"));
		}else {
			JobScheduler.schedule(new Transporter(Transporter.FROM_PARCEL,getContext().getServerInputStream(), 
					getContext().getClientOutputStream(),"local2client"));
			JobScheduler.schedule(new Transporter(Transporter.TO_PARCEL,getContext().getClientInputStream(), 
					getContext().getServerOutputStream(),"client2local"));
		}
		return Result.SUCCESS;
	}
	
	private class Transporter extends Job{
		private final String TAG;
		final int L = 2048;
		byte[] data = new byte[L];
		byte type;
		static final byte FROM_PARCEL = 0x01;
		static final byte TO_PARCEL = 0x02;
		
		private DataInputStream from;
		private OutputStream to;
		
		public Transporter(byte type,DataInputStream from,OutputStream to,String tag) {
			super(tag);
			this.from = from;
			this.to = to;
			
			this.type = type;
			this.TAG = tag;
		}
		
		@Override
		public void run() {
			long token = System.nanoTime();
			long cost = System.currentTimeMillis();
			Log.t(TAG, "begin trans >>>");
			try {
				
				while(true) {
					
					if((type & FROM_PARCEL) > 0) {
						Log.t(TAG, token + " read parcel from...");
						Parcel parcel = mCourier.readParcel(from);
						if(parcel == null) {
							Log.e(S5TransStage.this.TAG+"#"+TAG, "read parcel failed.");
							break;
						}
						
						Log.t(TAG, token + " recv parcel " + parcel.size());
						
						Log.d(TAG, "read trans parcel: " + StringUtils.toRawString(parcel.getData() ,parcel.size()));
						
						if((type & TO_PARCEL) > 0) {
							Log.t(TAG, token + " write parcel to...");
							if(!mCourier.writeParcel(parcel, to)) {
								Log.e(S5TransStage.this.TAG+"#"+TAG, "send parcel failed.");
								break;
							}
						}else {
							Log.t(TAG, token + " write stream to...");
							if(IOUtils.write(to, parcel.getData(), parcel.size()) != parcel.size()) {
								Log.e(S5TransStage.this.TAG+"#"+TAG, "send data failed");
								break;
							}
						}
					}else {
						int read = 0;
						Log.t(TAG, token + " read stream from...");
						if((read = from.read(data,0,L)) <= 0) {
							Log.e(S5TransStage.this.TAG+"#"+TAG, "read err " + read);
							break;
						}
						
						Log.t(TAG, token + " recv stream " + read);
						
						Log.d(TAG, "read trans stream succ: " + StringUtils.toRawString(data, read));
						
						if((type & TO_PARCEL) > 0) {
							Parcel p = new Parcel();
							p.append(data,0,read);
							Log.t(TAG, token + " write parcel to...");
							if(!mCourier.writeParcel(p, to)) {
								Log.e(S5TransStage.this.TAG+"#"+TAG, "send parcel failed");
								break;
							}
							Log.t(TAG, token + " sended parcel");
							
						}else {
							Log.t(TAG, token + " write stream to...");
							if(IOUtils.write(to, data, read) != read) {
								Log.e(S5TransStage.this.TAG+"#"+TAG, "send data failed");
								break;
							}
							Log.t(TAG, token + " sended stream");
						}
					}
				}
			} catch (IOException e) {
				ExceptionHandler.handleException(e);
			}
			
			sendResultBack(Result.E_S5_TRANS_END);
			
			Log.t(TAG, token + " end trans <<< " + (System.currentTimeMillis() - cost));
		}
	}
}
