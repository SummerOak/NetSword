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
import com.chedifier.netsword.cipher.Cipher;
import com.chedifier.netsword.cipher.Cipher.DecryptResult;
import com.chedifier.netsword.iface.Result;
import com.chedifier.netsword.socks5.AcceptorWrapper.IAcceptor;

public class SSockChannel implements IAcceptor {

	private int mConnId;
	private SocketChannel mSource;
	private SocketChannel mDest;

	private Selector mSelector;
	private SelectionKey mSourceKey;
	private SelectionKey mDestKey;

	private ByteBuffer mUpStreamBufferIn;
	private ByteBuffer mUpStreamBufferOut;
	private ByteBuffer mDownStreamBufferIn;
	private ByteBuffer mDownStreamBufferOut;

	private long mSrcIn = 0l;
	private long mSrcOut = 0l;
	private long mDestIn = 0l;
	private long mDestOut = 0l;

	private boolean mSrcInPaused = false;
	private boolean mDestInPaused = false;
	private boolean mAlive = false;

	private IChannelEvent mListener;
	private ITrafficEvent mTrafficListener;

	private final int BUFFER_SIZE;

	private final String getTag() {
		return "SSockChannel_c" + mConnId;
	}

	public SSockChannel(Selector selector,int bufferSize) {
		mSelector = selector;
		BUFFER_SIZE = bufferSize;
		
		mUpStreamBufferIn = ByteBuffer.allocate(BUFFER_SIZE);
		mUpStreamBufferOut = ByteBuffer.allocate(BUFFER_SIZE << 1);
		mDownStreamBufferIn = ByteBuffer.allocate(BUFFER_SIZE);
		mDownStreamBufferOut = ByteBuffer.allocate(BUFFER_SIZE << 1);

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
		Log.t(getTag(), "setDest " + socket + "  " + mDest);
		if (!mAlive) {
			Log.e(getTag(), "setDest>>> channel has died.");
			return;
		}

		if (mDest == null && socket != null) {
			mDest = socket;
			try {
				mDest.configureBlocking(false);
			} catch (IOException e) {
				Log.t(getTag(), "configureBlocking failed " + e.getMessage());
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
			updateOps(true, true, SelectionKey.OP_READ);
			return;
		}

		Log.e(getTag(), "src  socket already setted,can not be set dumplicated.");
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
		if (r > 0 || r == -1) {
			updateOps(!up, true, SelectionKey.OP_WRITE);
			if (r == -1) {
				Log.e(getTag(), "relay " + up + ">>> out buffer is full filled. pause reading in.");
				pauseOrResume(up, true);
			}
		}
		return r;
	}

	public int writeToBuffer(boolean up, byte[] data) {
		if (!mAlive) {
			Log.e(getTag(), "write>>> channel has died.");
			return -1;
		}

		int w = 0;
		if (data != null && data.length > 0) {
			ByteBuffer buffer = up ? mUpStreamBufferOut : mDownStreamBufferOut;
			if (buffer.remaining() >= data.length) {
				buffer.put(data);
				w = data.length;
				updateOps(!up, true, SelectionKey.OP_WRITE);
			} else {
				Log.e(getTag(), "writeToBuffer" + up + ">>> out buffer is full filled,need " + data.length + " remain "
						+ buffer.remaining() + " pause data read in.");
				pauseOrResume(up, true);
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
			Log.e(getTag(), "encryptRelay>>> not data in src,nothing need to relay.");
			return 0;
		}

		int len = src.position();
		byte[] data = Cipher.encrypt(src.array(), 0, len);
		if (data == null) {
			Log.e(getTag(), "encryptRelay>>> encrypt data failed.");
			notifyRelayFailed();
			return 0;
		}
		if (dest.remaining() >= data.length) {
			dest.put(data);
			src.clear();
			return len;
		} else {
			Log.e(getTag(),
					"encryptRelay>>> out buffer is full filled: need " + data.length + " remain " + dest.remaining());
			notifyRelayFailed();
			return -1;
		}
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
		DecryptResult decRes = Cipher.decrypt(src.array(), 0, len);
		if (decRes != null && decRes.origin != null && decRes.origin.length > 0 && decRes.decryptLen > 0) {
			byte[] data = decRes.origin;
			if (dest.remaining() >= data.length) {
				dest.put(data);
				cutBuffer(src, decRes.decryptLen);
				return len;
			} else {
				Log.e(getTag(), "encryptRelay>>> out buffer is full filled,need " + data.length + " remain "
						+ dest.remaining());
				notifyRelayFailed();
				return -1;
			}
		} else {
			Log.e(getTag(), "decryptRelay>>> decrypt packs failed.");
			notifyRelayFailed();
			return 0;
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

	private void updateOps(boolean src, boolean add, int opts) {
		Log.t(getTag(), "updateOps " + (add?"enable ":"disable ") + (src?" src " :" dest ") + opts + ": " + NetUtils.getOpsDest(opts));
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
	}

	private void pauseOrResume(boolean src, boolean pause) {
		updateOps(src, !pause, SelectionKey.OP_READ);
		if (src) {
			mSrcInPaused = pause;
		} else {
			mDestInPaused = pause;
		}
	}

	public void destroy() {
		mAlive = false;

		Log.r(getTag(), "total>>> src>" + mSrcIn + ",src<" + mSrcOut + ",dest>" + mDestIn + ",dest<" + mDestOut);

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

		if (mUpStreamBufferIn != null) {
			mUpStreamBufferIn = null;
		}
		if (mDownStreamBufferIn != null) {
			mDownStreamBufferIn = null;
		}

		if (mUpStreamBufferOut != null) {
			mUpStreamBufferOut = null;
		}

		if (mDownStreamBufferOut != null) {
			mDownStreamBufferOut = null;
		}
	}

	private int read(SocketChannel socketChannel, ByteBuffer buffer) {
		int r = 0;
		try {
			Log.d(getTag(), "pre read,buffer remain " + buffer.remaining());
			r = socketChannel.read(buffer);
			Log.d(getTag(), "read " + r + " bytes,total " + buffer.position());
			Log.i(getTag(), "read content: " + StringUtils.toRawString(buffer.array(), buffer.position() - r, r));
		} catch (Throwable e) {
			Log.e(getTag(), "read socket channel failed. " + e.getMessage());
			ExceptionHandler.handleException(e);
			r = -1;
		}

		if (r < 0) {
			notifySocketClosed(Result.E_S5_SOCKET_READ_FAILED);
		}

		return r;
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
			notifySocketClosed(Result.E_S5_SOCKET_WRITE_FAILED);
		}

		return -1;
	}

	@Override
	public Result accept(SelectionKey selKey, int opts) {
		if (!mAlive && selKey.isValid()) {
			Log.e(getTag(), "accept>>> channel has died.");
			return null;
		}

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
					Log.e(getTag(), "read data in src failed." + r + " block read in.");
					pauseOrResume(true, true);
				} else {
					onSrcIn(r);
				}
			}

			if (selKey.isValid() && selKey.isWritable()) {
				Log.d(getTag(), "src recv OP_WRITE");
				if (mDownStreamBufferOut != null && mDownStreamBufferOut.position() > 0) {
					int w = write(mSource, mDownStreamBufferOut);

					if (w > 0) {
						if (mDownStreamBufferOut.remaining() > (BUFFER_SIZE >> 1) && mDestInPaused) {
							Log.d(getTag(), "out buffer has enough remaining, open dest read in.");
							pauseOrResume(false, false);
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
				Log.t(getTag(), "dest receive connect ops.");
				try {
					if(!mDest.finishConnect()) {
						Log.e(getTag(), "finish connect failed.");
						notifySocketClosed(Result.E_S5_BIND_PROXY_FAILED);
						return null;
					}else {
						updateOps(false, false, SelectionKey.OP_CONNECT);
						updateOps(false, true, SelectionKey.OP_READ);
						Log.r(getTag(), "bind proxy success!");
					}
				} catch (Throwable e) {
					ExceptionHandler.handleException(e);
					Log.e(getTag(), "conn to proxy failed");
					notifySocketClosed(Result.E_S5_BIND_PROXY_FAILED);
					return null;
				}
			}

			if (selKey.isValid() && selKey.isReadable()) {
				Log.d(getTag(), "recv dest OP_READ");
				int r = read(mDest, mDownStreamBufferIn);
				if (r <= 0) {
					Log.e(getTag(), "read from dest failed," + r + " pause dest read.");
					pauseOrResume(false, true);
				} else {
					onDestIn(r);
				}
			}

			if (selKey.isValid() && selKey.isWritable()) {// dest channel is writable now
				Log.d(getTag(), "recv dest OP_WRITE");
				if (mUpStreamBufferOut != null && mUpStreamBufferOut.position() > 0) {
					int w = write(mDest, mUpStreamBufferOut);
					if (w > 0) {
						if (mUpStreamBufferOut.remaining() > (BUFFER_SIZE >> 1) && mSrcInPaused) {
							Log.d(getTag(), "out buffer has enough remaining, open src read in.");
							pauseOrResume(true, false);
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

	private void notifySocketClosed(Result result) {
		if (mAlive && mListener != null) {
			mListener.onSocketBroken(result);
		}
	}

	private void notifyRelayFailed() {
		if (mAlive && mListener != null) {
			mListener.onRelayFailed();
		}
	}
	
	private void notifyIntrestOpsUpdate(boolean src) {
		Log.d(getTag(), "notifyIntrestOpsUpdate " + (src?"source ":"dest"));
		if(mAlive && mListener != null) {
			if(src) {
				mListener.onSrcOpsUpdate(mSourceKey.interestOps());
			}else {				
				mListener.onDestOpsUpdate(mDestKey.interestOps());
			}
		}
	}

	public static interface IChannelEvent {

		void onSourceOpts(int opts);

		void onDestOpts(int opts);

		void onSocketBroken(Result result);

		void onRelayFailed();
		
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
