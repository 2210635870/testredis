package org.oham.testredis.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.util.StringUtils;

public class DateUtil {

	public static String dateFormat = "yyyy-MM-dd";
	public static String datetimeFormat = "yyyy-MM-dd HH:mm:ss";
	
	
	public static String date2String(Date date, String fmt) {
		if( date == null ) {
			return null;
		}
		
		return new SimpleDateFormat(fmt).format(date);
	}
	
	public static Date string2Date(String dateStr , String fmt) {
		if( StringUtils.isEmpty(dateStr) ) {
			return null;
		}
		
		Date result = null;
		try {
			result = new SimpleDateFormat(fmt).parse(dateStr);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		} 
		
		return result;
	}
}
