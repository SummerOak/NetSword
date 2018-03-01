package com.chedifier.netsword.socks5;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import com.chedifier.netsword.base.ExceptionHandler;
import com.chedifier.netsword.base.IOUtils;
import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.NetUtils;
import com.chedifier.netsword.base.StringUtils;
import com.chedifier.netsword.base.Timer;
import com.chedifier.netsword.base.TimerTask;
import com.chedifier.netsword.cipher.Cipher;
import com.chedifier.netsword.iface.Error;
import com.chedifier.netsword.memory.ByteBufferPool;
import com.chedifier.netsword.socks5.AcceptorWrapper.IAcceptor;

public class SSockChannel implements IAcceptor {

	private int mConnId;
	private SocketChannel mSource;
	private SocketChannel mDest;
	
	//destruct this channel if none I/O operations in specify timeout.
	private Timer mTimer;
	private TimerTask mSuicideTask;

	private Selector mSelector;
	private SelectionKey mSourceKey;
	private SelectionKey mDestKey;
	
	private boolean mDestConnected;

	private ByteBuffer mUpStreamBufferIn;
	private ByteBuffer mUpStreamBufferOut;
	private ByteBuffer mDownStreamBufferIn;
	private ByteBuffer mDownStreamBufferOut;

	private long mSrcIn = 0l;
	private long mSrcOut = 0l;
	private long mDestIn = 0l;
	private long mDestOut = 0l;
	
	private final byte MAX_RETRY_FOR_READ = 3;
	private byte mRetryTimesWhileReadNull = 0;
	
	private boolean mAlive = false;

	private IChannelEvent mListener;
	private ITrafficEvent mTrafficListener;

	private final int BUFFER_SIZE;
	private final int CHUNK_SIZE;

	private final String getTag() {
		return "SSockChannel_c" + mConnId;
	}

	public SSockChannel(Selector selector,int bufferSize,int chunkSize) {
		mSelector = selector;
		BUFFER_SIZE = bufferSize;
		CHUNK_SIZE = chunkSize;
		
		mDestConnected = false;
		
		mUpStreamBufferIn = ByteBufferPool.obtain(BUFFER_SIZE);
		mUpStreamBufferOut = ByteBufferPool.obtain(BUFFER_SIZE << 1);
		mDownStreamBufferIn = ByteBufferPool.obtain(BUFFER_SIZE);
		mDownStreamBufferOut = ByteBufferPool.obtain(BUFFER_SIZE << 1);
		
		mTimer = new Timer();
		mAlive = true;
	}

	public void setListener(IChannelEvent l) {
		mListener = l;
	}

	public void setTrafficListener(ITrafficEvent l) {
		mTrafficListener = l;
	}

	public void setConnId(int id) {
		mConnId = id;
	}

	public int getConnId() {
		return mConnId;
	}

	public void setDest(SocketChannel socket) {
		Log.i(getTag(), "setDest " + socket + "  " + mDest);
		if (!mAlive) {
			Log.e(getTag(), "setDest>>> channel has died.");
			return;
		}

		if (mDest == null && socket != null) {
			mDest = socket;
			try {
				mDest.configureBlocking(false);
			} catch (IOException e) {
				Log.i(getTag(), "configureBlocking failed " + e.getMessage());
				ExceptionHandler.handleException(e);
			}

			updateOps(false, true, SelectionKey.OP_CONNECT);
			return;
		}

		Log.e(getTag(), "dest socket already setted,can not be set dumplicated.");
	}

	public void setSource(SocketChannel socket) {
		if (!mAlive) {
			Log.e(getTag(), "setDest>>> channel has died.");
			return;
		}

		if (mSource == null && socket != null) {
			mSource = socket;
			try {
				mSource.configureBlocking(false);
			} catch (IOException e) {
				ExceptionHandler.handleException(e);
			}
			return;
		}

		Log.e(getTag(), "src  socket already setted,can not be set dumplicated.");
	}
	
	public int getChunkSize() {
		return CHUNK_SIZE;
	}

	public int relay(boolean up, boolean encrypt) {
		if (!mAlive) {
			Log.e(getTag(), "relay>>> channel has died.");
			return -1;
		}

		ByteBuffer from = up ? mUpStreamBufferIn : mDownStreamBufferIn;
		ByteBuffer to = up ? mUpStreamBufferOut : mDownStreamBufferOut;
		int r = encrypt ? encryptRelay(from, to) : decryptRelay(from, to);
		Log.d(getTag(), "relayed " + r + " bytes.");
		if (to.position() > 0) {
			updateOps(!up, true, SelectionKey.OP_WRITE);
		}
		
		if (r == -1) {
			Log.e(getTag(), "relay " + up + ">>> out buffer is full filled. pause reading in.");
			updateOps(up, false, SelectionKey.OP_READ);
		}
		
		return r;
	}

	public int writeToBuffer(boolean up, ByteBuffer data) {
		if (!mAlive) {
			Log.e(getTag(), "write>>> channel has died.");
			return -1;
		}

		int w = 0;
		if (data != null && data.remaining() > 0) {
			ByteBuffer buffer = up ? mUpStreamBufferOut : mDownStreamBufferOut;
			int r = data.remaining();
			if (buffer.remaining() >= data.remaining()) {
				try {
					buffer.put(data);
					w = r;
					updateOps(!up, true, SelectionKey.OP_WRITE);
				}catch(Exception e) {
					ExceptionHandler.handleException(e);
				}
			} else {
				Log.e(getTag(), "writeToBuffer" + up + ">>> out buffer is full filled,need " + r + " remain "
						+ buffer.remaining() + " pause data read in.");
				updateOps(up,false,SelectionKey.OP_READ);
			}
		}

		return w;
	}

	/**
	 * encrypt data in src and relay to dest.
	 * 
	 * @param src
	 *            : the source data need be encrypted and relayed to dest.
	 * @param dest:
	 *            the destnation where data in src need be encrypted and relayed to.
	 * @return the bytes be relayed of src, possibile 0 if nothing be relayed or -1
	 *         if dest buffer is full filled.
	 */
	private int encryptRelay(ByteBuffer src, ByteBuffer dest) {
		if (src.position() <= 0) {
			Log.d(getTag(), "encryptRelay>>> not data in src,nothing need to relay.");
			return 0;
		}

		int len = src.position();
		ByteBuffer outBuffer = ByteBufferPool.obtain(Cipher.estimateEncryptLen(len,CHUNK_SIZE));
		try {
			if(outBuffer != null && outBuffer.remaining() >= len) {
				int el = Cipher.encrypt(src.array(), 0, len,CHUNK_SIZE,outBuffer);
				if (el > 0) {
					outBuffer.flip();
					int ll = outBuffer.remaining();
					if(dest.remaining() >= ll) {
						try {
							dest.put(outBuffer);
							cutBuffer(src, el);
							return el;
						}catch(Exception e) {
							ExceptionHandler.handleException(e);
						}
						
					}else {
						Log.e(getTag(),
								"encryptRelay>>> out buffer is full filled: need " + ll + " remain " + dest.remaining());
						notifyRelayFailed(Error.E_S5_OUT_BUFFER_FULL_FILLED);
						return -1;
					}
				}else {
					Log.e(getTag(), "encrypt failed");
				}
			}else {
				Log.e(getTag(), "obtain out buffer failed");
			}
		}finally {
			ByteBufferPool.recycle(outBuffer);
		}
		
		Log.e(getTag(), "encryptRelay>>> encrypt data failed.");
		notifyRelayFailed(Error.E_S5_RELAY_ENCRYPT_FAILED);
		return 0;
	}

	/**
	 * decrypt data in src and relay to dest.
	 * 
	 * @param src
	 *            : the source data need be decrypted and relayed to dest.
	 * @param dest:
	 *            the destnation where data in src need be decrypted and relayed to.
	 * @return the bytes be relayed, possibile 0 if nothing be relayed or -1 if dest
	 *         buffer is full filled.
	 */
	private int decryptRelay(ByteBuffer src, ByteBuffer dest) {
		if (src.position() <= 0) {
			Log.e(getTag(), "decryptRelay>>> no data in src,nothing need to relay.");
			return 0;
		}

		int len = src.position();
		ByteBuffer decOutBuffer = ByteBufferPool.obtain(Cipher.estimateDecryptLen(len,getChunkSize()));
		try {
			int dl = Cipher.decrypt(src.array(), 0, len,getChunkSize(),decOutBuffer);
			if (dl > 0) {
				decOutBuffer.flip();
				final int ll = decOutBuffer.remaining();
				if (dest.remaining() >= ll) {
					dest.put(decOutBuffer);
					cutBuffer(src, dl);
					return dl;
				} else {
					Log.e(getTag(), "decryptRelay>>> out buffer is full filled,need " + ll + " remain "
							+ dest.remaining());
					notifyRelayFailed(Error.E_S5_OUT_BUFFER_FULL_FILLED);
					return -1;
				}
			} else {
				Log.d(getTag(), "decryptRelay>>> decrypt packs failed.");
				notifyRelayFailed(Error.E_S5_RELAY_DECRYPT_FAILED);
				return 0;
			}
		}finally {
			ByteBufferPool.recycle(decOutBuffer);
		}
		
	}

	public int cutBuffer(ByteBuffer buffer, int len) {
		if (buffer != null && buffer.position() >= len) {
			Log.i(getTag(), "before cut " + len + " : " + buffer.position());
			buffer.flip();
			buffer.position(len);
			buffer.compact();
			Log.i(getTag(), "after cut " + len + " : " + buffer.position());
			return len;
		}

		return 0;
	}

	public ByteBuffer getSrcInBuffer() {
		return mUpStreamBufferIn;
	}

	public ByteBuffer getDestInBuffer() {
		return mDownStreamBufferIn;
	}

	public ByteBuffer getSrcOutBuffer() {
		return mUpStreamBufferOut;
	}

	public ByteBuffer getDestOutBuffer() {
		return mDownStreamBufferOut;
	}

	private SelectionKey registerOpts(SocketChannel socketChannel, int ops) {
		if (socketChannel != null) {
			try {
				SelectionKey selKey = socketChannel.register(mSelector, ops);
				selKey.attach(this);
				return selKey;
			} catch (Throwable e) {
				ExceptionHandler.handleException(e);
			}
		}

		return null;
	}

	public void updateOps(boolean src, boolean add, int opts) {
		Log.i(getTag(), "updateOps " + (add?"enable ":"disable ") + (src?" src " :" dest ") + opts + ": " + NetUtils.getOpsDest(opts));
		SelectionKey key = src ? mSourceKey : mDestKey;
		if (key != null) {
			int oldOps = key.interestOps();
			opts = add ? (opts | oldOps) : (oldOps & (~opts));
		}

		if (src) {
			if(mSourceKey != null && mSourceKey.isValid()) {				
				mSourceKey.interestOps(opts);
			}else {				
				mSourceKey = registerOpts(mSource, opts);
			}
		} else {
			if(mDestKey != null && mDestKey.isValid()) {
				mDestKey.interestOps(opts);
			}else {				
				mDestKey = registerOpts(mDest, opts);
			}
		}
		
		notifyIntrestOpsUpdate(src);
		
		if(isChannelDead()) {
			Log.e(getTag(), "socket has died,channel will closed.");
			notifySocketClosed(Error.E_S5_CHANNEL_DEAD);
		}
	}
	
	private boolean hasOps(SelectionKey key,int intres) {
		return (key != null && (key.interestOps()&intres) > 0);
	}
	
	private boolean isChannelDead() {
		if(mDestKey != null && mDestKey.interestOps() == 0 && mSourceKey != null && mSourceKey.interestOps() == 0) {
			return true;
		}
		
		return false;
	}

	public synchronized void destroy() {
		Log.r(getTag(), "total>>> src>" + mSrcIn + ",src<" + mSrcOut + ",dest>" + mDestIn + ",dest<" + mDestOut);

		mAlive = false;
		if (mDestKey != null) {
			mDestKey.cancel();
			mDestKey = null;
		}

		if (mSourceKey != null) {
			mSourceKey.cancel();
			mSourceKey = null;
		}

		IOUtils.safeClose(mDest);
		IOUtils.safeClose(mSource);
		mDest = mSource = null;
		mConnId = -1;
		
		if(mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}
		
		if(mSuicideTask != null) {
			mSuicideTask.cancel();
			mSuicideTask = null;
		}
		
		if (mUpStreamBufferIn != null) {
			ByteBufferPool.recycle(mUpStreamBufferIn);
			mUpStreamBufferIn = null;
		}
		if (mDownStreamBufferIn != null) {
			ByteBufferPool.recycle(mDownStreamBufferIn);
			mDownStreamBufferIn = null;
		}

		if (mUpStreamBufferOut != null) {
			ByteBufferPool.recycle(mUpStreamBufferOut);
			mUpStreamBufferOut = null;
		}

		if (mDownStreamBufferOut != null) {
			ByteBufferPool.recycle(mDownStreamBufferOut);
			mDownStreamBufferOut = null;
		}
	}

	private int read(SocketChannel socketChannel, ByteBuffer buffer) {
		try {
			Log.d(getTag(), "pre read,buffer remain " + buffer.remaining());
			int r = socketChannel.read(buffer);
			Log.d(getTag(), "read " + r + " bytes,total " + buffer.position());
			Log.i(getTag(), "read content: " + StringUtils.toRawString(buffer.array(), buffer.position() - r, r));
			return r;
		} catch (Throwable e) {
			Log.e(getTag(), "read socket channel failed. " + e.getMessage());
			ExceptionHandler.handleException(e);
		}

		return -1;
	}

	private int write(SocketChannel socketChannel, ByteBuffer buffer) {
		try {
			buffer.flip();
			int w = socketChannel.write(buffer);

			Log.d(getTag(), "write " + w + " bytes,remain " + buffer.remaining());
			Log.i(getTag(), "write content: " + StringUtils.toRawString(buffer.array(), 0, w));

			buffer.compact();
			return w;
		} catch (Throwable e) {
			Log.e(getTag(), "write socket channel failed." + e.getMessage());
			ExceptionHandler.handleException(e);
		}

		return -1;
	}
	
	private void startKillTimer() {
		if(mSuicideTask != null) {
			mSuicideTask.cancel();
		}
		
		mSuicideTask = new TimerTask() {
			
			@Override
			public void run() {
				notifySocketClosed(Error.E_S5_SOCKETCHANNEL_ZOMBIE);
			}
		};
		
		mTimer.schedule(mSuicideTask, 2*60*1000L);
	}

	@Override
	public synchronized Error accept(SelectionKey selKey, int opts) {
		if (!mAlive || !selKey.isValid()) {
			Log.e(getTag(), "accept>>> channel has died.");
			return null;
		}
		
		startKillTimer();

		if (selKey == mSourceKey) {
			if (selKey.isValid() && selKey.isAcceptable()) {
				Log.e(getTag(), "src receive an unexpected ACCEPT ops: " + opts);
				return null;
			}

			if (selKey.isValid() && selKey.isConnectable()) {
				Log.e(getTag(), "src receive an unexpected CONNECT ops: " + opts);
				return null;
			}

			if (selKey.isValid() && selKey.isReadable()) {
				Log.d(getTag(), "src recv OP_READ");
				int r = read(mSource, mUpStreamBufferIn);
				if (r <= 0) {
					if(++mRetryTimesWhileReadNull > MAX_RETRY_FOR_READ) {
						Log.e(getTag(), "read data in src failed." + r + " block read in.");
						updateOps(true, false, SelectionKey.OP_READ);
					}
				} else {
					mRetryTimesWhileReadNull = 0;
					onSrcIn(r);
				}
			}

			if (selKey.isValid() && selKey.isWritable()) {
				Log.d(getTag(), "src recv OP_WRITE");
				if (mDownStreamBufferOut != null && mDownStreamBufferOut.position() > 0) {
					int w = write(mSource, mDownStreamBufferOut);

					if (w > 0) {
						if (mDownStreamBufferOut.remaining() > (BUFFER_SIZE >> 1) 
								&& mDestConnected && mDestKey != null && mDest != null && !hasOps(mDestKey, SelectionKey.OP_READ)) {
							Log.d(getTag(), "out buffer has enough remaining, open dest read in.");
							updateOps(false, true, SelectionKey.OP_READ);
						}

						onSrcOut(w);
					}
				}

				if (mDownStreamBufferOut == null || mDownStreamBufferOut.position() <= 0) {
					updateOps(true, false, SelectionKey.OP_WRITE);
				}
			}
			
			if(selKey.isValid()) {				
				notifySourceOps(opts);
			}

		} else if (selKey == mDestKey) {
			if (selKey.isValid() && selKey.isAcceptable()) {
				Log.e(getTag(), "dest receive an unexpected ACCEPT ops: " + opts);
				return null;
			}

			if (selKey.isValid() && selKey.isConnectable()) {
				Log.i(getTag(), "dest receive connect ops.");
				try {
					if(!mDest.finishConnect()) {
						Log.e(getTag(), "finish connect failed.");
						notifySocketClosed(Error.E_S5_BIND_PROXY_FAILED);
						return null;
					}else {
						updateOps(false, false, SelectionKey.OP_CONNECT);
						updateOps(false, true, SelectionKey.OP_READ);
						mDestConnected = true;
						Log.r(getTag(), "bind proxy success!");
					}
				} catch (Throwable e) {
					ExceptionHandler.handleException(e);
					Log.e(getTag(), "conn to proxy failed");
					notifySocketClosed(Error.E_S5_BIND_PROXY_FAILED);
					return null;
				}
			}

			if (selKey.isValid() && selKey.isReadable()) {
				Log.d(getTag(), "recv dest OP_READ");
				int r = read(mDest, mDownStreamBufferIn);
				if (r <= 0) {
					if(++mRetryTimesWhileReadNull > MAX_RETRY_FOR_READ) {
						Log.e(getTag(), "read from dest failed," + r + " pause dest read.");
						updateOps(false, false, SelectionKey.OP_READ);
					}
				} else {	
					mRetryTimesWhileReadNull = 0;
					onDestIn(r);
				}
			}

			if (selKey.isValid() && selKey.isWritable()) {// dest channel is writable now
				Log.d(getTag(), "recv dest OP_WRITE");
				if (mUpStreamBufferOut != null && mUpStreamBufferOut.position() > 0) {
					int w = write(mDest, mUpStreamBufferOut);
					if (w > 0) {
						if (mUpStreamBufferOut.remaining() > (BUFFER_SIZE >> 1) 
								&& mSource != null && mSourceKey != null && !hasOps(mSourceKey, SelectionKey.OP_READ)) {
							Log.d(getTag(), "out buffer has enough remaining, open src read in.");
							updateOps(true, true, SelectionKey.OP_READ);
						}

						onDestOut(w);
					}
				}

				if (mUpStreamBufferOut == null || mUpStreamBufferOut.position() <= 0) {// all data have been
																						// send,shutdown write event
					updateOps(false, false, SelectionKey.OP_WRITE);
				}
			}

			if(selKey.isValid()) {				
				notifyDestOps(opts);
			}
		} else {
			Log.e(getTag(), "accept an unexpected ops: " + opts);
		}

		return null;
	}

	private void onSrcIn(int len) {
		mSrcIn += len;
		if(mTrafficListener != null) {
			mTrafficListener.onSrcIn(len,mSrcIn);
		}
	}

	private void onSrcOut(int len) {
		mSrcOut += len;
		if(mTrafficListener != null) {
			mTrafficListener.onSrcOut(len,mSrcOut);
		}
	}

	private void onDestIn(int len) {
		mDestIn += len;
		if(mTrafficListener != null) {
			mTrafficListener.onDestIn(len,mDestIn);
		}
	}

	private void onDestOut(int len) {
		mDestOut += len;
		if(mTrafficListener != null) {
			mTrafficListener.onDestOut(len,mDestOut);
		}
	}

	private void notifySourceOps(int ops) {
		if (mAlive && mListener != null) {
			mListener.onSourceOpts(ops);
		}
	}

	private void notifyDestOps(int ops) {
		if (mAlive && mListener != null) {
			mListener.onDestOpts(ops);
		}
	}

	private void notifySocketClosed(Error result) {
		if (mAlive && mListener != null) {
			mListener.onSocketBroken(result);
		}
	}

	private void notifyRelayFailed(Error result) {
		if (mAlive && mListener != null) {
			mListener.onRelayFailed(result);
		}
	}
	
	private void notifyIntrestOpsUpdate(boolean src) {
		Log.d(getTag(), "notifyIntrestOpsUpdate " + (src?"source ":"dest"));
		if(mAlive && mListener != null) {
			if(src && mSourceKey != null) {
				mListener.onSrcOpsUpdate(mSourceKey.interestOps());
			}else if(!src && mDestKey != null){				
				mListener.onDestOpsUpdate(mDestKey.interestOps());
			}
		}
	}

	public static interface IChannelEvent {

		void onSourceOpts(int opts);

		void onDestOpts(int opts);

		void onSocketBroken(Error result);

		void onRelayFailed(Error result);
		
		void onSrcOpsUpdate(int ops);
		
		void onDestOpsUpdate(int ops);
	}

	public static interface ITrafficEvent {
		void onSrcIn(int len,long total);

		void onSrcOut(int len,long total);

		void onDestIn(int len,long total);

		void onDestOut(int len,long total);
		
	}

}
