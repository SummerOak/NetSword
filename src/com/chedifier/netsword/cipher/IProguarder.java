package com.chedifier.netsword.cipher;

import java.nio.ByteBuffer;

public interface IProguarder {
	boolean encode(byte[] origin,ByteBuffer outBuffer);
	boolean encode(byte[] origin,int offset,int len,ByteBuffer outBuffer);
	boolean decode(byte[] encode,ByteBuffer outBuffer);
	boolean decode(byte[] encode,int offset,int len,ByteBuffer outBuffer);
	int estimateEncodeLen(int len);
	int estimateDecodeLen(int len);
}
