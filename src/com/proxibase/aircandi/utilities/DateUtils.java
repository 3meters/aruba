package com.proxibase.aircandi.utilities;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtils
{
	public static final String	DATE_FORMAT_NOW	= "yyyy-MM-dd HH:mm:ss";

	public static String nowString()
	{
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(cal.getTime());
	}

	public static Date nowDate()
	{
		Calendar cal = Calendar.getInstance();
		return cal.getTime();
	}

	public static Date wcfToDate(String wcfDate)
	{
		String JSONDateToMilliseconds = "\\/(Date\\((.*?)(\\+.*)?\\))\\/";
		Pattern pattern = Pattern.compile(JSONDateToMilliseconds);
		Matcher matcher = pattern.matcher(wcfDate);
		String result = matcher.replaceAll("$2");
		return new Date(new Long(result));
	}
	
	public static int intervalInSeconds(Date dateOld, Date dateNew)
	{
		Long diff = dateNew.getTime() - dateOld.getTime();
		int seconds = (int) (diff / 1000);
		return seconds;
	}

	public static String intervalSince(Date dateOld, Date dateNew)
	{
		Long diff = dateNew.getTime() - dateOld.getTime();
		int minutes = (int) ((diff / 1000) / 60);
		int hours = (int) ((diff / 1000) / (60 * 60));
		int days = (int) ((diff / 1000) / (60 * 60 * 24));
		int hoursPart = hours - (days * 24);
		@SuppressWarnings("unused")
		int minutesPart = minutes - (hours * 60);

		String interval = "";
		if (days >= 2)
			interval += String.valueOf(days) + " days ago";
		else if (days >= 1) // x days ago 1 day and x hour ago
		{
			interval += "1 day";
			if (hoursPart == 1)
				interval += " " + String.valueOf(hoursPart) + " hour";
			else if (hoursPart >= 2)
				interval += " " + String.valueOf(hoursPart) + " hours";
			interval += " ago";
		}
		else if (hours == 1) // x hours x minutes ago
		{
			interval = "1 hour ago";
		}
		else if (hours > 1) // x hours x minutes ago
		{
			interval = String.valueOf(hours) + " hours ago";
		}
		else if (minutes == 1) // x hours x minutes ago
		{
			interval = "1 minute ago";
		}
		else if (minutes > 1) // x hours x minutes ago
		{
			interval = String.valueOf(minutes) + " minutes ago";
		}
		return interval;
	}
}
