package com.proxibase.aircandi.components;

import com.proxibase.aircandi.Aircandi;
import com.proxibase.sdk.android.proxi.service.ProxibaseServiceException;

public class Exceptions {

	public static boolean Handle(Exception exception) {
		/* 
		 * For now we are re-throwing all exceptions so they get 
		 * handled by the top level uncaught exception handler. 
		 */
		Aircandi.getInstance().setLaunchedFromRadar(false);
		throw new RuntimeException(exception);
	}
	
	public static boolean Handle(ProxibaseServiceException exception) {
		Aircandi.getInstance().setLaunchedFromRadar(false);
		throw new RuntimeException(exception);
	}
}