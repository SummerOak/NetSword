package com.chedifier.netsword;

public enum Result {
	
	SUCCESS(0,"success"),
	E_LOCAL_SOCKET_BUILD_FAILED(1,"local socket build failed"),
	
	
	E_S5_VERIFY_FAILED(4,"verify failed."),
	E_S5_CONN_BIND_REMOTE(4,"bind remote failed."),
	E_S5_CONN_BUILD_CONN_INFO_FAILED(4,"build conn info failed."),
	
	E_S5_SOCKET_ERROR_VERIFY(1,"socket error while verifing"),
	E_S5_SOCKET_ERROR_CONN(2,"socket error while connectting"),
	E_S5_SOCKET_ERROR_TRANS(3,"socket error while transportinging");
	
	
	private int type;
	private String msg;
	private Result(int type,String msg) {
		this.type = type;
		this.msg = msg;
	}
	
	public String getMessage() {
		return this.msg;
	}
	
}
