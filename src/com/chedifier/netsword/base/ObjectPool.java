package com.chedifier.netsword.base;

import java.util.LinkedList;

public class ObjectPool<T> {
	
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
			if(mPool.isEmpty()) {
				return mConstructor.newInstance(params);
			}
			
			T e = mPool.removeFirst();
			mConstructor.initialize(e,params);
			return e;
		}
	}
	
	public void release(T o) {
		if(o != null) {
			synchronized (mPool) {
				if(mPool.size() < mSize) {
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
