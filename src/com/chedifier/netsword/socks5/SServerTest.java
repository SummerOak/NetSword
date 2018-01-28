package com.chedifier.netsword.socks5;

public class SServerTest {
	
	public static void main(String[] args){
		
		SProxy s = new SProxy(8888,false);
		s.start();
	}
}
