// $codepro.audit.disable fileComment
package com.aircandi;

@SuppressWarnings("ucd")
public final class ProxiConstants {

	public static final int		TIMEOUT_SOCKET_QUERIES								= 50000;
	public static final int		TIMEOUT_SOCKET_UPDATES								= 30000;
	public static final int		TIMEOUT_CONNECTION									= 10000;

	public static final String	INSERT_USER_SECRET									= "larissa";																																	//$NON-NLS-1$

	public static final String	URL_PROXIBASE_SERVICE								= "http://ariseditions.com:8080";																												// $codepro.audit.disable constantNamingConvention //$NON-NLS-1$
	public static final String	_URL_PROXIBASE_SERVICE								= "https://api.aircandi.com";																													//$NON-NLS-1$
	public static final String	ADMIN_USER_ID										= "0001.000000.00000.000.000000";																												//$NON-NLS-1$

	public static final String	URL_PROXIBASE_SERVICE_REST							= URL_PROXIBASE_SERVICE + "/data/";																												//$NON-NLS-1$
	public static final String	URL_PROXIBASE_SERVICE_USER							= URL_PROXIBASE_SERVICE + "/user/";																												//$NON-NLS-1$
	public static final String	URL_PROXIBASE_SERVICE_ADMIN							= URL_PROXIBASE_SERVICE + "/admin/";																											//$NON-NLS-1$
	public static final String	URL_PROXIBASE_SERVICE_METHOD						= URL_PROXIBASE_SERVICE + "/do/";																												//$NON-NLS-1$
	public static final String	URL_PROXIBASE_SERVICE_AUTH							= URL_PROXIBASE_SERVICE + "/auth/";																												//$NON-NLS-1$
	public static final String	URL_PROXIBASE_SERVICE_ASSETS_SOURCE_ICONS			= URL_PROXIBASE_SERVICE + "/img/sources/";																										//$NON-NLS-1$
	public static final String	URL_PROXIBASE_SERVICE_ASSETS_CATEGORIES				= URL_PROXIBASE_SERVICE + "/img/categories/foursquare/";																						//$NON-NLS-1$

	public static final String	URL_PROXIBASE_MEDIA_IMAGES							= "http://aircandi-images.s3.amazonaws.com/";																									//$NON-NLS-1$
	public static final String	URL_PROXIBASE_SEARCH_IMAGES							= "https://api.datamarket.azure.com/Data.ashx/Bing/Search/v1/Image";																			//$NON-NLS-1$

	public static final String	USER_AGENT_MOBILE									= "Mozilla/5.0 (Linux; U; Android 2.1.3; en-us; Nexus S Build/GRK39F) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";	//$NON-NLS-1$
	public static final String	USER_AGENT_DESKTOP									= "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_3; en-us) AppleWebKit/533.16 (KHTML, like Gecko) Version/5.0 Safari/533.16";					//$NON-NLS-1$
	public static final String	USER_AGENT_NEXUS_S									= "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus S Build/GRK39F) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";	//$NON-NLS-1$

	public static final int		RADAR_ENTITY_LIMIT									= 50;
	public static final int		RADAR_PLACES_LIMIT									= 50;
	public static final int		RADAR_CHILDENTITY_LIMIT								= 20;
	public static final int		RADAR_COMMENT_LIMIT									= 50;
	public static final int		SOURCE_SUGGESTIONS_TIMEOUT							= 2000;

	public static final float	HTTP_STATUS_CODE_UNAUTHORIZED_CREDENTIALS			= 401.1f;
	public static final float	HTTP_STATUS_CODE_UNAUTHORIZED_SESSION_EXPIRED		= 401.2f;																																		// $codepro.audit.disable questionableName
	public static final float	HTTP_STATUS_CODE_FORBIDDEN_USER_EMAIL_NOT_UNIQUE	= 403.1f;																																		// $codepro.audit.disable questionableName
	public static final float	HTTP_STATUS_CODE_FORBIDDEN_USER_PASSWORD_WEAK		= 403.21f;																																		// $codepro.audit.disable questionableName

	private ProxiConstants() {}; // $codepro.audit.disable emptyMethod
}
