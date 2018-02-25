package com.chedifier.netsword.base;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

	private static SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMdd-HH-mm-ss-SS");
	private static Date sDate = new Date();
	
	public static final String getCurrentDate() {
		return getDate(System.currentTimeMillis());
	}
	
	public static final String getDate(long time) {
		sDate.setTime(time);
		return sDateFormat.format(sDate);
	}
}
