package com.chedifier.netsword;

import com.chedifier.ladder.base.Log;
import com.chedifier.ladder.base.StringUtils;
import com.chedifier.ladder.iface.Error;
import com.chedifier.ladder.iface.IProxyListener;
import com.chedifier.ladder.iface.SProxyIface;
import com.chedifier.netsword.swing.ConnsTableModel.COLUMN;
import com.chedifier.netsword.swing.SwordUI;
import com.chedifier.netsword.swing.SwordUI.ISwordUIEvent;

public class Socks5 implements IProxyListener,ISwordUIEvent{

	private static final String TAG = "SOCKS5";

	private SwordUI mSwordUI;
	
	private int mForceServer = 0;
	private boolean mNonUI = false;
	
	private Socks5(String[] args) {
		
		if(!parseArgs(args)) {
			printHelps();
			return;
		}
		
		if(!SProxyIface.init()) {
			return;
		}
		
		if(!mNonUI) {
			mSwordUI = SwordUI.build();
			mSwordUI.setEventListener(this);
			mSwordUI.show();
		}

		SProxyIface.start(this, mForceServer);
	}
	
	public static void main(String[] args) {
		new Socks5(args);
	}
	
	private boolean parseArgs(String[] args) {
		
		if(args != null) {
			for(String s:args) {
				if("s".equals(s)) {
					mForceServer = 1;
				}else if("c".equals(s)) {
					mForceServer = -1;
				}else if("nui".equals(s)) {
					mNonUI = true;
				}else {
					return false;
				}
			}
		}
		
		return true;
	}
	
	private void printHelps() {
		StringBuilder sb = new StringBuilder(125);
		sb.append("tips>>>\n");
		sb.append("input format [c|s|nui]").append("\n");
		sb.append("c		run as client").append("\n");
		sb.append("s		run as server").append("\n");
		sb.append("nui	run without ui").append("\n");
		
		Log.r(TAG, sb.toString());
	}

	@Override
	public Object onMessage(int msgId, Object... params) {
		if(mSwordUI == null) {
			return null;
		}
		
		switch (msgId) {
			case IProxyListener.PROXY_START:{
				boolean isLocal = false;
				if(params.length > 0 && params[0] instanceof Boolean) {
					isLocal = (boolean)params[0];
					mSwordUI.setServer(isLocal);
				}
				
				if(params.length > 1 && params[1] instanceof Integer) {
					mSwordUI.setLocalPort((int)params[1]);
				}
				
				if(params.length > 2 && params[2] instanceof String) {
					mSwordUI.setProxyHost((String)params[2]);
				}
				
				if(params.length > 3 && params[3] instanceof Integer) {
					mSwordUI.setProxyPort((int)params[3]);
				}
				
				
				break;
			}
					
			case IProxyListener.PROXY_STOP:{
				System.exit(0);
				break;
			}
			
			case IProxyListener.ERROR:{
				if(params != null) {
					if(params.length > 0 && params[0] instanceof Integer) {
						int id = (int)params[0];
						if(params.length > 1 && params[1] instanceof Error) {
							Error result = (Error)params[1];
							mSwordUI.updateConn(id, COLUMN.ERR, result.getType() + "(" + result.getMessage() + ")");
						}
					}
				}
				break;
			}
			
			case IProxyListener.RECV_CONN:{
				if(params != null) {
					if(params.length > 0 && params[0] instanceof Integer) {
						int id = (int)params[0];
						mSwordUI.addConn(id);
						if(params.length > 1 && params[1] instanceof String) {
							String client = (String)params[1];
							mSwordUI.updateConn(id, COLUMN.CLIENT, client);
						}
					}
				}
				
				break;
			}
			
			case IProxyListener.STATE_UPDATE:{
				if(params != null) {
					if(params.length > 0 && params[0] instanceof Integer) {
						int id = (int)params[0];
						if(params.length > 1 && params[1] instanceof Integer) {
							int newState = (int)params[1];
							mSwordUI.updateConn(id, COLUMN.STATE, newState);
						}
					}
				}
				
				break;
			}
			
			case IProxyListener.SPEED:{
				
				break;
			}
			
			case IProxyListener.SRC_IN:{
				if(params != null) {
					if(params.length > 0 && params[0] instanceof Integer) {
						int id = (int)params[0];
						if(params.length > 1 && params[1] instanceof Long) {
							long val = (long)params[1];
							mSwordUI.updateConn(id, COLUMN.SRC_IN, val);
						}
					}
				}
				break;
			}
			
			case IProxyListener.SRC_OUT:{
				if(params != null) {
					if(params.length > 0 && params[0] instanceof Integer) {
						int id = (int)params[0];
						if(params.length > 1 && params[1] instanceof Long) {
							long val = (long)params[1];
							mSwordUI.updateConn(id, COLUMN.SRC_OUT, val);
						}
					}
				}
				break;
			}
			
			case IProxyListener.DEST_IN:{
				if(params != null) {
					if(params.length > 0 && params[0] instanceof Integer) {
						int id = (int)params[0];
						if(params.length > 1 && params[1] instanceof Long) {
							long val = (long)params[1];
							mSwordUI.updateConn(id, COLUMN.DEST_IN, val);
						}
					}
				}
				break;
			}
			
			case IProxyListener.DEST_OUT:{
				if(params != null) {
					if(params.length > 0 && params[0] instanceof Integer) {
						int id = (int)params[0];
						if(params.length > 1 && params[1] instanceof Long) {
							long val = (long)params[1];
							mSwordUI.updateConn(id, COLUMN.DEST_OUT, val);
						}
					}
				}
				break;
			}
			
			case IProxyListener.CONN_INFO:{
				if(params != null) {
					if(params.length > 0 && params[0] instanceof Integer) {
						int id = (int)params[0];
						String remote = "";
						if(params.length > 2 && params[2] instanceof String) {
							remote += (String)params[2];
							
						}
						
						if(params.length > 1 && params[1] instanceof String && ((String)params[1]).length() > 0) {
							if(StringUtils.isEmpty(remote)) {
								remote += (String)params[1];
							}else {
								remote += "(" + (String)params[1] + ")";
							}
						}
						
						mSwordUI.updateConn(id, COLUMN.REMOTE, remote);
						
						if(params.length > 3 && params[3] instanceof Integer) {
							int port = (Integer)params[3];
							mSwordUI.updateConn(id, COLUMN.PORT, port);
						}
					}
				}
				break;
			}
			
			case IProxyListener.SRC_INTRS_OPS:{
				if(params != null) {
					if(params.length > 0 && params[0] instanceof Integer) {
						int id = (int)params[0];
						if(params.length > 1 && params[1] instanceof Integer) {
							int ops = (int)params[1];
							Log.d(TAG, "onMessage src ops " + ops);
							mSwordUI.updatePortOps(id, true, ops);
						}
					}
				}
				break;
			}
			
			case IProxyListener.DEST_INTRS_OPS:{
				if(params != null) {
					if(params.length > 0 && params[0] instanceof Integer) {
						int id = (int)params[0];
						if(params.length > 1 && params[1] instanceof Integer) {
							int ops = (int)params[1];
							Log.d(TAG, "onMessage dest ops " + ops);
							mSwordUI.updatePortOps(id, false, ops);
						}
					}
				}
				break;
			}
			
			case IProxyListener.ALIVE_NUM:{
				if(params != null) {
					if(params.length > 0 && params[0] instanceof Long) {
						long aliveNum = (long)params[0];
						mSwordUI.updateAliveConns(aliveNum);
					}
				}
				break;
			}
			
			case IProxyListener.MEMORY_INFO:{
				if(params != null) {
					if(params.length > 1 && params[0] instanceof Long && params[1] instanceof Long) {
						long inUsing = (long)params[0];
						long total = (long)params[1];
						mSwordUI.updateMem(inUsing, total);
					}
				}
				break;
			}
			
			default:break;
		}
		
		return null;
	}

	@Override
	public Object onSwordUIEvent(int eventId, Object... params) {
		switch(eventId) {
			case ISwordUIEvent.WINDOW_CLOSE:{
				SProxyIface.stop("window close");
				break;
			}
		}
		return null;
	}

}
