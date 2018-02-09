package com.chedifier.netsword.socks5;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import com.chedifier.netsword.base.ExceptionHandler;
import com.chedifier.netsword.base.IOUtils;
import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.StringUtils;
import com.chedifier.netsword.cipher.Cipher;
import com.chedifier.netsword.cipher.Cipher.DecryptResult;
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

	private final int BUFFER_SIZE;

	private final String getTag() {
		return "SSockChannel_c" + mConnId;
	}

	public SSockChannel(Selector selector) {
		mSelector = selector;

		BUFFER_SIZE = Configuration.getConfigInt(Configuration.BLOCKSIZE, 1024*100);
		mUpStreamBufferIn = ByteBuffer.allocate(BUFFER_SIZE);
		mUpStreamBufferOut = ByteBuffer.allocate(BUFFER_SIZE);
		mDownStreamBufferIn = ByteBuffer.allocate(BUFFER_SIZE);
		mDownStreamBufferOut = ByteBuffer.allocate(BUFFER_SIZE);

		mAlive = true;
	}

	public void setListener(IChannelEvent l) {
		mListener = l;
	}

	public void setConnId(int id) {
		mConnId = id;
	}

	public int getConnId() {
		return mConnId;
	}

	public void setDest(SocketChannel socket) {
		if(!mAlive) {
			Log.e(getTag(), "setDest>>> channel has died.");
			return;
		}
		
		if (mDest == null && socket != null) {
			mDest = socket;
			try {
				mDest.configureBlocking(false);
			} catch (IOException e) {
				ExceptionHandler.handleException(e);
			}

			updateOps(false, true, SelectionKey.OP_CONNECT | SelectionKey.OP_READ);
			return;
		}

		Log.e(getTag(), "dest socket already setted,can not be set dumplicated.");
	}

	public void setSource(SocketChannel socket) {
		if(!mAlive) {
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
		if(!mAlive) {
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
				pauseIn(up, true);
			}
		}
		return r;
	}

	public int writeToBuffer(boolean up, byte[] data) {
		if(!mAlive) {
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
				pauseIn(up, true);
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
	 * @return the bytes be relayed, possibile 0 if nothing be relayed or -1 if dest
	 *         buffer is full filled.
	 */
	private int encryptRelay(ByteBuffer src, ByteBuffer dest) {
		if (src.position() <= 0) {
			Log.e(getTag(), "encryptRelay>>> not data in src,nothing need to relay.");
			return 0;
		}

		byte[] data = Cipher.encrypt(src.array(), 0, src.position());
		if (data == null) {
			Log.e(getTag(), "encryptRelay>>> encrypt data failed.");
			notifyRelayFailed();
			return 0;
		}
		if (dest.remaining() >= data.length) {
			dest.put(data);
			src.clear();
			return data.length;
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

		DecryptResult decRes = Cipher.decrypt(src.array(), 0, src.position());
		if (decRes != null && decRes.origin != null && decRes.origin.length > 0 && decRes.decryptLen > 0) {
			byte[] data = decRes.origin;
			if (dest.remaining() >= data.length) {
				dest.put(data);
				cutBuffer(src, decRes.decryptLen);
				return decRes.decryptLen;
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
			Log.d(getTag(),
					"before cut " + len + " : " + StringUtils.toRawString(buffer.array(), 0, buffer.position()));
			buffer.flip();
			buffer.position(len);
			buffer.compact();
			Log.d(getTag(), "after cut " + len + " : " + StringUtils.toRawString(buffer.array(), 0, buffer.position()));
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
		SelectionKey key = src ? mSourceKey : mDestKey;
		if (key != null) {
			int oldOps = key.interestOps();
			opts = add ? (opts | oldOps) : (oldOps & (~opts));
		}

		if (src) {
			mSourceKey = registerOpts(mSource, opts);
		} else {
			mDestKey = registerOpts(mDest, opts);
		}
	}

	private void pauseIn(boolean src, boolean pause) {
		updateOps(src, !pause, SelectionKey.OP_READ);
		if (src) {
			mSrcInPaused = pause;
		} else {
			mDestInPaused = pause;
		}
	}

	public void destroy() {
		mAlive = false;
		
		Log.r(getTag(), "total>>> src>" + mSrcIn + ",src<" + mSrcOut + ",dest>"+mDestIn+",dest<"+mDestOut);
		
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
			Log.d(getTag(), "read content: " + StringUtils.toRawString(buffer.array(), buffer.position() - r, r));
		} catch (Throwable e) {
			Log.e(getTag(), "read socket channel failed. " + e.getMessage());
			ExceptionHandler.handleException(e);
			r = -1;
		}
		
		if(r < 0) {
			notifySocketClosed();
		}

		return r;
	}

	private int write(SocketChannel socketChannel, ByteBuffer buffer) {
		try {
			buffer.flip();
			int w = socketChannel.write(buffer);

			Log.d(getTag(), "write " + w + " bytes,remain " + buffer.remaining());
			Log.d(getTag(), "write content: " + StringUtils.toRawString(buffer.array(), 0, w));

			buffer.compact();
			return w;
		} catch (Throwable e) {
			Log.e(getTag(), "write socket channel failed." + e.getMessage());
			ExceptionHandler.handleException(e);
			notifySocketClosed();
		}

		return -1;
	}

	@Override
	public Result accept(SelectionKey selKey, int opts) {
		if(!mAlive) {
			Log.e(getTag(), "accept>>> channel has died.");
			return null;
		}
		
		if (selKey == mSourceKey) {
			if ((opts & SelectionKey.OP_ACCEPT) > 0) {
				Log.e(getTag(), "src receive an unexpected ACCEPT ops: " + opts);
				return null;
			}

			if ((opts & SelectionKey.OP_CONNECT) > 0) {
				Log.e(getTag(), "src receive an unexpected CONNECT ops: " + opts);
				return null;
			}

			if ((opts & SelectionKey.OP_READ) > 0) {
				Log.d(getTag(), "src recv OP_READ");
				int r = read(mSource, mUpStreamBufferIn);
				if (r <= 0) {
					Log.e(getTag(), "read data in src failed." + r + " block read in.");
					pauseIn(true, true);
				}else {
					mSrcIn += r;
				}
			}

			if ((opts & SelectionKey.OP_WRITE) > 0) {
				Log.d(getTag(), "src recv OP_WRITE");
				if (mDownStreamBufferOut != null && mDownStreamBufferOut.position() > 0) {
					int w = write(mSource, mDownStreamBufferOut);
				
					if (w > 0) {
						if(mDownStreamBufferOut.remaining() > (BUFFER_SIZE >> 1) && mDestInPaused) {
							Log.d(getTag(), "out buffer has enough remaining, open dest read in.");
							pauseIn(false, false);
						}
						
						mSrcOut += w;
					}
				}

				if (mDownStreamBufferOut == null || mDownStreamBufferOut.position() <= 0) {
					updateOps(true, false, SelectionKey.OP_WRITE);
				}
			}

			notifySourceOps(opts);
		} else if (selKey == mDestKey) {
			if ((opts & SelectionKey.OP_ACCEPT) > 0) {
				Log.e(getTag(), "receive an unexpected ACCEPT ops: " + opts);
				return null;
			}

			if ((opts & SelectionKey.OP_CONNECT) > 0) {
				Log.d(getTag(), "dest connected.");
			}

			if ((opts & SelectionKey.OP_READ) > 0) {
				Log.d(getTag(), "recv dest OP_READ");
				int r = read(mDest, mDownStreamBufferIn);
				if (r <= 0) {
					Log.e(getTag(), "read from dest failed," + r + " pause dest read.");
					pauseIn(false, true);
				}else {
					mDestIn += r;
				}
			}

			if ((opts & SelectionKey.OP_WRITE) > 0) {// dest channel is writable now
				Log.d(getTag(), "recv dest OP_WRITE");
				if (mUpStreamBufferOut != null && mUpStreamBufferOut.position() > 0) {
					int w = write(mDest, mUpStreamBufferOut);
					if (w > 0) {
						if(mUpStreamBufferOut.remaining() > (BUFFER_SIZE >> 1) && mSrcInPaused) {
							Log.d(getTag(), "out buffer has enough remaining, open src read in.");
							pauseIn(true, false);
						}
						
						mDestOut += w;
					}
				}

				if (mUpStreamBufferOut == null || mUpStreamBufferOut.position() <= 0) {// all data have been send,shutdown write event
					updateOps(false, false, SelectionKey.OP_WRITE);
				}
			}

			notifyDestOps(opts);
		} else {
			Log.e(getTag(), "accept an unexpected ops: " + opts);
		}

		return null;
	}
	
	private void notifySourceOps(int ops) {
		if(mAlive && mListener != null) {
			mListener.onSourceOpts(ops);
		}
	}
	
	private void notifyDestOps(int ops) {
		if(mAlive && mListener != null) {
			mListener.onDestOpts(ops);
		}
	}
	
	private void notifySocketClosed() {
		if(mAlive && mListener != null) {
			mListener.onSocketBroken();
		}
	}

	private void notifyRelayFailed() {
		if(mAlive && mListener != null) {
			mListener.onRelayFailed();
		}
	}
	
	public static interface IChannelEvent {
		void onSourceOpts(int opts);

		void onDestOpts(int opts);

		void onSocketBroken();

		void onRelayFailed();
	}

}
