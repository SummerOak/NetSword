package com.chedifier.netsword.base;

import java.util.LinkedList;

public class ObjectPool<T> {
	private static final String TAG = "ObjectPool";
	private LinkedList<T> mPool;
	private IConstructor<T> mConstructor;
	private int mSize;
	
	public ObjectPool(IConstructor<T> constructor,int size) {
		mPool = new LinkedList<>();
		mConstructor = constructor;
		mSize = size;
	}

	public T obtain(Object... params) {
		if(mConstructor == null) {
			return null;
		}
		
		synchronized (mPool) {
			T e = null;
			if(mPool.isEmpty()) {
				e = mConstructor.newInstance(params);
//				Log.d(TAG,"obtain-create " + System.identityHashCode(e));
			}else{
				e = mPool.removeFirst();
				mConstructor.initialize(e,params);
//				Log.d(TAG,"obtain-reuse " + System.identityHashCode(e));
			}
			
			return e;
		}
	}
	
	public void recycle(T o) {
		if(o != null) {
			synchronized (mPool) {
				if(mPool.size() < mSize && !mPool.contains(o)) {
//					Log.d(TAG,"release " + System.identityHashCode(o));
					mPool.add(o);
				}
			}
		}
	}
	
	public static interface IConstructor<T>{
		T newInstance(Object... params);
		void initialize(T e,Object... params);
	}
	
}
