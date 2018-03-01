package com.chedifier.netsword.socks5.version;

import com.chedifier.netsword.socks5.version.Parcel.Parcelable;
import com.chedifier.netsword.iface.Error;

public class InitFeedback {
	
	public static class V1 implements Parcelable{
		public Error error;
		public String extra;
		
		@Override
		public Parcel parcel() {
			
			Parcel parcel = Parcel.createEmptyParcel();
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
			int type = parcel.readInt(0);
			this.error = Error.valueOf(type);
			this.extra = parcel.readString("");
			int sign = parcel.readInt(1);
			if(sign == this.hashCode()) {
				return this;
			}
			
			return null;
		}
		
		@Override
		public int hashCode() {
			return (error==null?0:error.getType()) + (extra==null?0:extra.hashCode());
		}
		
	}

}
