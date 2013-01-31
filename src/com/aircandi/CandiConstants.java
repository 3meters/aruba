// $codepro.audit.disable fileComment
package com.aircandi;

import android.graphics.Bitmap.Config;
import android.util.Log;

@SuppressWarnings("ucd")
public final class CandiConstants {

	public static final boolean	DEBUG_TRACE						= false;
	public static final int		LOG_LEVEL						= BuildConfig.DEBUG ? Log.VERBOSE : Log.DEBUG;
	public static final boolean	TRACKING_ENABLED				= true;

	public static final String	APP_NAME						= "Aircandi";																				//$NON-NLS-1$

	/* Activity parameters */
	public static final String	EXTRA_PARENT_ENTITY_ID			= "com.aircandi.EXTRA_PARENT_ENTITY_ID";													//$NON-NLS-1$
	public static final String	EXTRA_ENTITY_ID					= "com.aircandi.EXTRA_ENTITY_ID";															//$NON-NLS-1$
	public static final String	EXTRA_ENTITY_IS_ROOT			= "com.aircandi.EXTRA_ENTITY_IS_ROOT";														//$NON-NLS-1$
	public static final String	EXTRA_ENTITY_TYPE				= "com.aircandi.EXTRA_ENTITY_TYPE";														//$NON-NLS-1$
	public static final String	EXTRA_USER_ID					= "com.aircandi.EXTRA_USER_ID";															//$NON-NLS-1$
	public static final String	EXTRA_URI						= "com.aircandi.EXTRA_URI";																//$NON-NLS-1$
	public static final String	EXTRA_URI_TITLE					= "com.aircandi.EXTRA_URI_TITLE";															//$NON-NLS-1$
	public static final String	EXTRA_URI_DESCRIPTION			= "com.aircandi.EXTRA_URI_DESCRIPTION";													//$NON-NLS-1$
	public static final String	EXTRA_MESSAGE					= "com.aircandi.EXTRA_MESSAGE";															//$NON-NLS-1$
	public static final String	EXTRA_ADDRESS					= "com.aircandi.EXTRA_ADDRESS";															//$NON-NLS-1$
	public static final String	EXTRA_CATEGORY					= "com.aircandi.EXTRA_CATEGORY";															//$NON-NLS-1$
	public static final String	EXTRA_SOURCE					= "com.aircandi.EXTRA_SOURCE";																//$NON-NLS-1$
	public static final String	EXTRA_SOURCES					= "com.aircandi.EXTRA_SOURCES";															//$NON-NLS-1$
	public static final String	EXTRA_PHONE						= "com.aircandi.EXTRA_PHONE";																//$NON-NLS-1$
	public static final String	EXTRA_COLLECTION_ID				= "com.aircandi.EXTRA_COLLECTION_ID";														//$NON-NLS-1$
	public static final String	EXTRA_VERIFY_URI				= "com.aircandi.EXTRA_VERIFY_URI";															//$NON-NLS-1$
	public static final String	EXTRA_COMMAND_TYPE				= "com.aircandi.EXTRA_COMMAND_TYPE";														//$NON-NLS-1$
	public static final String	EXTRA_SEARCH_PHRASE				= "com.aircandi.EXTRA_SEARCH_PHRASE";														//$NON-NLS-1$
	public static final String	EXTRA_LIST_TYPE					= "com.aircandi.EXTRA_LIST_TARGET";														//$NON-NLS-1$
	public static final String	EXTRA_PICTURE_SOURCE			= "com.aircandi.EXTRA_PICTURE_SOURCE";														//$NON-NLS-1$
	public static final String	EXTRA_UPSIZE_SYNTHETIC			= "com.aircandi.EXTRA_UPSIZE_SYNTHETIC";													//$NON-NLS-1$
	public static final String	EXTRA_PAGING_ENABLED			= "com.aircandi.EXTRA_PAGING_ENABLED";														//$NON-NLS-1$

	public static final int		MILLS_PER_SECOND				= 1000;
	public static final int		TIME_ONE_SECOND					= MILLS_PER_SECOND * 1;
	public static final int		TIME_TEN_SECONDS				= MILLS_PER_SECOND * 10;
	public static final int		TIME_TWENTY_SECONDS				= MILLS_PER_SECOND * 20;
	public static final int		TIME_THIRTY_SECONDS				= MILLS_PER_SECOND * 30;
	public static final int		TIME_ONE_MINUTE					= MILLS_PER_SECOND * 60;
	public static final int		TIME_TWO_MINUTES				= MILLS_PER_SECOND * 60 << 1;
	public static final int		TIME_FIVE_MINUTES				= MILLS_PER_SECOND * 60 * 5;
	public static final int		TIME_FIFTEEN_MINUTES			= MILLS_PER_SECOND * 60 * 15;
	public static final int		TIME_THIRTY_MINUTES				= MILLS_PER_SECOND * 60 * 30;
	public static final int		TIME_SIXTY_MINUTES				= MILLS_PER_SECOND * 60 * 60;

	/* Wifi scanning */
	public static final int		INTERVAL_SCAN_WIFI				= TIME_ONE_MINUTE;
	public static final int		INTERVAL_UPDATE_CHECK			= TIME_SIXTY_MINUTES;
	public static final int		INTERVAL_REFRESH				= TIME_THIRTY_MINUTES;

	public static final int		MAX_Y_OVERSCROLL_DISTANCE		= 50;

	public static final int		RADAR_BEACON_SIGNAL_BUCKET_SIZE	= 1;
	public static final int		RADAR_BEACON_INDICATOR_CAUTION	= -80;

	public static final int		TABS_PRIMARY_ID					= 1;
	public static final int		TABS_PROFILE_FORM_ID			= 2;
	public static final int		TABS_ENTITY_FORM_ID				= 3;
	public static final int		TABS_CANDI_PICKER_ID			= 4;

	/*
	 * Using quality = 70 for jpeg compression reduces image file size by 85% with
	 * an acceptable degradation of image quality. A 1280x960 image went from
	 * 1007K to 152K.
	 */
	public static final int		IMAGE_QUALITY_S3				= 70;																						// $codepro.audit.disable constantNamingConvention
	/*
	 * Will handle a typical 5 megapixel 2560x1920 image that has been sampled by two to 1280x960
	 * Sampling by 4 produces 640x480. Assumes four channel ARGB including alpha.
	 */
	public static final int		IMAGE_MEMORY_BYTES_MAX			= 4915200;																					// 4 megapixels
	public static final Config	IMAGE_CONFIG_DEFAULT			= Config.ARGB_8888;
	/*
	 * Consistent with 5 megapixel sampled by two.
	 */
	public static final int		IMAGE_DIMENSION_MAX				= 960;

	public static final String	THEME_DEFAULT					= "aircandi_theme_midnight";																//$NON-NLS-1$
	public static final String	URL_AIRCANDI_UPGRADE			= "https://aircandi.com/install";															//$NON-NLS-1$
	public static final String	URL_AIRCANDI_TERMS				= "https://aircandi.com/pages/terms.html";													//$NON-NLS-1$
	public static final String	S3_BUCKET_IMAGES				= "aircandi-images";																		// $codepro.audit.disable constantNamingConvention //$NON-NLS-1$

	public static final String	TYPE_CANDI_POST					= "com.aircandi.candi.post";																//$NON-NLS-1$
	public static final String	TYPE_CANDI_PICTURE				= "com.aircandi.candi.picture";															//$NON-NLS-1$
	public static final String	TYPE_CANDI_PLACE				= "com.aircandi.candi.place";																//$NON-NLS-1$
	public static final String	TYPE_CANDI_SOURCE				= "com.aircandi.candi.source";																//$NON-NLS-1$
	public static final String	TYPE_CANDI_LINK					= "com.aircandi.candi.link";																//$NON-NLS-1$

	public static final int		ACTIVITY_MARKET					= 200;
	public static final int		ACTIVITY_PICTURE_PICK_DEVICE	= 300;
	public static final int		ACTIVITY_PICTURE_SEARCH			= 305;
	public static final int		ACTIVITY_PICTURE_MAKE			= 310;
	public static final int		ACTIVITY_PICTURE_PICK_PLACE		= 315;
	public static final int		ACTIVITY_SIGNIN					= 400;
	public static final int		ACTIVITY_COMMENT				= 430;
	public static final int		ACTIVITY_SOURCES_EDIT			= 535;
	public static final int		ACTIVITY_SOURCE_EDIT			= 540;
	public static final int		ACTIVITY_SOURCE_NEW				= 545;
	public static final int		ACTIVITY_TEMPLATE_PICK			= 560;
	public static final int		ACTIVITY_PREFERENCES			= 600;
	public static final int		ACTIVITY_ADDRESS_EDIT			= 800;
	public static final int		ACTIVITY_CATEGORY_EDIT			= 810;
	public static final int		ACTIVITY_ENTITY_EDIT			= 900;
	public static final int		ACTIVITY_PICTURE_SOURCE_PICK	= 950;

	public static final int		RESULT_ENTITY_INSERTED			= 100;
	public static final int		RESULT_ENTITY_UPDATED			= 110;
	public static final int		RESULT_ENTITY_DELETED			= 120;
	public static final int		RESULT_COMMENT_INSERTED			= 200;
	public static final int		RESULT_PROFILE_UPDATED			= 310;
	public static final int		RESULT_USER_SIGNED_IN			= 400;

	public static final int		NOTIFICATION_NETWORK			= 1000;
	public static final int		NOTIFICATION_UPDATE				= 2000;

	/* The default search radius in meters when searching for nearby beacons. */
	public static final int		LOCATION_DEFAULT_RADIUS			= 150;

	public static final boolean	SUPPORTS_ECLAIR					= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ECLAIR;
	public static final boolean	SUPPORTS_FROYO					= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO;
	public static final boolean	SUPPORTS_GINGERBREAD			= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD;
	public static final boolean	SUPPORTS_HONEYCOMB				= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB;
	public static final boolean	SUPPORTS_ICE_CREAM_SANDWICH		= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	public static final boolean	SUPPORTS_JELLY_BEAN				= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN;

	private CandiConstants() {}; // $codepro.audit.disable emptyMethod
}
