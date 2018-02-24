/**
 * 
 */
package com.chedifier.netsword.iface;

public interface IProxyListener {
	
	/**
	 * 
	 * params: boolean isLocal,int localPort,String serverAddr,int port
	 * 
	 */
	public static final int PROXY_START 		= 1;
	
	/**
	 * params:boolean isLocal
	 */
	public static final int PROXY_STOP 		= 2;
	
	/**
	 * params:int id,String ip
	 */
	public static final int RECV_CONN 		= 3;
	
	/**
	 * params:int id,int newState
	 */
	public static final int STATE_UPDATE 	= 4;
	
	/**
	 * params: int id,Result error
	 */
	public static final int ERROR 			= 5;
	
	/**
	 * params:int id,byte type,int speed
	 */
	public static final int SPEED 			= 6;
	
	/**
	 * params:int id,long srcIn
	 */
	public static final int SRC_IN 			= 7;
	
	/**
	 * params:int id,long srcOut
	 */
	public static final int SRC_OUT 			= 8;
	
	/**
	 * params:int id,long destIn
	 */
	public static final int DEST_IN			= 9;
	
	/**
	 * params:int id,long destOut
	 */
	public static final int DEST_OUT 		= 10;
	
	/**
	 * params: int id,String ip,String domain,int port
	 */
	public static final int CONN_INFO		= 11;
	
	/**
	 * params: int id,int ops
	 */
	public static final int SRC_INTRS_OPS	= 12;
	
	/**
	 * params: int id,int ops
	 */
	public static final int DEST_INTRS_OPS	= 13;
	
	/**
	 * params: int aliveNum
	 */
	public static final int ALIVE_NUM		= 14;

	Object onMessage(int msgId,Object... params);
	
	
	public static final class SPEEDTYPE{
		public static final byte SRC_IN 		= 1;
		public static final byte SRC_OUT 	= 2;
		public static final byte DEST_IN 	= 3;
		public static final byte DEST_OUT 	= 4;
	}
}
