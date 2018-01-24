package com.chedifier.netsword.server;

public class SServerTest {
	
	public static void main(String[] args){
		
		SServer s = new SServer(8888);
		s.start();
	}
}
