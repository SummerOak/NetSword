package com.chedifier.netsword.base;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.chedifier.netsword.base.ObjectPool.IConstructor;

public class DateUtils {

	private static ObjectPool<SimpleDateFormat> sDateFormatPool = new ObjectPool<SimpleDateFormat>(new IConstructor<SimpleDateFormat>() {

		@Override
		public SimpleDateFormat newInstance(Object... params) {
			return new SimpleDateFormat("yyyyMMdd-HH-mm-ss-SS");
		}

		@Override
		public void initialize(SimpleDateFormat e, Object... params) {
		}
	}, 10);
	
	private static ObjectPool<Date> sDatePool = new ObjectPool<Date>(new IConstructor<Date>() {

		@Override
		public Date newInstance(Object... params) {
			return new Date((Long)params[0]);
		}

		@Override
		public void initialize(Date e, Object... params) {
			e.setTime((Long)params[0]);
		}
	}, 10);
	
	public static final String getCurrentDate() {
		return getDate(System.currentTimeMillis());
	}
	
	public static final String getDate(long time) {
		Date date = sDatePool.obtain(time);
		SimpleDateFormat format = sDateFormatPool.obtain();
		String ret = format.format(date);
		
		System.out.println("jjjdj time: " + time + " ret "+ ret);
		sDatePool.recycle(date);
		sDateFormatPool.recycle(format);
		return ret;
	}
}
