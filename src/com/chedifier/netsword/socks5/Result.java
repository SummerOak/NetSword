package com.chedifier.netsword.socks5;

public enum Result {
	
	SUCCESS(0,"success"),
	E_LOCAL_SOCKET_BUILD_FAILED(1,"local socket build failed"),
	E_S5_VERIFY_FAILED(2,"verify failed."),
	E_S5_CONN_BIND_REMOTE(3,"bind remote failed."),
	E_S5_CONN_BUILD_CONN_INFO_FAILED(4,"build conn info failed."),
	E_S5_SOCKET_ERROR_VERIFY(5,"socket error while verifing"),
	E_S5_SOCKET_ERROR_CONN(6,"socket error while connectting"),
	E_S5_SOCKET_ERROR_TRANS(7,"socket error while transportinging");
	
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