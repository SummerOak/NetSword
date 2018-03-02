package com.chedifier.netsword.socks5.version;

import com.chedifier.netsword.socks5.version.Parcel.Parcelable;

public class InitInfo{

	
	public static class V1 implements Parcelable{
		public int DATA_TYPE = Parcel.DATA_TYPE.INIT_INFO;
		public int versionCode;
		public String versionName;
		public String userName;
		public String password;
		
		@Override
		public Parcel parcel() {
			Parcel parcel = Parcel.createEmptyParcel();
			parcel.writeInt(DATA_TYPE);
			parcel.writeInt(versionCode);
			parcel.writeString(versionName);
			parcel.writeString(userName);
			parcel.writeString(password);
			int sign = hashCode();
			parcel.writeInt(sign);
			return parcel;
		}
		
		
		@Override
		public Parcelable parcel(Parcel parcel) {
			parcel.flip();
			this.DATA_TYPE = parcel.readInt(-1);
			if(this.DATA_TYPE == Parcel.DATA_TYPE.INIT_INFO) {
				this.versionCode = parcel.readInt(0);
				this.versionName = parcel.readString("");
				this.userName = parcel.readString("");
				this.password = parcel.readString("");
				int s = parcel.readInt(1);
				int sign  = hashCode();
				if(s == sign) {
					return this;
				}
			}
			
			return null;
		}
		
		@Override
		public int hashCode() {
			return versionCode 
					+ (versionName==null?0:versionName.hashCode()) 
					+ (userName==null?0:userName.hashCode())  
					+ (password==null?0:password.hashCode());
		}
	}
}
