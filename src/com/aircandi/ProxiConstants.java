package com.aircandi;

public interface ProxiConstants {

	public static final int		TIMEOUT_SOCKET_QUERIES								= 50000;
	public static final int		TIMEOUT_SOCKET_UPDATES								= 30000;
	public static final int		TIMEOUT_CONNECTION									= 10000;
	public static final int		JSON_NOT_INT_PRIMITIVE								= 999999;
	public static final int		RESULT_OK											= -1;
	public static final int		RESULT_FAIL											= 0;
	public static final boolean	MODE_DEBUG											= true;

	public static final String	INSERT_USER_SECRET									= "larissa";

	public static final String	URL_PROXIBASE_SERVICE								= "http://ariseditions.com:8080/";
	public static final String	_URL_PROXIBASE_SERVICE								= "https://aircandi.com:643/";
	public static final String	ADMIN_USER_ID										= "0001.000000.00000.000.000000";

	public static final String	URL_PROXIBASE_SERVICE_REST							= URL_PROXIBASE_SERVICE + "data/";
	public static final String	URL_PROXIBASE_SERVICE_USER							= URL_PROXIBASE_SERVICE + "user/";
	public static final String	URL_PROXIBASE_SERVICE_ADMIN							= URL_PROXIBASE_SERVICE + "admin/";
	public static final String	URL_PROXIBASE_SERVICE_METHOD						= URL_PROXIBASE_SERVICE + "do/";
	public static final String	URL_PROXIBASE_SERVICE_AUTH							= URL_PROXIBASE_SERVICE + "auth/";

	public static final String	URL_PROXIBASE_MEDIA_IMAGES							= "https://aircandi-images.s3.amazonaws.com/";
	public static final String	URL_PROXIBASE_SEARCH_IMAGES							= "https://api.datamarket.azure.com/Data.ashx/Bing/Search/v1/Image";
	public static final String	URL_PROXIBASE_SEARCH_VENUES							= "https://api.foursquare.com/v2/venues/search?client_id=MDUDBL5H3OZ5LEGONM3CZWQFBYDNEA5AYKPGFBHUOLQ4QHF4&client_secret=SAAG02RBJ3FEMXJUXIU2NZ1O3YN5PRWJ0JA31HP2UECXEIXD&v=201209274";

	public static final String	USER_AGENT_MOBILE									= "Mozilla/5.0 (Linux; U; Android 2.1.3; en-us; Nexus S Build/GRK39F) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";
	public static final String	USER_AGENT_DESKTOP									= "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_3; en-us) AppleWebKit/533.16 (KHTML, like Gecko) Version/5.0 Safari/533.16";
	public static final String	USER_AGENT_NEXUS_S									= "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus S Build/GRK39F) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";

	public static final int		RADAR_ENTITY_LIMIT									= 50;
	public static final int		RADAR_PLACES_LIMIT									= 30;
	public static final int		RADAR_CHILDENTITY_LIMIT								= 20;
	public static final int		RADAR_COMMENT_LIMIT									= 50;

	public static final float	HTTP_STATUS_CODE_UNAUTHORIZED_CREDENTIALS			= 401.1f;
	public static final float	HTTP_STATUS_CODE_UNAUTHORIZED_SESSION_EXPIRED		= 401.2f;
	public static final float	HTTP_STATUS_CODE_FORBIDDEN_USER_EMAIL_NOT_UNIQUE	= 403.1f;
	public static final float	HTTP_STATUS_CODE_FORBIDDEN_USER_PASSWORD_WEAK		= 403.21f;
}
