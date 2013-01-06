package com.aircandi;

import android.app.AlarmManager;
import android.graphics.Bitmap.Config;
import android.util.Log;

public interface CandiConstants {
	/*
	 * TODO Check these when deploying the app.
	 */
	public static boolean		DEVELOPER_MODE							= false;
	public static final boolean	DEBUG_TRACE								= false;
	public static final int		LOG_LEVEL								= Log.VERBOSE;
	public static final boolean	TRACKING_ENABLED						= true;

	public static final String	APP_NAME								= "Aircandi";
	public static final String	GOOGLE_API_KEY_RELEASE					= "059_3gpXU_DPYyT-FzFkprl7kk2TvDhm_6mKWLQ";
	public static final String	GOOGLE_API_KEY_DEBUG					= "059_3gpXU_DM8qW_gRO68TJul5KWUSUzIE3gQlw";

	/* Activity parameters */
	public static final String	EXTRA_PARENT_ENTITY_ID					= "com.aircandi.EXTRA_PARENT_ENTITY_ID";
	public static final String	EXTRA_BEACON_ID							= "com.aircandi.EXTRA_BEACON_ID";
	public static final String	EXTRA_ENTITY_ID							= "com.aircandi.EXTRA_ENTITY_ID";
	public static final String	EXTRA_ENTITY_IS_ROOT					= "com.aircandi.EXTRA_ENTITY_IS_ROOT";
	public static final String	EXTRA_ENTITY_TYPE						= "com.aircandi.EXTRA_ENTITY_TYPE";
	public static final String	EXTRA_ENTITY_LOCATION					= "com.aircandi.EXTRA_ENTITY_LOCATION";
	public static final String	EXTRA_USER_ID							= "com.aircandi.EXTRA_USER_ID";
	public static final String	EXTRA_URI								= "com.aircandi.EXTRA_URI";
	public static final String	EXTRA_URI_TITLE							= "com.aircandi.EXTRA_URI_TITLE";
	public static final String	EXTRA_URI_DESCRIPTION					= "com.aircandi.EXTRA_URI_DESCRIPTION";
	public static final String	EXTRA_MESSAGE							= "com.aircandi.EXTRA_MESSAGE";
	public static final String	EXTRA_ADDRESS							= "com.aircandi.EXTRA_ADDRESS";
	public static final String	EXTRA_CATEGORY							= "com.aircandi.EXTRA_CATEGORY";
	public static final String	EXTRA_PHONE								= "com.aircandi.EXTRA_PHONE";
	public static final String	EXTRA_ENTITY_TREE						= "com.aircandi.EXTRA_ENTITY_TREE";
	public static final String	EXTRA_COLLECTION_ID						= "com.aircandi.EXTRA_COLLECTION_ID";
	public static final String	EXTRA_STRING_ID							= "com.aircandi.EXTRA_STRING_ID";
	public static final String	EXTRA_VERIFY_URI						= "com.aircandi.EXTRA_VERIFY_URI";
	public static final String	EXTRA_COMMAND_TYPE						= "com.aircandi.EXTRA_COMMAND_TYPE";
	public static final String	EXTRA_NAVIGATION_TOP					= "com.aircandi.EXTRA_NAVIGATION_TOP";
	public static final String	EXTRA_SEARCH_PHRASE						= "com.aircandi.EXTRA_SEARCH_PHRASE";
	public static final String	EXTRA_AS_PICKER							= "com.aircandi.EXTRA_AS_PICKER";
	public static final String	EXTRA_LIST_TYPE							= "com.aircandi.EXTRA_LIST_TARGET";
	public static final String	EXTRA_PICTURE_SOURCE					= "com.aircandi.EXTRA_PICTURE_SOURCE";
	public static final String	EXTRA_UPSIZE_SYNTHETIC					= "com.aircandi.EXTRA_UPSIZE_SYNTHETIC";

	public static final int		MILLS_PER_SECOND						= 1000;
	public static final int		TIME_ONE_SECOND							= MILLS_PER_SECOND * 1;
	public static final int		TIME_TEN_SECONDS						= MILLS_PER_SECOND * 10;
	public static final int		TIME_TWENTY_SECONDS						= MILLS_PER_SECOND * 20;
	public static final int		TIME_THIRTY_SECONDS						= MILLS_PER_SECOND * 30;
	public static final int		TIME_ONE_MINUTE							= MILLS_PER_SECOND * 60;
	public static final int		TIME_TWO_MINUTES						= MILLS_PER_SECOND * 60 * 2;
	public static final int		TIME_FIVE_MINUTES						= MILLS_PER_SECOND * 60 * 5;
	public static final int		TIME_FIFTEEN_MINUTES					= MILLS_PER_SECOND * 60 * 15;
	public static final int		TIME_THIRTY_MINUTES						= MILLS_PER_SECOND * 60 * 30;
	public static final int		TIME_SIXTY_MINUTES						= MILLS_PER_SECOND * 60 * 60;

	/* Wifi scanning */
	public static final int		INTERVAL_SCAN_RADAR						= TIME_ONE_MINUTE;
	public static final int		INTERVAL_SCAN_EDITING					= TIME_TWENTY_SECONDS;
	public static final int		INTERVAL_SCAN_TUNING					= TIME_TWENTY_SECONDS;

	public static final int		LOCATION_POLLING_INTERVAL				= TIME_FIVE_MINUTES;																		// 5 minutes
	public static final int		LOCATION_POLLING_TIMEOUT				= TIME_ONE_MINUTE;
	public static final int		INTERVAL_UPDATE_CHECK					= TIME_SIXTY_MINUTES;

	public static final int		MAX_Y_OVERSCROLL_DISTANCE				= 50;

	public static final int		RADAR_BEACON_SIGNAL_BUCKET_SIZE			= 1;
	public static final int		RADAR_SCAN_MISS_MAX						= 1;
	public static final int		RADAR_BEACON_INDICATOR_CAUTION			= -80;

	public static final float	CANDI_VIEW_HIGHLIGHT_THICKNESS			= 8.0f;
	public static final float	CANDI_VIEW_SCALE						= 1.0f;
	public static final int		CANDI_VIEW_WIDTH						= 250;
	public static final int		CANDI_VIEW_SPACING_VERTICAL				= 30;
	public static final int		CANDI_VIEW_SPACING_HORIZONTAL			= 15;
	public static final int		CANDI_VIEW_BODY_HEIGHT					= 250;
	public static final int		CANDI_VIEW_TITLE_HEIGHT					= 100;
	public static final int		CANDI_VIEW_TITLE_LINES					= 2;
	public static final int		CANDI_VIEW_REFLECTION_HEIGHT			= 125;
	public static final int		CANDI_VIEW_REFLECTION_GAP				= 1;
	public static final int		CANDI_VIEW_TITLE_SPACER_HEIGHT			= 2;
	public static final int		CANDI_VIEW_FONT_SIZE					= 30;																						// pixels
	public static final int		CANDI_VIEW_HEIGHT						= CANDI_VIEW_BODY_HEIGHT
																				+ CANDI_VIEW_TITLE_HEIGHT
																				+ CANDI_VIEW_TITLE_SPACER_HEIGHT;

	public static final int		CANDI_VIEW_ZOOMED_PADDING				= 10;
	public static final int		CANDI_VIEW_BADGE_WIDTH					= 100;

	public static final int		MAP_VIEW_FONT_SIZE						= 20;
	public static final int		MAP_VIEW_TITLE_MARGIN					= 6;
	public static final int		MAP_VIEW_MARKER_HEIGHT					= 50;
	public static final int		MAP_VIEW_TITLE_LENGTH_MAX				= 150;

	/* Dimensions at mdpi */
	public static final int		ANDROID_STATUSBAR_HEIGHT				= 25;
	public static final int		ANDROID_ACTIONBAR_HEIGHT				= 48;

	public static final int		SWIPE_MAX_OFF_PATH						= 500;
	public static final float	SWIPE_SMALL_FLING						= 100;
	public static final long	TOUCH_MAX_TIME							= 500;

	public static final int		DIALOG_INSTALL_ID						= 1;
	public static final int		DIALOG_WIFIENABLE_ID					= 2;
	public static final int		DIALOG_NEW_CANDI_ID						= 3;

	public static final int		TABS_PRIMARY_ID							= 1;
	public static final int		TABS_PROFILE_FORM_ID					= 2;
	public static final int		TABS_ENTITY_FORM_ID						= 3;
	public static final int		TABS_CANDI_PICKER_ID					= 4;
	public static final int		TABS_LINK_PICKER_ID						= 5;
	public static final int		TABS_USER_FORM_ID						= 6;

	public static final int		VERTEX_INDEX_X							= 0;
	public static final int		VERTEX_INDEX_Y							= 1;

	public static final String	IMAGE_CAPTURE_PATH						= "/aircandi/images/";

	public static final int		IMAGE_MEMORY_BYTES_MAX					= 4096000;
	public static final int		IMAGE_MEMORY_CACHE_WIDTH_MAX			= 100;																						// (100x100x4) + 10000 extra
	public static final Config	IMAGE_CONFIG_DEFAULT					= Config.ARGB_8888;
	public static final int		IMAGE_WIDTH_SMALL						= 60;
	public static final int		IMAGE_WIDTH_DEFAULT						= 250;
	public static final int		IMAGE_WIDTH_MAXIMUM						= 1024;
	public static final int		IMAGE_WIDTH_ORIGINAL					= -1;

	public static final boolean	LINK_JAVASCRIPT_ENABLED					= false;
	public static final boolean	LINK_ZOOM								= false;

	public static final String	THEME_DEFAULT							= "aircandi_theme_midnight";
	public static final String	URL_AIRCANDI_UPGRADE					= "https://aircandi.com/install";
	public static final String	URL_AIRCANDI_TERMS						= "https://aircandi.com/pages/terms.html";
	public static final String	S3_BUCKET_IMAGES						= "aircandi-images";

	public static final String	TYPE_CANDI_POST							= "com.aircandi.candi.post";
	public static final String	TYPE_CANDI_PICTURE						= "com.aircandi.candi.picture";
	public static final String	TYPE_CANDI_PLACE						= "com.aircandi.candi.place";
	public static final String	TYPE_CANDI_SOURCE						= "com.aircandi.candi.source";
	public static final String	TYPE_CANDI_LINK							= "com.aircandi.candi.link";

	public static final int		ACTIVITY_ENTITY_HANDLER					= 100;
	public static final int		ACTIVITY_MARKET							= 200;
	public static final int		ACTIVITY_PICTURE_PICK_DEVICE			= 300;
	public static final int		ACTIVITY_PICTURE_SEARCH					= 305;
	public static final int		ACTIVITY_PICTURE_MAKE					= 310;
	public static final int		ACTIVITY_PICTURE_PICK_PLACE				= 315;
	public static final int		ACTIVITY_VIDEO_PICK						= 320;
	public static final int		ACTIVITY_VIDEO_MAKE						= 330;
	public static final int		ACTIVITY_SIGNIN							= 400;
	public static final int		ACTIVITY_SIGNUP							= 410;
	public static final int		ACTIVITY_PROFILE						= 420;
	public static final int		ACTIVITY_COMMENT						= 430;
	public static final int		ACTIVITY_COMMENT_LIST					= 435;
	public static final int		ACTIVITY_CANDI_INFO						= 440;
	public static final int		ACTIVITY_CANDI_LIST						= 450;
	public static final int		ACTIVITY_LINK_PICK						= 500;
	public static final int		ACTIVITY_WEBSITE_EDIT					= 525;
	public static final int		ACTIVITY_FACEBOOK_PICK					= 530;
	public static final int		ACTIVITY_CANDI_PICK						= 550;
	public static final int		ACTIVITY_TEMPLATE_PICK					= 560;
	public static final int		ACTIVITY_PREFERENCES					= 600;
	public static final int		ACTIVITY_CUSTOM_PLACE					= 700;
	public static final int		ACTIVITY_ADDRESS_EDIT					= 800;
	public static final int		ACTIVITY_CATEGORY_EDIT					= 810;
	public static final int		ACTIVITY_ENTITY_EDIT					= 900;
	public static final int		ACTIVITY_PICTURE_SOURCE_PICK			= 950;

	public static final int		RESULT_ENTITY_INSERTED					= 100;
	public static final int		RESULT_ENTITY_UPDATED					= 110;
	public static final int		RESULT_ENTITY_DELETED					= 120;
	public static final int		RESULT_ENTITY_CHILD_DELETED				= 130;
	public static final int		RESULT_COMMENT_INSERTED					= 200;
	public static final int		RESULT_PROFILE_INSERTED					= 300;
	public static final int		RESULT_PROFILE_UPDATED					= 310;
	public static final int		RESULT_USER_SIGNED_IN					= 400;

	public static final int		VISIBILITY_PUBLIC						= 0;
	public static final int		VISIBILITY_PRIVATE						= 1;

	public static final int		NOTIFICATION_NETWORK					= 1000;
	public static final int		NOTIFICATION_UPDATE						= 2000;

	public static final float	DURATION_MULTIPLIER						= 1.0f;
	public static final float	DURATION_ZOOM							= 0.5f * DURATION_MULTIPLIER;
	public static final float	DURATION_BOUNCEBACK						= 0.5f * DURATION_MULTIPLIER;
	public static final float	DURATION_SLOTTING_MAJOR					= 0.5f * DURATION_MULTIPLIER;
	public static final float	DURATION_SLOTTING_MINOR					= 0.5f * DURATION_MULTIPLIER;
	public static final float	DURATION_CANDIINFO_SHOW					= 0.4f * DURATION_MULTIPLIER;
	public static final float	DURATION_CANDIINFO_HIDE					= 0.4f * DURATION_MULTIPLIER;
	public static final float	DURATION_CANDIBODY_COLLAPSE				= 0.8f * DURATION_MULTIPLIER;
	public static final float	DURATION_CANDIBODY_EXPAND				= 0.8f * DURATION_MULTIPLIER;
	public static final float	DURATION_REFLECTION_HIDESHOW			= 0.8f * DURATION_MULTIPLIER;
	public static final float	DURATION_PLACEHOLDER_HIDESHOW			= 0.5f * DURATION_MULTIPLIER;
	public static final float	DURATION_ZONE_HIDESHOW					= 1.5f * DURATION_MULTIPLIER;
	public static final float	DURATION_TRANSITIONS_OVERFLOW_FADE		= 0.5f * DURATION_MULTIPLIER;
	public static final float	DURATION_TRANSITIONS_FADE				= 0.8f * DURATION_MULTIPLIER;
	public static final float	DURATION_TRANSITIONS_FADE_TEST			= 3.0f * DURATION_MULTIPLIER;
	public static final float	DURATION_TRANSITIONS_MOVE				= 0.8f * DURATION_MULTIPLIER;
	public static final float	DURATION_TRANSITIONS_DELAY				= 0.5f * DURATION_MULTIPLIER;
	public static final float	DURATION_INSTANT						= 0.0f;

	public static final boolean	TRANSITIONS_ACTIVE						= true;

	public static final String	USER_AGENT_MOBILE						= "Mozilla/5.0 (Linux; U; Android 2.1.3; en-us; "
																				+ "Nexus S Build/GRK39F) AppleWebKit/533.1 (KHTML, like Gecko) "
																				+ "Version/4.0 Mobile Safari/533.1";
	public static final String	USER_AGENT_DESKTOP						= "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_3; en-us) "
																				+ "AppleWebKit/533.16 (KHTML, like Gecko) "
																				+ "Version/5.0 Safari/533.16";
	public static final String	USER_AGENT_BACK							= "Mozilla/5.0 (Linux; U; Android 2.2.1; fr-ch; A43 Build/FROYO) "
																				+ "AppleWebKit/533.1 (KHTML, like Gecko) "
																				+ "Version/4.0 Mobile Safari/533.1";
	public static final String	USER_AGENT_NEXUS_S						= "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus S Build/GRK39F) "
																				+ "AppleWebKit/533.1 (KHTML, like Gecko) "
																				+ "Version/4.0 Mobile Safari/533.1";

	/* The default search radius in meters when searching for nearby beacons. */
	public static int			LOCATION_DEFAULT_RADIUS					= 150;

	/* The maximum distance the user should travel between location updates. */
	public static int			LOCATION_MAX_DISTANCE					= LOCATION_DEFAULT_RADIUS / 2;

	/* The maximum time that should pass before the user gets a location update. */
	public static long			LOCATION_MAX_TIME						= AlarmManager.INTERVAL_FIFTEEN_MINUTES;

	/*
	 * You will generally want passive location updates to occur less frequently
	 * than active updates. You need to balance location freshness with battery
	 * life. The location update distance for passive updates.
	 */
	public static int			LOCATION_PASSIVE_MAX_DISTANCE			= LOCATION_MAX_DISTANCE;

	/* The location update time for passive updates */
	public static long			LOCATION_PASSIVE_MAX_TIME				= LOCATION_MAX_TIME;

	/* Use the GPS (fine location provider) when the Activity is visible? */
	public static boolean		USE_GPS_WHEN_ACTIVITY_VISIBLE			= true;

	/* When the user exits via the back button, do you want to disable passive background updates. */
	public static boolean		DISABLE_PASSIVE_LOCATION_WHEN_USER_EXIT	= false;

	/* Maximum latency before you force a cached detail page to be updated. */
	public static long			MAX_DETAILS_UPDATE_LATENCY				= AlarmManager.INTERVAL_DAY;

	/*
	 * Prefetching place details is useful but potentially expensive. The
	 * following values lets you disable prefetching when on mobile data or low battery
	 * conditions. Only prefetch on WIFI?
	 */
	public static boolean		PREFETCH_ON_WIFI_ONLY					= false;

	/* Disable prefetching when battery is low? */
	public static boolean		DISABLE_PREFETCH_ON_LOW_BATTERY			= true;

	/* How long to wait before retrying failed checkins. */
	public static long			CHECKIN_RETRY_INTERVAL					= AlarmManager.INTERVAL_FIFTEEN_MINUTES;

	/* The maximum number of locations to prefetch for each update. */
	public static int			PREFETCH_LIMIT							= 5;

	public static boolean		SUPPORTS_ECLAIR							= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ECLAIR;
	public static boolean		SUPPORTS_FROYO							= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO;
	public static boolean		SUPPORTS_GINGERBREAD					= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD;
	public static boolean		SUPPORTS_HONEYCOMB						= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB;
	public static boolean		SUPPORTS_ICE_CREAM_SANDWICH				= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	public static boolean		SUPPORTS_JELLY_BEAN						= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN;
}
