package com.proxibase.aircandi.utils;

import android.util.Log;

public class Logger {
	
	/*
	 * Logging levels
	 * 
	 * ERROR = 6
	 * WARN = 5
	 * INFO = 4
	 * DEBUG = 3
	 * VERBOSE = 2
	 */

	private static int	LOG_LEVEL	= Log.VERBOSE;

	/**
	 * Error
	 * 
	 * This level of logging should be used when something fatal has happened, i.e. something that will have
	 * user-visible consequences and won't be recoverable without explicitly deleting some data, uninstalling
	 * applications, wiping the data partitions or reflashing the entire phone (or worse). This level is always logged.
	 * Issues that justify some logging at the ERROR level are typically good candidates to be reported to a
	 * statistics-gathering server.
	 */
	static public void e(String tag, String task, String msgFormat) {
		if (LOG_LEVEL <= Log.ERROR) {
			Log.e(tag, msgFormat);
		}
	}

	static public void e(String tag, String task, String msgFormat, Throwable t) {
		if (LOG_LEVEL <= Log.ERROR) {
			Log.e(tag, msgFormat, t);
		}
	}

	/**
	 * Warning
	 * 
	 * This level of logging should used when something serious and unexpected happened, i.e. something that will have
	 * user-visible consequences but is likely to be recoverable without data loss by performing some explicit action,
	 * ranging from waiting or restarting an app all the way to re-downloading a new version of an application or
	 * rebooting the device. This level is always logged. Issues that justify some logging at the WARNING level might
	 * also be considered for reporting to a statistics-gathering server.
	 */
	static public void w(String tag, String task, String msgFormat) {
		if (LOG_LEVEL <= Log.WARN) {
			Log.w(tag, msgFormat);
		}
	}

	static public void w(String tag, String task, String msgFormat, Throwable t) {
		if (LOG_LEVEL <= Log.WARN) {
			Log.w(tag, msgFormat, t);
		}
	}

	/**
	 * Information
	 * 
	 * This level of logging should used be to note that something interesting to most people happened, i.e. when a
	 * situation is detected that is likely to have widespread impact, though isn't necessarily an error. Such a
	 * condition should only be logged by a module that reasonably believes that it is the most authoritative in that
	 * domain (to avoid duplicate logging by non-authoritative components). This level is always logged.
	 */
	static public void i(String tag, String task, String msgFormat) {
		if (LOG_LEVEL <= Log.INFO) {
			Log.i(tag, msgFormat);
		}
	}

	static public void i(String tag, String task, String msgFormat, Throwable t) {
		if (LOG_LEVEL <= Log.INFO) {
			Log.i(tag, msgFormat, t);
		}
	}

	/**
	 * Debug: Compiled in but stripped at runtime.
	 * 
	 * This level of logging should be used to further note what is happening on the device that could be relevant to
	 * investigate and debug unexpected behaviors. You should log only what is needed to gather enough information about
	 * what is going on about your component. If your debug logs are dominating the log then you probably should be
	 * using verbose logging.
	 */
	static public void d(String tag, String task, String msgFormat) {
		if (LOG_LEVEL <= Log.DEBUG) {
			Log.d(tag, msgFormat);
		}
	}

	static public void d(String tag, String task, String msgFormat, Throwable t) {
		if (LOG_LEVEL <= Log.DEBUG) {
			Log.d(tag, msgFormat, t);
		}
	}

	/**
	 * Verbose: Should never compiled into release version
	 * 
	 * This level of logging should be used for everything else. This level will only be logged on debug builds.
	 */
	static public void v(String tag, String task, String msgFormat) {
		if (LOG_LEVEL <= Log.VERBOSE) {
			Log.v(tag, msgFormat);
		}
	}

	static public void v(String tag, String task, String msgFormat, Throwable t) {
		if (LOG_LEVEL <= Log.VERBOSE) {
			Log.v(tag, msgFormat, t);
		}
	}
}
