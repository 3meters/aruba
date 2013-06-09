package com.aircandi.components;

import android.util.Log;

import com.aircandi.Constants;

@SuppressWarnings("ucd")
public class Logger {

	/*
	 * Logging levels and guidance
	 * 
	 * ERROR = 6
	 * A problem that has crashed the applicaion.
	 * 
	 * WARN = 5
	 * When a condition exists that could be a concern but we keep running.
	 * 
	 * INFO = 4
	 * Reporting that is part of normal operation.
	 * 
	 * DEBUG = 3
	 * Information that is needed to track down bugs either locally or via remote crash reports.
	 * 
	 * VERBOSE = 2
	 * Chatty stuff that is helpful in the logs while developing but will never used in release.
	 */

	private static final boolean	INCLUDE_MODULE	= true;

	private static enum LogLevel {
		Error,
		Warning,
		Info,
		Debug,
		Verbose
	}

	private static void Log(LogLevel logLevel, Object taskContext, String msgFormat) {
		Log(logLevel, taskContext, msgFormat, null);
	}

	private static void Log(LogLevel logLevel, Object taskContext, String msgFormat, Throwable t) {

		String task = "";
		if (INCLUDE_MODULE && taskContext != null) {
			task = "[" + Thread.currentThread().getName() + "]: " + taskContext.getClass().getSimpleName() + ": ";
		}
		if (logLevel == LogLevel.Error) {
			Log.e(Constants.APP_NAME, task + msgFormat, t);
		}
		else if (logLevel == LogLevel.Warning) {
			Log.w(Constants.APP_NAME, task + msgFormat, t);
		}
		else if (logLevel == LogLevel.Info) {
			Log.i(Constants.APP_NAME, task + msgFormat, t);
		}
		else if (logLevel == LogLevel.Debug) {
			Log.d(Constants.APP_NAME, task + msgFormat, t);
		}
		else if (logLevel == LogLevel.Verbose) {
			Log.v(Constants.APP_NAME, task + msgFormat, t);
		}
	}

	/**
	 * Error
	 * This level of logging should be used when something fatal has happened, i.e. something that will have
	 * user-visible consequences and won't be recoverable without explicitly deleting some data, uninstalling
	 * applications, wiping the data partitions or reflashing the entire phone (or worse). This level is always logged.
	 * Issues that justify some logging at the ERROR level are typically good candidates to be reported to a
	 * statistics-gathering server.
	 */
	public static void e(Object taskContext, String msgFormat) {
		if (Constants.LOG_LEVEL <= Log.ERROR) {
			Log(LogLevel.Error, taskContext, msgFormat);
		}
	}

	public static void e(Object taskContext, String msgFormat, Throwable t) {
		if (Constants.LOG_LEVEL <= Log.ERROR) {
			Log(LogLevel.Error, taskContext, msgFormat, t);
		}
	}

	/**
	 * Warning
	 * This level of logging should used when something serious and unexpected happened, i.e. something that will have
	 * user-visible consequences but is likely to be recoverable without data loss by performing some explicit action,
	 * ranging from waiting or restarting an app all the way to re-downloading a new version of an application or
	 * rebooting the device. This level is always logged. Issues that justify some logging at the WARNING level might
	 * also be considered for reporting to a statistics-gathering server.
	 */
	public static void w(Object taskContext, String msgFormat) {
		if (Constants.LOG_LEVEL <= Log.WARN) {
			Log(LogLevel.Warning, taskContext, msgFormat);
		}
	}

	public static void w(Object taskContext, String msgFormat, Throwable t) {
		if (Constants.LOG_LEVEL <= Log.WARN) {
			Log(LogLevel.Warning, taskContext, msgFormat, t);
		}
	}

	/**
	 * Information
	 * This level of logging should used be to note that something interesting to most people happened, i.e. when a
	 * situation is detected that is likely to have widespread impact, though isn't necessarily an error. Such a
	 * condition should only be logged by a module that reasonably believes that it is the most authoritative in that
	 * domain (to avoid duplicate logging by non-authoritative components). This level is always logged.
	 */
	public static void i(Object taskContext, String msgFormat) {
		if (Constants.LOG_LEVEL <= Log.INFO) {
			Log(LogLevel.Info, taskContext, msgFormat);
		}
	}

	public static void i(Object taskContext, String msgFormat, Throwable t) {
		if (Constants.LOG_LEVEL <= Log.INFO) {
			Log(LogLevel.Info, taskContext, msgFormat, t);
		}
	}

	/**
	 * Debug: Compiled in but stripped at runtime.
	 * This level of logging should be used to further note what is happening on the device that could be relevant to
	 * investigate and debug unexpected behaviors. You should log only what is needed to gather enough information about
	 * what is going on about your component. If your debug logs are dominating the log then you probably should be
	 * using verbose logging.
	 */
	public static void d(Object taskContext, String msgFormat) {
		if (Constants.LOG_LEVEL <= Log.DEBUG) {
			Log(LogLevel.Debug, taskContext, msgFormat);
		}
	}

	public static void d(Object taskContext, String msgFormat, Throwable t) {
		if (Constants.LOG_LEVEL <= Log.DEBUG) {
			Log(LogLevel.Debug, taskContext, msgFormat, t);
		}
	}

	/**
	 * Verbose: Should never compiled into release version
	 * This level of logging should be used for everything else. This level will only be logged on debug builds.
	 */
	public static void v(Object taskContext, String msgFormat) {
		if (Constants.LOG_LEVEL <= Log.VERBOSE) {
			Log(LogLevel.Verbose, taskContext, msgFormat);
		}
	}

	public static void v(Object taskContext, String msgFormat, Throwable t) {
		if (Constants.LOG_LEVEL <= Log.VERBOSE) {
			Log(LogLevel.Verbose, taskContext, msgFormat, t);
		}
	}
}
