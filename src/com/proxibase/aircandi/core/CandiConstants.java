package com.proxibase.aircandi.core;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.util.modifier.ease.EaseExponentialOut;
import org.anddev.andengine.util.modifier.ease.EaseLinear;
import org.anddev.andengine.util.modifier.ease.EaseQuartIn;
import org.anddev.andengine.util.modifier.ease.EaseQuartOut;
import org.anddev.andengine.util.modifier.ease.EaseStrongOut;
import org.anddev.andengine.util.modifier.ease.IEaseFunction;

import android.graphics.Bitmap.Config;

public interface CandiConstants {

	public static final boolean			DEBUG_TRACE							= false;
	public static final String			APP_NAME							= "Aircandi";
	public static final String			APP_PACKAGE_NAME					= "com.proxibase.aircandi.";
	public static final String			GOOGLE_API_KEY_RELEASE				= "0NLNz2BjIntPUuNQrqaiA6J8TTFflv_PHQ2cTdQ";
	public static final String			GOOGLE_API_KEY_DEBUG				= "0NLNz2BjIntNt_gkO2_iEs5wIIxNGrG_pmHjPbA";

	public static final int				LAYER_GENERAL						= 0;
	public static final int				LAYER_ZONES							= 1;
	public static final int				LAYER_CANDI							= 2;

	public static final int				INTERVAL_NETWORK_PHONEY				= 3000;
	public static final int				INTERVAL_SCAN						= 60000;
	public static final int				LOCATION_SCAN_TIME_LIMIT			= 45000;
	public static final int				LOCATION_EXPIRATION					= 30000;

	public static final float			CANDI_VIEW_HALO						= 2.0f;

	public static final int				CANDI_VIEW_WIDTH					= 250;
	public static final int				CANDI_VIEW_SPACING					= 20;
	public static final int				CANDI_VIEW_BODY_HEIGHT				= 250;
	public static final int				CANDI_VIEW_TITLE_HEIGHT				= 100;
	public static final int				CANDI_VIEW_REFLECTION_HEIGHT		= 125;
	public static final int				CANDI_VIEW_REFLECTION_GAP			= 1;
	public static final int				CANDI_VIEW_TITLE_SPACER_HEIGHT		= 2;
	public static final int				CANDI_VIEW_FONT_SIZE				= 24;
	public static final int				CANDI_VIEW_HEIGHT					= CANDI_VIEW_BODY_HEIGHT + CANDI_VIEW_TITLE_HEIGHT
																				+ CANDI_VIEW_TITLE_SPACER_HEIGHT;
	public static final int				CANDI_VIEW_ZOOMED_PADDING			= 10;

	public static final int				MAP_VIEW_FONT_SIZE					= 20;
	public static final int				MAP_VIEW_TITLE_MARGIN				= 6;
	public static final int				MAP_VIEW_MARKER_HEIGHT				= 50;
	public static final int				MAP_VIEW_TITLE_LENGTH_MAX			= 150;

	/* Dimensions at mdpi */
	public static final int				CANDI_TITLEBAR_HEIGHT				= 40;
	public static final int				CANDI_TABBAR_HEIGHT					= 40;
	public static final int				ANDROID_STATUSBAR_HEIGHT			= 25;

	public static final int				SWIPE_MAX_OFF_PATH					= 500;
	public static final float			SWIPE_SMALL_FLING					= 100;
	public static final long			TOUCH_MAX_TIME						= 500;

	public static final int				DIALOG_INSTALL_ID					= 1;
	public static final int				DIALOG_WIFIENABLE_ID				= 2;
	public static final int				DIALOG_NEW_CANDI_ID					= 3;

	public static final int				VERTEX_INDEX_X						= 0;
	public static final int				VERTEX_INDEX_Y						= 1;

	public static final String			CACHE_PATH							= "/imagecache/aircandi";
	public static final long			CACHE_TARGET_SIZE					= 8000000;
	public static final long			CACHE_TRIGGER_SIZE					= 10000000;

	public static final int				TWO_MINUTES							= 1000 * 60 * 2;
	public static final int				FIVE_MINUTES						= 1000 * 60 * 5;

	public static final int				IMAGE_MEMORY_BYTES_MAX				= 2048000;
	public static final Config			IMAGE_CONFIG_DEFAULT				= Config.ARGB_8888;
	public static final int				IMAGE_WIDTH_SMALL					= 60;
	public static final int				IMAGE_WIDTH_DEFAULT					= 250;
	public static final int				IMAGE_WIDTH_ORIGINAL				= -1;
	public static final String			IMAGE_BROKEN						= "gfx/placeholder3.png";

	public static final String			URL_PROXIBASE						= "https://api.proxibase.com/";

	public static final String			URL_AIRCANDI_SERVICE_ODATA			= "http://dev.aircandi.com/airodata.svc/";
	public static final String			URL_AIRCANDI_SERVICE				= "http://dev.aircandi.com/airlogic.asmx/";
	public static final String			URL_AIRCANDI_BLOG					= "http://devblog.proxibase.com/";
	public static final String			URL_AIRCANDI_MEDIA					= "https://s3.amazonaws.com/";
	public static final String			S3_BUCKET_IMAGES					= "3meters_images";

	public static final String			TYPE_CANDI_POST						= "com.proxibase.aircandi.candi.post";
	public static final String			TYPE_CANDI_PICTURE					= "com.proxibase.aircandi.candi.picture";
	public static final String			TYPE_CANDI_LINK						= "com.proxibase.aircandi.candi.link";

	public static final String			USER_ANONYMOUS						= "{\"data\": [{\"_id\": \"0000.000000.00000.000.000000\",\"_modifier\": \"0000.000000.00000.000.000001\",\"_creator\": \"0000.000000.00000.000.000001\",\"_owner\": \"0000.000000.00000.000.000001\",\"name\": \"Anonymous\",\"email\": \"anonymous@3meters.com\",\"password\": \"12345678\",\"imageUri\": \"resource:placeholder_user\", \"isDeveloper\": false, \"createdDate\": 1317427200,\"modifiedDate\": 1317427200}], \"count\": 1, \"moreRecords\": false}";

	public static final int				ACTIVITY_ENTITY_HANDLER				= 100;
	public static final int				ACTIVITY_MARKET						= 200;
	public static final int				ACTIVITY_PICTURE_PICK_DEVICE		= 300;
	public static final int				ACTIVITY_PICTURE_PICK_AIRCANDI		= 305;
	public static final int				ACTIVITY_PICTURE_MAKE				= 310;
	public static final int				ACTIVITY_VIDEO_PICK					= 320;
	public static final int				ACTIVITY_VIDEO_MAKE					= 330;
	public static final int				ACTIVITY_SIGNIN						= 400;
	public static final int				ACTIVITY_SIGNUP						= 410;
	public static final int				ACTIVITY_PROFILE					= 420;
	public static final int				ACTIVITY_COMMENT					= 430;
	public static final int				ACTIVITY_COMMENT_LIST				= 435;
	public static final int				ACTIVITY_CANDI_INFO					= 440;
	public static final int				ACTIVITY_LINK_PICK					= 500;
	public static final int				ACTIVITY_PREFERENCES				= 600;

	public static final int				RESULT_ENTITY_INSERTED				= 100;
	public static final int				RESULT_ENTITY_UPDATED				= 110;
	public static final int				RESULT_ENTITY_DELETED				= 120;
	public static final int				RESULT_COMMENT_INSERTED				= 200;
	public static final int				RESULT_PROFILE_INSERTED				= 300;
	public static final int				RESULT_PROFILE_UPDATED				= 310;
	public static final int				RESULT_USER_SIGNED_IN				= 400;

	public static final int				VISIBILITY_PUBLIC					= 0;
	public static final int				VISIBILITY_PRIVATE					= 1;

	public static final int				NOTIFICATION_NETWORK				= 1000;

	public static final float			DURATION_MULTIPLIER					= 1.0f;
	public static final float			DURATION_ZOOM						= 0.5f * DURATION_MULTIPLIER;
	public static final float			DURATION_BOUNCEBACK					= 1.0f * DURATION_MULTIPLIER;
	public static final float			DURATION_SLOTTING_MAJOR				= 0.5f * DURATION_MULTIPLIER;
	public static final float			DURATION_SLOTTING_MINOR				= 0.5f * DURATION_MULTIPLIER;
	public static final float			DURATION_CANDIINFO_SHOW				= 0.4f * DURATION_MULTIPLIER;
	public static final float			DURATION_CANDIINFO_HIDE				= 0.4f * DURATION_MULTIPLIER;
	public static final float			DURATION_CANDIBODY_COLLAPSE			= 0.8f * DURATION_MULTIPLIER;
	public static final float			DURATION_CANDIBODY_EXPAND			= 0.8f * DURATION_MULTIPLIER;
	public static final float			DURATION_REFLECTION_HIDESHOW		= 0.8f * DURATION_MULTIPLIER;
	public static final float			DURATION_PLACEHOLDER_HIDESHOW		= 0.5f * DURATION_MULTIPLIER;
	public static final float			DURATION_TRANSITIONS_OVERFLOW_FADE	= 0.5f * DURATION_MULTIPLIER;
	public static final float			DURATION_TRANSITIONS_FADE			= 0.8f * DURATION_MULTIPLIER;
	public static final float			DURATION_TRANSITIONS_FADE_TEST		= 3.0f * DURATION_MULTIPLIER;
	public static final float			DURATION_TRANSITIONS_MOVE			= 0.8f * DURATION_MULTIPLIER;
	public static final float			DURATION_TRANSITIONS_DELAY			= 0.5f * DURATION_MULTIPLIER;
	public static final float			DURATION_INSTANT					= 0.0f;

	public static final IEaseFunction	EASE_FADE_IN						= EaseLinear.getInstance();
	public static final IEaseFunction	EASE_FADE_OUT						= EaseLinear.getInstance();
	public static final IEaseFunction	EASE_FADE_OUT_STRONG				= EaseQuartOut.getInstance();
	public static final IEaseFunction	EASE_FADE_OUT_WEAK					= EaseQuartIn.getInstance();
	public static final IEaseFunction	EASE_SLOTTING_MINOR					= EaseStrongOut.getInstance();
	public static final IEaseFunction	EASE_SLOTTING_MAJOR					= EaseStrongOut.getInstance();
	public static final IEaseFunction	EASE_BOUNCE_BACK					= EaseStrongOut.getInstance();
	public static final IEaseFunction	EASE_FLING							= EaseExponentialOut.getInstance();

	public static final boolean			TRANSITIONS_ACTIVE					= true;

	public static final int				GL_BLEND_FUNCTION_SOURCE			= GL10.GL_ONE;
	public static final int				GL_BLEND_FUNCTION_DESTINATION		= GL10.GL_ONE_MINUS_SRC_ALPHA;
	public static final TextureOptions	GL_TEXTURE_OPTION					= TextureOptions.BILINEAR_PREMULTIPLYALPHA;
	public static final String			USER_AGENT_MOBILE					= "Mozilla/5.0 (Linux; U; Android 2.1.3; en-us; Nexus S Build/GRK39F) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";
	public static final String			USER_AGENT_DESKTOP					= "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_3; en-us) AppleWebKit/533.16 (KHTML, like Gecko) Version/5.0 Safari/533.16";
	public static final String			USER_AGENT_BACK						= "Mozilla/5.0 (Linux; U; Android 2.2.1; fr-ch; A43 Build/FROYO) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";
	public static final String			USER_AGENT_NEXUS_S					= "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus S Build/GRK39F) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";

}
