package com.chedifier.netsword.socks5;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import com.chedifier.netsword.base.ExceptionHandler;
import com.chedifier.netsword.base.IOUtils;
import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.ObjectPool;
import com.chedifier.netsword.base.ObjectPool.IConstructor;
import com.chedifier.netsword.iface.IProxyListener;
import com.chedifier.netsword.iface.Result;
import com.chedifier.netsword.iface.SProxyIface;
import com.chedifier.netsword.metrics.ISpeedListener;
import com.chedifier.netsword.metrics.SpeedMetrics;
import com.chedifier.netsword.socks5.AbsS5Stage.ICallback;
import com.chedifier.netsword.socks5.AcceptorWrapper.IAcceptor;
import com.chedifier.netsword.socks5.SSockChannel.ITrafficEvent;

public class SProxy implements IAcceptor{

	private final String TAG;

	private static int sConnectionId;
	private final int mPort;
	private Selector mSelector;
	private ServerSocketChannel mSocketChannel = null;
	private boolean mIsLocal;
	
	private int mChannelBufferSize;
	private boolean mWorking = false;
	private String mProxyHost = null;
	private int mProxyPort;
	private InetSocketAddress mProxyAddress;
	
	private ObjectPool<Relayer> mRelayerPool;
	private volatile long mConnections = 0;
	private IProxyListener mListener;
	
	
	public static SProxy createLocal(int port,String serverHost,int serverPort) {
		return new SProxy(port,true,serverHost,serverPort,null);
	}
	
	public static SProxy createServer(int port) {
		return new SProxy(port,false,"",0,null);
	}
	
	public static SProxy createLocal(int port,String serverHost,int serverPort,IProxyListener l) {
		return new SProxy(port,true,serverHost,serverPort,l);
	}
	
	public static SProxy createServer(int port,IProxyListener l) {
		return new SProxy(port,false,"",0,l);
	}

	private SProxy(int port,boolean isLocal,String serverHost,int serverPort,IProxyListener l) {
		TAG = "SProxy." + (isLocal?"local":"server");
		mPort = port;
		mIsLocal = isLocal;
		mListener = l;
		
		mChannelBufferSize = Configuration.getConfigInt(Configuration.BUFFER_SIZE, Configuration.DEFAULT_BUFFERSIZE);
		if (mChannelBufferSize <= 0 || mChannelBufferSize > (Configuration.DEFAULT_BUFFERSIZE << 1)) {
			Log.e(TAG, "buffer size is too small or big, adjust to default.");
			mChannelBufferSize = Configuration.DEFAULT_BUFFERSIZE;
		}

		Log.d(TAG, "buffer size is " + mChannelBufferSize);
		
		init();
		
		
		if(isLocal) {
			mProxyHost = serverHost;
			mProxyPort = serverPort;
			mProxyAddress = new InetSocketAddress(serverHost,serverPort);
		}
		
		mRelayerPool = new ObjectPool<Relayer>(new IConstructor<Relayer>() {

			@Override
			public Relayer newInstance(Object... params) {
				return new Relayer((SocketChannel)params[0]);
			}

			@Override
			public void initialize(Relayer e, Object... params) {
				e.init((SocketChannel)params[0]);
			}
		}, 20);
		
		
	}
	
	private void init() {
		AcceptorWrapper.init();
		
	}
	
	private synchronized int generateConnectionId() {
		return sConnectionId = (++sConnectionId < 0? 0:sConnectionId);
	}

	public void start() {
		try {
			mSelector = Selector.open();
			mSocketChannel = ServerSocketChannel.open();
			mSocketChannel.configureBlocking(false);
			InetSocketAddress addr = new InetSocketAddress(mPort);
			mSocketChannel.bind(addr);
			SelectionKey selKey = mSocketChannel.register(mSelector, SelectionKey.OP_ACCEPT);
			selKey.attach(AcceptorWrapper.wrapper(this));
		}catch (Throwable t) {
			Log.e(TAG, "start failed." + t.getMessage());
			ExceptionHandler.handleException(t);
			IOUtils.safeClose(mSocketChannel);
			Messenger.notifyMessage(mListener, IProxyListener.ERROR, 0,Result.E_LOCAL_SOCKET_BUILD_FAILED);
			return;
		}
		
		mWorking = true;
		Messenger.notifyMessage(mListener, IProxyListener.PROXY_START, mIsLocal,mPort, mProxyHost,mProxyPort);
		
		Log.r(TAG, "start success >>> listening " + mPort);
		while(mWorking) {
			int sel = 0;
			try {
				Log.d(TAG, "select next ops...");
				if((sel = mSelector.select()) == 0) {
					Log.d(TAG, "nothing to do,go next... " + mSelector.selectedKeys().size());
					continue;
				}
				Log.d(TAG, "selected ops: " + sel);
			} catch (Throwable t) {
				ExceptionHandler.handleException(t);
			}
			
			Set<SelectionKey> regKeys = mSelector.selectedKeys();
            Iterator<SelectionKey> it = regKeys.iterator();  
            while (it.hasNext()) {
            		SelectionKey key = it.next();
            		it.remove();
            		if(!key.isValid()) {
            			continue;
            		}
            		
//            		Log.d(TAG, "select " + key + " ops " + key.readyOps());
            		if(key != null && key.attachment() instanceof IAcceptor) {
            			((IAcceptor)key.attachment()).accept(key,key.readyOps());
            		}
            }
		}
		
		Messenger.notifyMessage(mListener, IProxyListener.PROXY_STOP, mIsLocal);
	}
	
	public void stop() {
		mWorking = false;
	}
	
	private synchronized void incConnection() {
		++mConnections;
		Log.r(TAG, "inc " + mConnections);
	}
	
	private synchronized void decConnection() {
		--mConnections;
		Log.r(TAG, "dec " + mConnections);
	}
	
	@Override
	public Result accept(SelectionKey selKey,int opt) {
		if(selKey.isAcceptable()) {
			Log.d(TAG, "recv a connection...");
			try {
				SocketChannel sc = mSocketChannel.accept();
				if(sc != null) {					
					mRelayerPool.obtain(sc);
//					new Relayer(sc);
				}
			} catch (Throwable e) {
				ExceptionHandler.handleException(e);
			}
		}
		return null;
	}

	private class Relayer implements ICallback,ITrafficEvent,ISpeedListener{
		
		private SSockChannel mChannel;
		private SpeedMetrics mMetrics;
		private int mConnId;
		
		private Relayer(SocketChannel conn) {
			init(conn);
		}
		
		private final String getTag() {
			return "Relayer_c"+mConnId;
		}
		
		private void init(SocketChannel conn) {
			mConnId = generateConnectionId();
			String clientAddr = (conn.socket() != null && conn.socket().getInetAddress() != null)?conn.socket().getInetAddress().getHostAddress():"unknown";
			Log.d(getTag(), "receive an conntion " + clientAddr);
			
			Messenger.notifyMessage(mListener, IProxyListener.RECV_CONN,mConnId, clientAddr);
			
			incConnection();
			mChannel = new SSockChannel(mSelector,mChannelBufferSize);
			mChannel.setConnId(mConnId);
			AbsS5Stage stage = new S5InitStage(mChannel, mIsLocal, this);
			stage.setConnId(mConnId);
			stage.start();
			
			mChannel.setSource(conn);
			mChannel.setTrafficListener(this);
			mMetrics = new SpeedMetrics(this);
			
			if(mIsLocal) {
				try {
					SocketChannel sc = SocketChannel.open();
					sc.configureBlocking(false);
					sc.connect(mProxyAddress);
					mChannel.setDest(sc);
				} catch (Throwable e) {
					Log.e(getTag(), "failed to connect to proxy server.");
					ExceptionHandler.handleException(e);
					Messenger.notifyMessage(mListener, IProxyListener.ERROR,mConnId, Result.E_S5_BIND_PROXY_FAILED);
					release();
					return;
				}
			}
		}
		
		private void release() {
			Log.d(TAG, "release conn " + mConnId);
			mChannel.destroy();
			decConnection();
			
			Messenger.notifyMessage(mListener, IProxyListener.STATE_UPDATE, mConnId, SProxyIface.STATE.TERMINATE);
			
			mConnId = -1;
			mRelayerPool.release(this);
		}
		
		@Override
		public void onStateChange(int newState, Object... params) {
			Messenger.notifyMessage(mListener, IProxyListener.STATE_UPDATE, mConnId, newState, params);
		}
		
		@Override
		public void onError(Result result) {
			Log.e(getTag(), result.getMessage());
			
			Messenger.notifyMessage(mListener,IProxyListener.ERROR,mConnId, result);
			
			release();
			
		}
		
		@Override
		public void onSpeed(int tag, int speed) {
			Messenger.notifyMessage(mListener,IProxyListener.SPEED, mConnId, (byte)(tag&0xFF), speed);
		}
		
		@Override
		public void onSrcIn(int len, long total) {
			mMetrics.add(IProxyListener.SPEEDTYPE.SRC_IN, len);
			Messenger.notifyMessage(mListener,IProxyListener.SRC_IN, mConnId, total);
		}

		@Override
		public void onSrcOut(int len, long total) {
			mMetrics.add(IProxyListener.SPEEDTYPE.SRC_OUT, len);
			Messenger.notifyMessage(mListener,IProxyListener.SRC_OUT, mConnId, total);
		}

		@Override
		public void onDestIn(int len, long total) {
			mMetrics.add(IProxyListener.SPEEDTYPE.DEST_IN, len);
			Messenger.notifyMessage(mListener,IProxyListener.DEST_IN, mConnId, total);
		}

		@Override
		public void onDestOut(int len, long total) {
			mMetrics.add(IProxyListener.SPEEDTYPE.DEST_OUT, len);
			Messenger.notifyMessage(mListener,IProxyListener.DEST_OUT, mConnId, total);
		}

		@Override
		public void onConnInfo(String ip, String domain, int port) {
			Messenger.notifyMessage(mListener,IProxyListener.CONN_INFO, mConnId, ip,domain,port);
		}

		@Override
		public void onSrcOpsUpdate(int ops) {
			Messenger.notifyMessage(mListener,IProxyListener.SRC_INTRS_OPS, mConnId, ops);
		}

		@Override
		public void onDestOpsUpdate(int ops) {
			Messenger.notifyMessage(mListener,IProxyListener.DEST_INTRS_OPS, mConnId, ops);
		}
	}

}
