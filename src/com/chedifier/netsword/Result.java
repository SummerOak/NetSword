package com.chedifier.netsword;

public enum Result {
	
	SUCCESS(0,"success"),
	E_LOCAL_SOCKET_BUILD_FAILED(1,"local socket build failed"),
	E_LOCAL_SOCKET_ALREADY_LISTENING(2,"local socket already listening");
	
	
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
