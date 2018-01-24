package com.chedifier.netsword;

public enum Result {
	
	SUCCESS(0,"success"),
	E_LOCAL_SOCKET_BUILD_FAILED(1,"local socket build failed"),
	
	
	
	E_S5_VERIFY_READ_HEAD(4,"stage verify, read data format err"),
	E_S5_VERIFY_VER(3,"stage verify, wrong socks version"),
	E_S5_VERIFY_METHOD_LEN_READ(4,"read data format err"),
	E_S5_VERIFY_SEND_PROXY(4,"read data format err"),
	E_S5_VERIFY_READ_PROXY(4,"read data format err"),
	E_S5_VERIFY_SEND_LOCAL(4,"read data format err"),
	
	E_S5_CONN_READ_HEAD(4,"stage verify, read data format err"),
	E_S5_CONN_VER(3,"stage verify, wrong socks version"),
	E_S5_CONN_READ_IPV4(4,"read data format err"),
	E_S5_CONN_READ_DOMAIN(4,"read data format err"),
	E_S5_CONN_READ_IPV6(4,"read data format err"),
	E_S5_CONN_READ_PORT(4,"read data format err"),
	E_S5_CONN_SEND_SERVER(4,"read data format err"),
	
	E_S5_CONN_INVALIDATE_HEAD(4,"stage verify, read data format err"),
	E_S5_CONN_WIRTE_HEAD(4,"stage verify, read data format err"),
	E_S5_CONN_WRITE_IPV4(4,"read data format err"),
	E_S5_CONN_WRITE_DOMAIN(4,"read data format err"),
	E_S5_CONN_WRITE_IPV6(4,"read data format err"),
	E_S5_CONN_WRITE_PORT(4,"read data format err"),
	E_S5_CONN_WRITE_LOCAL(4,"read data format err"),
	
	
	E_LOCAL_SOCKET_ALREADY_LISTENING(2000,"local socket already listening");
	
	
	
	
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
