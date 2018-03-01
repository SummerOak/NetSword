package com.chedifier.netsword.memory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.ObjectPool;
import com.chedifier.netsword.base.ObjectPool.IConstructor;

public class ByteBufferPool {
	private static final String TAG = "ByteBufferPool";
	
	private static Map<Integer,ObjectPool<ByteBuffer>> sPool = new HashMap<>();
	private static long sMemInPool = 0L;
	private static long sMemTotal = 0L;
	private static final int[] CALIBRATION = new int[] {256,512,1024,1<<13,1<<18,1<<20,1<<21};
	
	private static List<IMemInfoListener> sMemListeners = new CopyOnWriteArrayList<>();
	
	public static void addListener(IMemInfoListener l) {
		for(IMemInfoListener t:sMemListeners) {
			if(t == l) {
				return;
			}
		}
		
		sMemListeners.add(l);
	}
	
	public static void removeListener(IMemInfoListener l) {
		Iterator<IMemInfoListener> itr = sMemListeners.iterator();
		while(itr.hasNext()) {
			if(itr.next() == l) {
				itr.remove();
			}
		}
	}
	
	private static void notify(long pool,long total) {
		for(IMemInfoListener l:sMemListeners) {
			l.onMemoryInfo(pool, total);
		}
	}
	
	private static int align(int size) {
		if(size <= 0) {
			return 0;
		}
		
		for(int i=0;i<CALIBRATION.length;i++) {
			if(size <= CALIBRATION[i]) {
				return CALIBRATION[i];
			}
		}
		
		return 0;
	}
	
	public static synchronized ByteBuffer obtain(int size) {
		final int fsize = align(size);
		if(fsize <= 0) {
			Log.e(TAG, "wrong size");
			return null;
		}
		
		ObjectPool<ByteBuffer> pool = sPool.get(fsize);
		if(pool == null) {
			pool = new ObjectPool<ByteBuffer>(new IConstructor<ByteBuffer>() {
				@Override
				public ByteBuffer newInstance(Object... params) {
					sMemTotal += fsize;
					
					return ByteBuffer.allocate(fsize);
				}
				
				@Override
				public void initialize(ByteBuffer e, Object... params) {
					e.clear();
					sMemInPool -= fsize;
				}
			},Integer.MAX_VALUE);
			
			sPool.put(fsize, pool);
		}
		
		ByteBuffer buffer = pool.obtain();
		if(buffer == null) {
			Log.e(TAG, "obtain buffer from pool failed.");
		}
		
		notify(sMemInPool,sMemTotal);
		Log.i(TAG, "obtain: sMemTotal=" + sMemTotal + " sMemInPool="+sMemInPool);
		
		return buffer;
	}
	
	public static synchronized void recycle(ByteBuffer buffer) {
		if(buffer != null) {
			int size = buffer.capacity();
			ObjectPool<ByteBuffer> pool = sPool.get(size);
			if(pool != null) {
				if(pool.recycle(buffer)) {
					sMemInPool += size;
				}
			}
		}
		
		Log.i(TAG, "recycle: sMemInPool="+sMemInPool);
	}
	
	public interface IMemInfoListener{
		void onMemoryInfo(long pool,long total);
	}
}
