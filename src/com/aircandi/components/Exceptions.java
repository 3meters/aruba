package com.aircandi.components;


public class Exceptions {

	public static boolean handle(Exception exception) {
		/* 
		 * For now we are re-throwing all exceptions so they get 
		 * handled by the top level uncaught exception handler. 
		 */
		throw new RuntimeException(exception);
	}
}