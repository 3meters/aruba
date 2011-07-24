package com.proxibase.aircandi.candi.utils;

public interface CandiConstants {

	public static final int		CANDI_VIEW_WIDTH				= 250;
	public static final int		CANDI_VIEW_SPACING				= 20;
	public static final int		CANDI_VIEW_BODY_HEIGHT			= 250;
	public static final int		CANDI_VIEW_TITLE_HEIGHT			= 100;
	public static final int		CANDI_VIEW_REFLECTION_HEIGHT	= 125;
	public static final int		CANDI_TITLEBAR_HEIGHT			= 63;
	public static final int		CANDI_VIEW_TITLE_SPACER_HEIGHT	= 2;
	public static final int		CANDI_VIEW_FONT_SIZE			= 24;
	public static final int		CANDI_VIEW_HEIGHT				= CANDI_VIEW_BODY_HEIGHT + CANDI_VIEW_TITLE_HEIGHT + CANDI_VIEW_TITLE_SPACER_HEIGHT;
	public static final int		CANDI_VIEW_ZOOMED_PADDING		= 10;
	public static final int		SWIPE_MAX_OFF_PATH				= 500;
	public static final int		LAYER_GENERAL					= 0;
	public static final int		LAYER_ZONES						= 1;
	public static final int		LAYER_CANDI						= 2;
	public static final int		DIALOG_INSTALL_ID				= 1;
	public static final int		DIALOG_WIFIENABLE_ID			= 2;
	public static final int		VERTEX_INDEX_X					= 0;
	public static final int		VERTEX_INDEX_Y					= 1;
	public static final String	PATH_IMAGECACHE					= "/imagecache/aircandi";
	public static final long	TOUCH_MAX_TIME					= 500;

	public static final int		TWO_MINUTES						= 1000 * 60 * 2;
	public static final int		FIVE_MINUTES					= 1000 * 60 * 5;
	public static final String	URL_AIRCANDI_SERVICE_ODATA		= "http://dev.aircandi.com/airodata.svc/";
	public static final String	URL_AIRCANDI_SERVICE			= "http://dev.aircandi.com/airlogic.asmx/";
	public static final String	URL_AIRCANDI_BLOG				= "http://devblog.proxibase.com/";
	public static final String	URL_AIRCANDI_MEDIA				= "https://s3.amazonaws.com/";

	public static final boolean	MODE_DEBUG						= true;
	public static final String	APP_NAME						= "Aircandi";

}
