package com.proxibase.aircandi.utils;

import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;

public class Exceptions {

	public static boolean Handle(Exception exception) {
		/* 
		 * For now we are re-throwing all exceptions so they get 
		 * handled by the top level uncaught exception handler. 
		 */
		throw new RuntimeException(exception);
	}
	
	public static boolean Handle(ProxibaseException exception) {
		throw new RuntimeException(exception);
	}
}