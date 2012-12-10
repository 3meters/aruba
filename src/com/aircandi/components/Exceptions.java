package com.aircandi.components;

import com.aircandi.Aircandi;
import com.aircandi.service.ProxibaseServiceException;

public class Exceptions {

	public static boolean Handle(Exception exception) {
		/* 
		 * For now we are re-throwing all exceptions so they get 
		 * handled by the top level uncaught exception handler. 
		 */
		Aircandi.getInstance().setLaunchedNormally(false);
		throw new RuntimeException(exception);
	}
	
	public static boolean Handle(ProxibaseServiceException exception) {
		Aircandi.getInstance().setLaunchedNormally(false);
		throw new RuntimeException(exception);
	}
}