package com.chedifier.netsword.socks5.version;

import com.chedifier.netsword.socks5.version.Parcel.Parcelable;
import com.chedifier.netsword.iface.Error;

public class InitFeedback {
	
	public static class V1 implements Parcelable{
		public int DATA_TYPE = Parcel.DATA_TYPE.INIT_FEEDBACK;
		
		public Error error;
		public String extra;
		
		@Override
		public Parcel parcel() {
			
			Parcel parcel = Parcel.createEmptyParcel();
			parcel.writeInt(DATA_TYPE);
			parcel.writeInt(error==null?0:error.getType());
			parcel.writeString(extra);
			parcel.writeInt(hashCode());
			
			return parcel;
		}

		@Override
		public Parcelable parcel(Parcel parcel) {
			if(parcel == null) {
				return null;
			}
			
			parcel.flip();
			this.DATA_TYPE = parcel.readInt(-1);
			if(DATA_TYPE == Parcel.DATA_TYPE.INIT_FEEDBACK) {
				int type = parcel.readInt(0);
				this.error = Error.valueOf(type);
				this.extra = parcel.readString("");
				int sign = parcel.readInt(1);
				if(sign == this.hashCode()) {
					return this;
				}
			}
			
			return null;
		}
		
		@Override
		public int hashCode() {
			return (error==null?0:error.getType()) + (extra==null?0:extra.hashCode());
		}
		
	}

}
