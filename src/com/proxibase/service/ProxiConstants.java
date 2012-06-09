package com.proxibase.service;

public interface ProxiConstants {

	public static final int		TIMEOUT_SOCKET										= 30000;
	public static final int		TIMEOUT_CONNECTION									= 5000;
	public static final int		JSON_NOT_INT_PRIMITIVE								= 999999;
	public static final int		RESULT_OK											= -1;
	public static final int		RESULT_FAIL											= 0;
	public static final int		TWO_MINUTES											= 1000 * 60 * 2;
	public static final int		FIVE_MINUTES										= 1000 * 60 * 5;
	public static final boolean	MODE_DEBUG											= true;

	public static final String	URL_PROXIBASE_SERVICE								= "http://api.aircandi.com:8080/";
	public static final String	URL_PROXIBASE_SERVICE_METHOD						= "http://api.aircandi.com:8080/__do/";
	public static final String	_URL_PROXIBASE_SERVICE								= "https://api.proxibase.com/";
	public static final String	_URL_PROXIBASE_SERVICE_METHOD						= "https://api.proxibase.com/__do/";
	public static final String	URL_PROXIBASE_MEDIA									= "https://s3.amazonaws.com/";
	public static final String	APP_NAME											= "Proxibase";
	public static final String	USER_AGENT_MOBILE									= "Mozilla/5.0 (Linux; U; Android 2.1.3; en-us; Nexus S Build/GRK39F) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";
	public static final String	USER_AGENT_DESKTOP									= "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_3; en-us) AppleWebKit/533.16 (KHTML, like Gecko) Version/5.0 Safari/533.16";
	public static final String	USER_AGENT_NEXUS_S									= "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus S Build/GRK39F) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";

	public static final int		HTTP_STATUS_CODE_SUCCESS							= 200;
	public static final int		HTTP_STATUS_CODE_CREATED							= 201;
	public static final int		HTTP_STATUS_CODE_ACCEPTED							= 202;
	public static final int		HTTP_STATUS_CODE_UPDATED							= 204;
	public static final int		HTTP_STATUS_CODE_UNAUTHORIZED						= 401;
	public static final int		HTTP_STATUS_CODE_FORBIDDEN							= 403;
	public static final int		HTTP_STATUS_CODE_NOT_FOUND							= 404;
	public static final int		HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR				= 500;
	public static final int		HTTP_STATUS_CODE_SERVICE_TEMPORARILY_UNAVAILABLE	= 503;
	public static final int		HTTP_STATUS_CODE_GATEWAY_TIMEOUT					= 504;

}
