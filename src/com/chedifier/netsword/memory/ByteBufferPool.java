package com.chedifier.netsword.memory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.chedifier.netsword.base.ExceptionHandler;
import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.ObjectPool;
import com.chedifier.netsword.base.ObjectPool.IConstructor;

public class ByteBufferPool {
	private static final String TAG = "ByteBufferPool";
	
	private static Map<Integer,ObjectPool<ByteBuffer>> sPool = new HashMap<>();
	private static Set<Long> sInUsing = new HashSet<Long>();
	private static Map<Long,MemInfo> sMemInfo = new HashMap<>();
	
	private static long sMemInUsing = 0L;
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
	
	private static void notify(long using,long total) {
		for(IMemInfoListener l:sMemListeners) {
			l.onMemoryInfo(using, total);
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
	
	public static long getMemInUsing() {
		return sMemInUsing;
	}
	
	public static long getMemTotal() {
		return sMemTotal;
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
					sMemInUsing += fsize;
					
					return ByteBuffer.allocate(fsize);
				}
				
				@Override
				public void initialize(ByteBuffer e, Object... params) {
					
					e.clear();
					sMemInUsing += fsize;
				}
			},Integer.MAX_VALUE);
			
			sPool.put(fsize, pool);
		}
		
		ByteBuffer buffer = pool.obtain();
		onBufferRentOut(ExceptionHandler.getStackTraceString(new Throwable()), buffer);
		
		if(buffer == null) {
			Log.e(TAG, "obtain buffer from pool failed.");
		}
		notify(sMemInUsing,sMemTotal);
		Log.i(TAG, "obtain: sMemTotal=" + sMemTotal + " sMemInPool="+sMemInUsing);
		
		return buffer;
	}
	
	public static synchronized void recycle(ByteBuffer buffer) {
		if(buffer != null) {
			int size = buffer.capacity();
			ObjectPool<ByteBuffer> pool = sPool.get(size);
			if(pool != null) {
				if(pool.recycle(buffer)) {
					sMemInUsing -= size;
					onBufferBack(buffer);
				}
			}
		}
		
		Log.i(TAG, "recycle: sMemInPool="+sMemInUsing);
	}
	
	private static void onBufferRentOut(String user,ByteBuffer buffer) {
		Log.i(TAG, "onBufferRentOut " + user);
		if(buffer != null) {
			long id = getByteBufferId(buffer);
			MemInfo info = sMemInfo.get(id);
			if(info == null) {
				info = new MemInfo();
				info.buffer = buffer;
				sMemInfo.put(id, info);
			}
			info.owner = user;
			
			sInUsing.add(id);
		}
	}
	
	private static void onBufferBack(ByteBuffer buffer) {
		sInUsing.remove(getByteBufferId(buffer));
	}
	
	private static final long getByteBufferId(ByteBuffer buffer) {
		return (long)System.identityHashCode(buffer);
	}
	
	public static synchronized String dumpInfo() {
		StringBuilder sb = new StringBuilder(2048);
		sb.append("pool info: \n");
		long poolSize = 0;
		Iterator<Map.Entry<Integer,ObjectPool<ByteBuffer>>> itr = sPool.entrySet().iterator();
		while(itr.hasNext()) {
			Map.Entry<Integer,ObjectPool<ByteBuffer>> entry = itr.next();
			int bufferSize = entry.getKey();
			int bufferNum = entry.getValue().getPoolSize();
			sb.append(bufferSize).append(": ").append(bufferNum).append("\n");
			poolSize += (bufferSize*bufferNum);
		}
		
		sb.append("meminfo in using: \n");
		Iterator<Long> itr2 = sInUsing.iterator();
		Map<String,Long> usage = new HashMap<>();
		while(itr2.hasNext()) {
			long id = itr2.next();
			MemInfo memInfo = sMemInfo.get(id);
			ByteBuffer buffer = null;
			String owner = null;
			if(memInfo != null) {
				if(memInfo.owner == null) {					
					Log.i(TAG, "memInfo " + memInfo.owner);
				}
				owner = memInfo.owner;
				buffer = memInfo.buffer;
			}else {
				Log.i(TAG, "memInfo is null.");
			}
			if(owner == null) {
				owner = "unknown";
			}
			
			if(buffer == null) {
				Log.e(TAG, "buffer is NULL");
				continue;
			}
			
			Long size = usage.get(owner);
			if(size == null) {
				usage.put(owner, (long)buffer.capacity());
			}else {
				size+=buffer.capacity();
				usage.put(owner, size);
			}
		}
		
		Iterator<Map.Entry<String, Long>> itr3 = usage.entrySet().iterator();
		long inUsing = 0L;
		while(itr3.hasNext()) {
			Map.Entry<String,Long> entry = itr3.next();
			sb.append(entry.getKey()).append("\n").append(" >>>> hold >>>> ").append(entry.getValue()).append(" bytes\n");
			inUsing += entry.getValue();
		}
		
		sb.append(poolSize).append(" bytes in pool,").append(inUsing).append(" bytes in using,").append(" total: ").append(poolSize+inUsing).append(" bytes.");	
		
		return sb.toString();
	}
	
	public interface IMemInfoListener{
		void onMemoryInfo(long inUsing,long total);
	}
	
	private static class MemInfo{
		public String owner;
		public ByteBuffer buffer;
	}
}
