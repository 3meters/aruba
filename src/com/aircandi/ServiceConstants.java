// $codepro.audit.disable fileComment
package com.aircandi;

@SuppressWarnings("ucd")
public final class ServiceConstants {

	public static final int		TIMEOUT_SOCKET_QUERIES							= 30000;
	public static final int		TIMEOUT_SOCKET_UPDATES							= 30000;
	public static final int		TIMEOUT_CONNECTION								= 20000;
	public static final int		TIMEOUT_APPLINK_SEARCH							= 10000;
	public static final int		TIMEOUT_APPLINK_REFRESH							= 10000;
	public static final int		TIMEOUT_APPLINK_UPDATE							= 2000;

	public static String		WALLED_GARDEN_URI								= "http://clients3.google.com/generate_204";
	public static final int		WALLED_GARDEN_SOCKET_TIMEOUT_MS					= 5000;

	/*
	 * Used when trying to verify that a network connection is available. The retries
	 * are used to allow for the case where the connecting process is underway.
	 */
	public static int			CONNECT_TRIES									= 10;
	public static int			CONNECT_WAIT									= 500;

	/*
	 * When spotty connectivity causes timeouts, we retry using softer constraints.
	 * We do not attempt retries if the call involved an update/insert/delete.
	 */
	public static final int		MAX_BACKOFF_IN_MILLISECONDS						= 5 * 1000;
	public static final int		MAX_BACKOFF_RETRIES								= 6;

	public static final int		DEFAULT_MAX_CONNECTIONS							= 50;
	public static final int		DEFAULT_CONNECTIONS_PER_ROUTE					= 20;

	public static final String	INSERT_USER_SECRET								= "larissa";																																	//$NON-NLS-1$
	public static final String	ADMIN_USER_ID									= "us.000000.00000.000.000000";																												//$NON-NLS-1$
	public static final String	ANONYMOUS_USER_ID								= "us.000000.00000.000.111111";																												//$NON-NLS-1$

	public static final String	__URL_PROXIBASE_SERVICE							= "http://ariseditions.com:8080";																												// local																					// $codepro.audit.disable constantNamingConvention //$NON-NLS-1$
	public static final String	_URL_PROXIBASE_SERVICE							= "https://api.aircandi.com";																													// production																					//$NON-NLS-1$
	public static final String	URL_PROXIBASE_SERVICE							= "https://api.aircandi.com:444";																												// staging																									//$NON-NLS-1$

	public static final String	PATH_PROXIBASE_SERVICE_ASSETS_APPLINK_ICONS		= "/img/applinks/";																															//$NON-NLS-1$
	public static final String	PATH_PROXIBASE_SERVICE_ASSETS_CATEGORIES		= "/img/categories/";																															//$NON-NLS-1$

	public static final String	URL_PROXIBASE_SERVICE_REST						= URL_PROXIBASE_SERVICE + "/data/";																											//$NON-NLS-1$
	public static final String	URL_PROXIBASE_SERVICE_USER						= URL_PROXIBASE_SERVICE + "/user/";																											//$NON-NLS-1$
	public static final String	URL_PROXIBASE_SERVICE_ADMIN						= URL_PROXIBASE_SERVICE + "/admin/";																											//$NON-NLS-1$
	public static final String	URL_PROXIBASE_SERVICE_METHOD					= URL_PROXIBASE_SERVICE + "/do/";																												//$NON-NLS-1$
	public static final String	URL_PROXIBASE_SERVICE_PLACES					= URL_PROXIBASE_SERVICE + "/places/";																											//$NON-NLS-1$
	public static final String	URL_PROXIBASE_SERVICE_APPLINKS					= URL_PROXIBASE_SERVICE + "/applinks/";																										//$NON-NLS-1$
	public static final String	URL_PROXIBASE_SERVICE_AUTH						= URL_PROXIBASE_SERVICE + "/auth/";																											//$NON-NLS-1$
	public static final String	URL_PROXIBASE_SERVICE_ASSETS_APPLINK_ICONS		= URL_PROXIBASE_SERVICE + PATH_PROXIBASE_SERVICE_ASSETS_APPLINK_ICONS;																			//$NON-NLS-1$
	public static final String	URL_PROXIBASE_SERVICE_ASSETS_CATEGORIES			= URL_PROXIBASE_SERVICE + PATH_PROXIBASE_SERVICE_ASSETS_CATEGORIES;																			//$NON-NLS-1$

	public static final String	URL_PROXIBASE_MEDIA_IMAGES						= "http://aircandi-images.s3.amazonaws.com/";																									//$NON-NLS-1$
	public static final String	URL_PROXIBASE_SEARCH_IMAGES						= "https://api.datamarket.azure.com/Data.ashx/Bing/Search/v1/Image";																			//$NON-NLS-1$

	public static final String	USER_AGENT_MOBILE								= "Mozilla/5.0 (Linux; U; Android 2.1.3; en-us; Nexus S Build/GRK39F) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";	//$NON-NLS-1$
	public static final String	USER_AGENT_DESKTOP								= "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_3; en-us) AppleWebKit/533.16 (KHTML, like Gecko) Version/5.0 Safari/533.16";					//$NON-NLS-1$
	public static final String	USER_AGENT_NEXUS_S								= "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus S Build/GRK39F) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";	//$NON-NLS-1$

	public static final int		PROXIMITY_BEACON_COVERAGE						= 1;

	public static final int		PAGE_SIZE_APPLINKS								= 100;
	public static final int		PAGE_SIZE_CANDIGRAMS							= 30;
	public static final int		PAGE_SIZE_COMMENTS								= 30;
	public static final int		PAGE_SIZE_PICTURES								= 30;
	public static final int		PAGE_SIZE_PLACES								= 30;
	public static final int		PAGE_SIZE_USERS									= 30;
	public static final int		PAGE_SIZE_PLACES_MAP							= 50;

	public static final float	HTTP_STATUS_CODE_UNAUTHORIZED_CREDENTIALS		= 401.1f;
	public static final float	HTTP_STATUS_CODE_UNAUTHORIZED_SESSION_EXPIRED	= 401.2f;																																		// $codepro.audit.disable questionableName
	public static final float	HTTP_STATUS_CODE_UNAUTHORIZED_WHITELIST			= 401.4f;																																		// $codepro.audit.disable questionableName
	public static final float	HTTP_STATUS_CODE_UNAUTHORIZED_UNVERIFIED		= 401.5f;																																		// $codepro.audit.disable questionableName

	public static final float	HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE			= 403.1f;																																		// $codepro.audit.disable questionableName
	public static final float	HTTP_STATUS_CODE_FORBIDDEN_USER_PASSWORD_WEAK	= 403.21f;																																		// $codepro.audit.disable questionableName

	private ServiceConstants() {}; // $codepro.audit.disable emptyMethod
}
