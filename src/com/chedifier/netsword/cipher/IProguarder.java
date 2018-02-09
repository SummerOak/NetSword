package com.chedifier.netsword.cipher;

public interface IProguarder {
	byte[] encode(byte[] origin);
	byte[] encode(byte[] origin,int offset,int len);
	byte[] decode(byte[] encode);
	byte[] decode(byte[] encode,int offset,int len);
}
