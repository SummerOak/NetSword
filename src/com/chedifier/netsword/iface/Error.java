package com.chedifier.netsword.iface;

public enum Error {
	
	SUCCESS(0,"success"),
	E_LOCAL_SOCKET_BUILD_FAILED(1,"local socket build failed"),
	E_S5_VERIFY_FAILED(2,"verify failed."),
	E_S5_CONN_BIND_REMOTE(3,"bind remote failed."),
	E_S5_CONN_BUILD_CONN_INFO_FAILED(4,"build conn info failed."),
	E_S5_SOCKET_ERROR_VERIFY(5,"socket error while verifing"),
	E_S5_SOCKET_ERROR_CONN(6,"socket error while connectting"),
	E_S5_SOCKET_ERROR_TRANS(7,"socket error while transportinging"),
	E_S5_BIND_PROXY_FAILED(8,"bind proxy failed."),
	E_S5_SOCKET_READ_FAILED(9,"sc read failed"),
	E_S5_SOCKET_WRITE_FAILED(10,"sc write failed"),
	E_S5_SOCKETCHANNEL_TIMEOUT(11,"sc time out"),
	E_S5_RELAY_ENCRYPT_FAILED(12,"encrypt failed"),
	E_S5_RELAY_DECRYPT_FAILED(13,"decrypt failed"),
	E_S5_OUT_BUFFER_FULL_FILLED(14,"out buffer full filled"),
	E_S5_CHANNEL_DEAD(15,"channel dead");
	
	private int type;
	private String msg;
	private Error(int type,String msg) {
		this.type = type;
		this.msg = msg;
	}
	
	public String getMessage() {
		return this.msg;
	}
	
}
