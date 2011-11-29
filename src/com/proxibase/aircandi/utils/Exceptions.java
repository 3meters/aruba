package com.proxibase.aircandi.utils;

import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;

public class Exceptions {

	public static void Handle(Exception exception) {
		exception.printStackTrace();
	}
	
	public static void Handle(ProxibaseException exception) {
		exception.printStackTrace();
	}
}