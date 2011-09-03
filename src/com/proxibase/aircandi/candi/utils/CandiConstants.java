package com.proxibase.aircandi.candi.utils;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.opengl.texture.TextureOptions;

import android.graphics.Bitmap.Config;

public interface CandiConstants {

	public static final boolean			MODE_DEBUG						= true;
	public static final String			APP_NAME						= "Aircandi";

	public static final int				LAYER_GENERAL					= 0;
	public static final int				LAYER_ZONES						= 1;
	public static final int				LAYER_CANDI						= 2;

	public static final int				CANDI_VIEW_WIDTH				= 250;
	public static final int				CANDI_VIEW_SPACING				= 20;
	public static final int				CANDI_VIEW_BODY_HEIGHT			= 250;
	public static final int				CANDI_VIEW_TITLE_HEIGHT			= 100;
	public static final int				CANDI_VIEW_REFLECTION_HEIGHT	= 125;
	public static final int				CANDI_TITLEBAR_HEIGHT			= 63;
	public static final int				CANDI_VIEW_TITLE_SPACER_HEIGHT	= 2;
	public static final int				CANDI_VIEW_FONT_SIZE			= 24;
	public static final int				CANDI_VIEW_HEIGHT				= CANDI_VIEW_BODY_HEIGHT + CANDI_VIEW_TITLE_HEIGHT
																			+ CANDI_VIEW_TITLE_SPACER_HEIGHT;
	public static final int				CANDI_VIEW_ZOOMED_PADDING		= 10;

	public static final int				SWIPE_MAX_OFF_PATH				= 500;
	public static final float			SWIPE_SMALL_FLING				= 100;
	public static final long			TOUCH_MAX_TIME					= 500;

	public static final int				DIALOG_INSTALL_ID				= 1;
	public static final int				DIALOG_WIFIENABLE_ID			= 2;

	public static final int				VERTEX_INDEX_X					= 0;
	public static final int				VERTEX_INDEX_Y					= 1;

	public static final String			PATH_IMAGECACHE					= "/imagecache/aircandi";

	public static final int				TWO_MINUTES						= 1000 * 60 * 2;
	public static final int				FIVE_MINUTES					= 1000 * 60 * 5;

	public static final int				IMAGE_BYTES_MAX					= 500000;
	public static final Config			IMAGE_CONFIG_DEFAULT			= Config.ARGB_8888;

	public static final String			URL_AIRCANDI_SERVICE_ODATA		= "http://dev.aircandi.com/airodata.svc/";
	public static final String			URL_AIRCANDI_SERVICE			= "http://dev.aircandi.com/airlogic.asmx/";
	public static final String			URL_AIRCANDI_BLOG				= "http://devblog.proxibase.com/";
	public static final String			URL_AIRCANDI_MEDIA				= "https://s3.amazonaws.com/";
	public static final String			S3_BUCKET_IMAGES				= "3meters_images";

	public static final String			TYPE_CANDI_TOPIC				= "com.proxibase.aircandi.candi.topic";
	public static final String			TYPE_CANDI_WEB					= "com.proxibase.aircandi.candi.web";
	public static final String			TYPE_CANDI_POST					= "com.proxibase.aircandi.candi.post";

	public static final int				ACTIVITY_ENTITY_HANDLER			= 100;
	public static final int				ACTIVITY_MARKET					= 200;
	public static final int				ACTIVITY_PHOTO_PICK				= 300;
	public static final int				ACTIVITY_PHOTO_MAKE				= 310;
	public static final int				ACTIVITY_VIDEO_PICK				= 320;
	public static final int				ACTIVITY_VIDEO_MAKE				= 330;

	public static final int				VISIBILITY_PRIVATE				= 0;
	public static final int				VISIBILITY_PASSWORD_PROTECTED	= 1;
	public static final int				VISIBILITY_PUBLIC				= 2;

	public static final float			DURATION_MULTIPLIER				= 1.0f;
	public static final float			DURATION_ZOOM					= 0.5f * DURATION_MULTIPLIER;
	public static final float			DURATION_BOUNCEBACK				= 1.0f * DURATION_MULTIPLIER;
	public static final float			DURATION_SLOTTING_MAJOR			= 0.5f * DURATION_MULTIPLIER;
	public static final float			DURATION_SLOTTING_MINOR			= 0.5f * DURATION_MULTIPLIER;
	public static final float			DURATION_CANDIINFO_SHOW			= 0.4f * DURATION_MULTIPLIER;
	public static final float			DURATION_CANDIINFO_HIDE			= 0.4f * DURATION_MULTIPLIER;
	public static final float			DURATION_CANDIBODY_COLLAPSE		= 0.8f * DURATION_MULTIPLIER;
	public static final float			DURATION_CANDIBODY_EXPAND		= 0.8f * DURATION_MULTIPLIER;
	public static final float			DURATION_REFLECTION_HIDESHOW	= 0.8f * DURATION_MULTIPLIER;
	public static final float			DURATION_PLACEHOLDER_HIDESHOW	= 0.5f * DURATION_MULTIPLIER;
	public static final float			DURATION_TRANSITIONS_FADE		= 0.8f * DURATION_MULTIPLIER;
	public static final float			DURATION_TRANSITIONS_MOVE		= 0.8f * DURATION_MULTIPLIER;
	public static final float			DURATION_TRANSITIONS_DELAY		= 0.5f * DURATION_MULTIPLIER;

	public static final int				GL_BLEND_FUNCTION_SOURCE		= GL10.GL_ONE;
	public static final int				GL_BLEND_FUNCTION_DESTINATION	= GL10.GL_ONE_MINUS_SRC_ALPHA;
	public static final TextureOptions	GL_TEXTURE_OPTION				= TextureOptions.BILINEAR_PREMULTIPLYALPHA;

}
