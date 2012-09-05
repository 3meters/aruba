package com.aircandi.components;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtils {

	public static final String	DATE_NOW_FORMAT				= "yyyy-MM-dd HH:mm:ss";
	public static final String	DATE_NOW_FORMAT_FILENAME	= "yyyyMMdd_HHmmss";
	public static final String	DATE_FORMAT_TIME_SINCE		= "MMM d";
	public static final String	TIME_FORMAT_TIME_SINCE		= "h:mm";
	public static final String	AMPM_FORMAT_TIME_SINCE		= "a";

	public static String nowString() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_NOW_FORMAT);
		return sdf.format(cal.getTime());
	}

	public static String nowString(String pattern) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		return sdf.format(cal.getTime());
	}

	public static Date nowDate() {
		Calendar cal = Calendar.getInstance();
		return cal.getTime();
	}

	public static Date wcfToDate(String wcfDate) {
		String JSONDateToMilliseconds = "\\/(Date\\((.*?)(\\+.*)?\\))\\/";
		Pattern pattern = Pattern.compile(JSONDateToMilliseconds);
		Matcher matcher = pattern.matcher(wcfDate);
		String result = matcher.replaceAll("$2");
		return new Date(Long.valueOf(result));
	}

	public static int intervalInSeconds(Date dateOld, Date dateNew) {
		Long diff = dateNew.getTime() - dateOld.getTime();
		int seconds = (int) (diff / 1000);
		return seconds;
	}

	/* Change a Date to GMT */
	public static Date toGMT(Date date) {
		return changeTimeZone(date, TimeZone.getTimeZone("GMT"));
	}

	/* Change a date to GMT from a given timezone */
	public static Date toGmtFromZone(Date date, String fromZone) {
		TimeZone pst = TimeZone.getTimeZone(fromZone);
		return new Date(date.getTime() - pst.getRawOffset());
	}

	/* Change a date in another timezone */
	public static Date changeTimeZone(Date date, TimeZone zone) {
		Calendar first = Calendar.getInstance(zone);
		first.setTimeInMillis(date.getTime());

		Calendar output = Calendar.getInstance();
		output.set(Calendar.YEAR, first.get(Calendar.YEAR));
		output.set(Calendar.MONTH, first.get(Calendar.MONTH));
		output.set(Calendar.DAY_OF_MONTH, first.get(Calendar.DAY_OF_MONTH));
		output.set(Calendar.HOUR_OF_DAY, first.get(Calendar.HOUR_OF_DAY));
		output.set(Calendar.MINUTE, first.get(Calendar.MINUTE));
		output.set(Calendar.SECOND, first.get(Calendar.SECOND));
		output.set(Calendar.MILLISECOND, first.get(Calendar.MILLISECOND));

		return output.getTime();
	}

	public static String timeSince(Long dateOldMillis, Long dateNewMillis) {

		Long dateNewLong = dateNewMillis;
		Long dateOldLong = dateOldMillis;

		Date dateNew = new Date(dateNewLong);
		Date dateOld = new Date(dateOldLong);
		
		Long diff = dateNew.getTime() - dateOld.getTime();

		if (diff <= 0) {
			return "just now";
		}
		int seconds = (int) (diff / 1000);
		int minutes = (int) ((diff / 1000) / 60);
		int hours = (int) ((diff / 1000) / (60 * 60));
		int days = (int) ((diff / 1000) / (60 * 60 * 24));
		@SuppressWarnings("unused")
		int hoursPart = hours - (days * 24);
		@SuppressWarnings("unused")
		int minutesPart = minutes - (hours * 60);

		String interval = "";
		if (days >= 1) {
			SimpleDateFormat datePart = new SimpleDateFormat(DATE_FORMAT_TIME_SINCE);
			SimpleDateFormat timePart = new SimpleDateFormat(TIME_FORMAT_TIME_SINCE);
			SimpleDateFormat ampmPart = new SimpleDateFormat(AMPM_FORMAT_TIME_SINCE);
			return datePart.format(dateOld.getTime()) + " at "
					+ timePart.format(dateOld.getTime())
					+ ampmPart.format(dateOld.getTime()).toLowerCase();
		}
		else if (hours == 1) /* x hours x minutes ago */
		{
			interval = "1 hour ago";
		}
		else if (hours > 1) /* x hours x minutes ago */
		{
			interval = String.valueOf(hours) + " hours ago";
		}
		else if (minutes == 1) /* x hours x minutes ago */
		{
			interval = "1 minute ago";
		}
		else if (minutes > 1) /* x hours x minutes ago */
		{
			interval = String.valueOf(minutes) + " minutes ago";
		}
		else if (seconds == 1) /* 1 second ago */
		{
			interval = "1 second ago";
		}
		else if (seconds > 1) /* x hours x minutes ago */
		{
			interval = String.valueOf(seconds) + " seconds ago";
		}
		return interval;
	}

	public static String intervalSince(Date dateOld, Date dateNew) {
		Long diff = dateNew.getTime() - dateOld.getTime();
		int seconds = (int) (diff / 1000);
		int minutes = (int) ((diff / 1000) / 60);
		int hours = (int) ((diff / 1000) / (60 * 60));
		int days = (int) ((diff / 1000) / (60 * 60 * 24));
		int hoursPart = hours - (days * 24);
		@SuppressWarnings("unused")
		int minutesPart = minutes - (hours * 60);

		String interval = "";
		if (days >= 2) {
			interval += String.valueOf(days) + " days ago";
		}
		else if (days >= 1) /* x days ago 1 day and x hour ago */
		{
			interval += "1 day";
			if (hoursPart == 1)
				interval += " " + String.valueOf(hoursPart) + " hour";
			else if (hoursPart >= 2)
				interval += " " + String.valueOf(hoursPart) + " hours";
			interval += " ago";
		}
		else if (hours == 1) /* x hours x minutes ago */
		{
			interval = "1 hour ago";
		}
		else if (hours > 1) /* x hours x minutes ago */
		{
			interval = String.valueOf(hours) + " hours ago";
		}
		else if (minutes == 1) /* x hours x minutes ago */
		{
			interval = "1 minute ago";
		}
		else if (minutes > 1) /* x hours x minutes ago */
		{
			interval = String.valueOf(minutes) + " minutes ago";
		}
		else if (seconds == 1) /* 1 second ago */
		{
			interval = "1 second ago";
		}
		else if (seconds > 1) /* x hours x minutes ago */
		{
			interval = String.valueOf(seconds) + " seconds ago";
		}
		return interval;
	}
}
