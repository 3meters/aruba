// $codepro.audit.disable fileComment
package com.aircandi;

import android.graphics.Bitmap.Config;
import android.util.Log;

import com.aircandi.beta.BuildConfig;

@SuppressWarnings("ucd")
public final class Constants {

	public static final boolean	DEBUG_TRACE							= false;
	public static final int		LOG_LEVEL							= BuildConfig.DEBUG ? Log.VERBOSE : Log.DEBUG;
	public static final boolean	TRACKING_ENABLED					= true;

	public static final String	APP_NAME							= "Aircandi";																				//$NON-NLS-1$
	public static final String	APP_MARKET_URI						= "market://details?id=com.aircandi.beta&referrer=utm_source%3Daircandi_app";
	public static final String	SENDER_ID							= "657673071389";																			// google Api project number for aircandi

	/* Activity parameters */
	public static final String	EXTRA_ENTITY_PARENT_ID				= "com.aircandi.EXTRA_PARENT_ENTITY_ID";													//$NON-NLS-1$
	public static final String	EXTRA_ENTITY_ID						= "com.aircandi.EXTRA_ENTITY_ID";															//$NON-NLS-1$
	public static final String	EXTRA_ENTITY_SCHEMA					= "com.aircandi.EXTRA_ENTITY_SCHEMA";														//$NON-NLS-1$
	public static final String	EXTRA_ENTITY_TYPE					= "com.aircandi.EXTRA_ENTITY_TYPE";														//$NON-NLS-1$
	public static final String	EXTRA_ENTITIES						= "com.aircandi.EXTRA_ENTITIES";															//$NON-NLS-1$
	public static final String	EXTRA_ENTITY						= "com.aircandi.EXTRA_ENTITY";															//$NON-NLS-1$

	public static final String	EXTRA_PLACE							= "com.aircandi.EXTRA_PLACE";																//$NON-NLS-1$
	public static final String	EXTRA_APPLINK						= "com.aircandi.EXTRA_APPLINK";															//$NON-NLS-1$
	public static final String	EXTRA_APPLINKS						= "com.aircandi.EXTRA_APPLINKS";															//$NON-NLS-1$

	public static final String	EXTRA_USER_ID						= "com.aircandi.EXTRA_USER_ID";															//$NON-NLS-1$
	public static final String	EXTRA_URI							= "com.aircandi.EXTRA_URI";																//$NON-NLS-1$
	public static final String	EXTRA_URI_TITLE						= "com.aircandi.EXTRA_URI_TITLE";															//$NON-NLS-1$
	public static final String	EXTRA_URI_DESCRIPTION				= "com.aircandi.EXTRA_URI_DESCRIPTION";													//$NON-NLS-1$

	public static final String	EXTRA_BITMAP						= "com.aircandi.EXTRA_BITMAP";															//$NON-NLS-1$
	public static final String	EXTRA_MESSAGE						= "com.aircandi.EXTRA_MESSAGE";															//$NON-NLS-1$
	public static final String	EXTRA_CATEGORY						= "com.aircandi.EXTRA_CATEGORY";															//$NON-NLS-1$
	public static final String	EXTRA_VERIFY_URI					= "com.aircandi.EXTRA_VERIFY_URI";															//$NON-NLS-1$
	public static final String	EXTRA_SEARCH_PHRASE					= "com.aircandi.EXTRA_SEARCH_PHRASE";														//$NON-NLS-1$
	public static final String	EXTRA_PICTURE_SOURCE				= "com.aircandi.EXTRA_PICTURE_SOURCE";														//$NON-NLS-1$
	public static final String	EXTRA_UPSIZE_SYNTHETIC				= "com.aircandi.EXTRA_UPSIZE_SYNTHETIC";													//$NON-NLS-1$
	public static final String	EXTRA_PAGING_ENABLED				= "com.aircandi.EXTRA_PAGING_ENABLED";														//$NON-NLS-1$
	public static final String	EXTRA_PHOTO							= "com.aircandi.EXTRA_PHOTO";																//$NON-NLS-1$
	public static final String	EXTRA_REFRESH_FORCE					= "com.aircandi.EXTRA_REFRESH_FORCE";														//$NON-NLS-1$
	public static final String	EXTRA_HELP_ID						= "com.aircandi.EXTRA_HELP_ID";															//$NON-NLS-1$
	public static final String	EXTRA_SHORTCUTS						= "com.aircandi.EXTRA_SHORTCUTS";															//$NON-NLS-1$
	public static final String	EXTRA_SKIP_SAVE						= "com.aircandi.EXTRA_EDIT_ONLY";															//$NON-NLS-1$

	/* Activity parameters: lists */
	public static final String	EXTRA_LIST_MODE						= "com.aircandi.EXTRA_LIST_MODE";															//$NON-NLS-1$
	public static final String	EXTRA_LIST_SCHEMA					= "com.aircandi.EXTRA_LIST_SCHEMA";														//$NON-NLS-1$
	public static final String	EXTRA_LIST_NEW_ENABLED				= "com.aircandi.EXTRA_LIST_NEW_ENABLED";													//$NON-NLS-1$
	public static final String	EXTRA_LIST_ITEM_RESID				= "com.aircandi.EXTRA_LIST_ITEM_RESID";													//$NON-NLS-1$

	/* Interval helpers */
	public static final int		MILLS_PER_SECOND					= 1000;
	public static final int		TIME_ONE_SECOND						= MILLS_PER_SECOND * 1;
	public static final int		TIME_TEN_SECONDS					= MILLS_PER_SECOND * 10;
	public static final int		TIME_TWENTY_SECONDS					= MILLS_PER_SECOND * 20;
	public static final int		TIME_THIRTY_SECONDS					= MILLS_PER_SECOND * 30;
	public static final int		TIME_ONE_MINUTE						= MILLS_PER_SECOND * 60;
	public static final int		TIME_TWO_MINUTES					= MILLS_PER_SECOND * 60 << 1;
	public static final int		TIME_FIVE_MINUTES					= MILLS_PER_SECOND * 60 * 5;
	public static final int		TIME_FIFTEEN_MINUTES				= MILLS_PER_SECOND * 60 * 15;
	public static final int		TIME_THIRTY_MINUTES					= MILLS_PER_SECOND * 60 * 30;
	public static final int		TIME_SIXTY_MINUTES					= MILLS_PER_SECOND * 60 * 60;

	/* Wifi scanning */
	public static final int		INTERVAL_SCAN_WIFI					= TIME_ONE_MINUTE;
	public static final int		INTERVAL_CATEGORIES_DOWNLOAD		= TIME_ONE_MINUTE;
	public static final int		INTERVAL_UPDATE_CHECK				= TIME_SIXTY_MINUTES;
	public static final int		INTERVAL_REFRESH					= TIME_THIRTY_MINUTES;
	public static final int		INTERVAL_TETHER_ALERT				= TIME_SIXTY_MINUTES * 12;

	/* Ui */
	public static final int		MAX_Y_OVERSCROLL_DISTANCE			= 50;
	public static final int		TABS_PRIMARY_ID						= 1;
	public static final int		TABS_PROFILE_FORM_ID				= 2;
	public static final int		TABS_ENTITY_FORM_ID					= 3;
	public static final int		TABS_CANDI_PICKER_ID				= 4;
	public static final float	DIALOGS_DIM_AMOUNT					= 0.5f;

	public static final int		RADAR_BEACON_SIGNAL_BUCKET_SIZE		= 1;
	public static final int		RADAR_BEACON_INDICATOR_CAUTION		= -80;
	/*
	 * Using quality = 70 for jpeg compression reduces image file size by 85% with
	 * an acceptable degradation of image quality. A 1280x960 image went from
	 * 1007K to 152K.
	 */
	public static final int		IMAGE_QUALITY_S3					= 70;																						// $codepro.audit.disable constantNamingConvention
	/*
	 * Will handle a typical 5 megapixel 2560x1920 image that has been sampled by two to 1280x960
	 * Sampling by 4 produces 640x480. Assumes four channel ARGB including alpha.
	 */
	public static final int		IMAGE_MEMORY_BYTES_MAX				= 4915200;																					// 4 megapixels
	public static final Config	IMAGE_CONFIG_DEFAULT				= Config.ARGB_8888;
	/*
	 * Consistent with 5 megapixel sampled by two.
	 */
	public static final int		IMAGE_DIMENSION_MAX					= 960;

	public static final String	URL_AIRCANDI_UPGRADE				= "https://aircandi.com/install";															//$NON-NLS-1$
	public static final String	URL_AIRCANDI_TERMS					= "https://aircandi.com/pages/terms.html";													//$NON-NLS-1$
	public static final String	S3_BUCKET_IMAGES					= "aircandi-images";																		// $codepro.audit.disable constantNamingConvention //$NON-NLS-1$

	public static final String	SCHEMA_ENTITY_POST					= "post";																					//$NON-NLS-1$
	public static final String	SCHEMA_ENTITY_PLACE					= "place";																					//$NON-NLS-1$
	public static final String	SCHEMA_ENTITY_COMMENT				= "comment";																				//$NON-NLS-1$
	public static final String	SCHEMA_ENTITY_APPLINK				= "applink";																				//$NON-NLS-1$
	public static final String	SCHEMA_ENTITY_BEACON				= "beacon";																				//$NON-NLS-1$
	public static final String	SCHEMA_ENTITY_USER					= "user";																					//$NON-NLS-1$
	public static final String	SCHEMA_LINK							= "link";																					//$NON-NLS-1$
	public static final String	SCHEMA_ANY							= "any";																					//$NON-NLS-1$

	public static final String	TYPE_ANY							= "any";																					//$NON-NLS-1$

	public static final String	TYPE_APPLINK_FACEBOOK				= "facebook";																				//$NON-NLS-1$
	public static final String	TYPE_APPLINK_TWITTER				= "twitter";																				//$NON-NLS-1$
	public static final String	TYPE_APPLINK_WEBSITE				= "website";																				//$NON-NLS-1$
	public static final String	TYPE_APPLINK_EMAIL					= "email";																					//$NON-NLS-1$
	public static final String	TYPE_APPLINK_YELP					= "yelp";																					//$NON-NLS-1$
	public static final String	TYPE_APPLINK_FOURSQUARE				= "foursquare";																			//$NON-NLS-1$
	public static final String	TYPE_APPLINK_OPENTABLE				= "opentable";																				//$NON-NLS-1$
	public static final String	TYPE_APPLINK_URBANSPOON				= "urbanspoon";																			//$NON-NLS-1$
	public static final String	TYPE_APPLINK_CITYSEARCH				= "citysearch";																			//$NON-NLS-1$
	public static final String	TYPE_APPLINK_CITYGRID				= "citygrid";																				//$NON-NLS-1$
	public static final String	TYPE_APPLINK_YAHOOLOCAL				= "yahoolocal";																			//$NON-NLS-1$
	public static final String	TYPE_APPLINK_OPENMENU				= "openmenu";																				//$NON-NLS-1$
	public static final String	TYPE_APPLINK_ZAGAT					= "zagat";																					//$NON-NLS-1$
	public static final String	TYPE_APPLINK_TRIPADVISOR			= "tripadvisor";																			//$NON-NLS-1$
	public static final String	TYPE_APPLINK_GOOGLEPLACE			= "googleplace";																			//$NON-NLS-1$
	public static final String	TYPE_APPLINK_INSTAGRAM				= "instagram";																				//$NON-NLS-1$

	public static final String	TYPE_APPLINK_MAP					= "map";																					//$NON-NLS-1$
	public static final String	TYPE_APPLINK_LIKE					= "like";																					//$NON-NLS-1$
	public static final String	TYPE_APPLINK_WATCH					= "watch";																					//$NON-NLS-1$
	public static final String	TYPE_APPLINK_COMMENT				= "comment";																				//$NON-NLS-1$
	public static final String	TYPE_APPLINK_POST					= "post";																					//$NON-NLS-1$

	public static final String	TYPE_LINK_PROXIMITY					= "proximity";																				//$NON-NLS-1$
	public static final String	TYPE_LINK_LIKE						= "like";																					//$NON-NLS-1$
	public static final String	TYPE_LINK_WATCH						= "watch";																					//$NON-NLS-1$
	public static final String	TYPE_LINK_APPLINK					= "applink";																				//$NON-NLS-1$
	public static final String	TYPE_LINK_COMMENT					= "comment";																				//$NON-NLS-1$
	public static final String	TYPE_LINK_POST						= "post";																					//$NON-NLS-1$

	public static final String	TYPE_COUNT_LINK_PROXIMITY			= "link_proximity";																		//$NON-NLS-1$
	public static final String	TYPE_COUNT_LINK_PROXIMITY_MINUS		= "link_proximity_minus";																	//$NON-NLS-1$

	public static final String	TYPE_BEACON_FIXED					= "fixed";																					//$NON-NLS-1$
	public static final String	TYPE_BEACON_MOBILE					= "mobile";																				//$NON-NLS-1$
	public static final String	TYPE_BEACON_TEMPORARY				= "temporary";																				//$NON-NLS-1$

	public static final String	TYPE_PROVIDER_FOURSQUARE			= "foursquare";																			//$NON-NLS-1$
	public static final String	TYPE_PROVIDER_GOOGLE				= "google";																				//$NON-NLS-1$
	public static final String	TYPE_PROVIDER_FACTUAL				= "factual";																				//$NON-NLS-1$
	public static final String	TYPE_PROVIDER_AIRCANDI				= "aircandi";																				//$NON-NLS-1$
	public static final String	TYPE_PROVIDER_USER					= "user";																					//$NON-NLS-1$

	public static final String	PHOTO_SOURCE_DEFAULT				= "default";																				//$NON-NLS-1$
	public static final String	PHOTO_SOURCE_SEARCH					= "search";																				//$NON-NLS-1$
	public static final String	PHOTO_SOURCE_GALLERY				= "gallery";																				//$NON-NLS-1$
	public static final String	PHOTO_SOURCE_CAMERA					= "camera";																				//$NON-NLS-1$
	public static final String	PHOTO_SOURCE_PLACE					= "place";																					//$NON-NLS-1$
	public static final String	PHOTO_SOURCE_FACEBOOK				= "facebook";																				//$NON-NLS-1$
	public static final String	PHOTO_SOURCE_TWITTER				= "twitter";																				//$NON-NLS-1$

	public static final int		ACTIVITY_MARKET						= 200;
	public static final int		ACTIVITY_PICTURE_PICK_DEVICE		= 300;
	public static final int		ACTIVITY_PICTURE_SEARCH				= 305;
	public static final int		ACTIVITY_PICTURE_MAKE				= 310;
	public static final int		ACTIVITY_PICTURE_PICK_PLACE			= 315;
	public static final int		ACTIVITY_SIGNIN						= 400;
	public static final int		ACTIVITY_COMMENT					= 430;
	public static final int		ACTIVITY_APPLINKS_EDIT				= 535;
	public static final int		ACTIVITY_APPLINK_EDIT				= 540;
	public static final int		ACTIVITY_APPLINK_NEW				= 545;
	public static final int		ACTIVITY_TEMPLATE_PICK				= 560;
	public static final int		ACTIVITY_PREFERENCES				= 600;
	public static final int		ACTIVITY_ADDRESS_EDIT				= 800;
	public static final int		ACTIVITY_CATEGORY_EDIT				= 810;
	public static final int		ACTIVITY_ENTITY_EDIT				= 900;
	public static final int		ACTIVITY_PICTURE_SOURCE_PICK		= 950;
	public static final int		ACTIVITY_ENTITY_INSERT				= 960;

	public static final int		RESULT_ENTITY_INSERTED				= 100;
	public static final int		RESULT_ENTITY_UPDATED				= 110;
	public static final int		RESULT_ENTITY_DELETED				= 120;
	public static final int		RESULT_ENTITY_EDITED				= 130;
	public static final int		RESULT_COMMENT_INSERTED				= 200;
	public static final int		RESULT_PROFILE_UPDATED				= 310;
	public static final int		RESULT_USER_SIGNED_IN				= 400;

	public static final int		NOTIFICATION_NETWORK				= 1000;
	public static final int		NOTIFICATION_UPDATE_AIRCANDI		= 2000;
	public static final int		NOTIFICATION_COMMENT				= 3000;
	public static final int		NOTIFICATION_CANDI_ADDED			= 4000;

	public static final boolean	SUPPORTS_ECLAIR						= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ECLAIR;
	public static final boolean	SUPPORTS_FROYO						= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO;
	public static final boolean	SUPPORTS_GINGERBREAD				= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD;
	public static final boolean	SUPPORTS_HONEYCOMB					= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB;
	public static final boolean	SUPPORTS_ICE_CREAM_SANDWICH			= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	public static final boolean	SUPPORTS_JELLY_BEAN					= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN;

	/* Prefs - users */
	public static final String	PREF_THEME							= "Pref_Theme";
	public static final String	PREF_SEARCH_RADIUS					= "Pref_Search_Radius";
	public static final String	PREF_SOUND_EFFECTS					= "Pref_Sound_Effects";
	public static final String	PREF_NOTIFICATIONS_COMMENTS			= "Pref_Notifications_Comments";
	public static final String	PREF_NOTIFICATIONS_POSTS			= "Pref_Notifications_Posts";
	public static final String	PREF_NOTIFICATIONS_NEARBY			= "Pref_Notifications_Nearby";

	/* Prefs - dev only */
	public static final String	PREF_ENABLE_DEV						= "Pref_Enable_Dev";
	public static final String	PREF_ENTITY_FENCING					= "Pref_Entity_Fencing";
	public static final String	PREF_SHOW_PLACE_RANK_SCORE			= "Pref_Show_Place_Rank_Score";
	public static final String	PREF_TESTING_BEACONS				= "Pref_Testing_Beacons";
	public static final String	PREF_TESTING_LOCATION				= "Pref_Testing_Location";
	public static final String	PREF_TESTING_PLACE_PROVIDER			= "Pref_Testing_Place_Provider";

	/* Settings */
	public static final String	SETTING_USER						= "Setting_User";
	public static final String	SETTING_USER_SESSION				= "Setting_User_Session";
	public static final String	SETTING_PICTURE_SEARCH				= "Setting_Picture_Search";
	public static final String	SETTING_LAST_EMAIL					= "Setting_Last_Email";
	public static final String	SETTING_RUN_ONCE_HELP_RADAR			= "Setting_Run_Once_Help_Radar";
	public static final String	SETTING_RUN_ONCE_HELP_CANDI_PLACE	= "Setting_Run_Once_Help_Candi_Place";

	/* Defaults */
	public static final String	PREF_THEME_DEFAULT					= "aircandi_theme_midnight";
	public static final String	PREF_SEARCH_RADIUS_DEFAULT			= "8047";																					// five miles
	public static final Boolean	PREF_SOUND_EFFECTS_DEFAULT			= true;
	public static final Boolean	PREF_NOTIFICATIONS_COMMENTS_DEFAULT	= true;
	public static final Boolean	PREF_NOTIFICATIONS_POSTS_DEFAULT	= true;
	public static final Boolean	PREF_NOTIFICATIONS_NEARBY_DEFAULT	= true;

	public static final Boolean	PREF_ENABLE_DEV_DEFAULT				= false;
	public static final Boolean	PREF_ENTITY_FENCING_DEFAULT			= false;
	public static final Boolean	PREF_SHOW_PLACE_RANK_SCORE_DEFAULT	= false;
	public static final String	PREF_TESTING_BEACONS_DEFAULT		= "natural";
	public static final String	PREF_TESTING_LOCATION_DEFAULT		= "natural";
	public static final String	PREF_TESTING_PLACE_PROVIDER_DEFAULT	= TYPE_PROVIDER_FOURSQUARE;

	private Constants() {}; // $codepro.audit.disable emptyMethod
}
