package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;

import com.aircandi.ProxiConstants;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.ImageResult;
import com.aircandi.components.bitmaps.ImageResult.Thumbnail;
import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Photo extends ServiceObject implements Cloneable, Serializable {
	/*
	 * sourceName: aircandi, foursquare, external
	 */
	private static final long	serialVersionUID	= 4979315562693226461L;

	@Expose
	public String				prefix;
	@Expose
	public String				suffix;
	@Expose
	public Number				width;
	@Expose
	public Number				height;
	@Expose
	public String				source;
	@Expose
	public Number				createdDate;
	@Expose(serialize = false, deserialize = true)
	public Boolean				colorize			= false;
	@Expose(serialize = false, deserialize = true)
	public String				colorizeKey;
	@Expose(serialize = false, deserialize = true)
	public Integer				color;

	/* Only comes from foursquare */
	@Expose(serialize = false, deserialize = true)
	public Entity				user;

	/* client only */
	public String				name;
	public Boolean				usingDefault = false;

	/* Used to stash temp bitmaps. Always access using set/getBitmap() */
	protected String			bitmapKey;
	protected Boolean			bitmapLocalOnly		= false;

	public Photo() {}

	public Photo(String prefix, String suffix, Number width, Number height, String sourceName) {
		this.prefix = prefix;
		this.suffix = suffix;
		this.width = width;
		this.height = height;
		this.source = sourceName;
	}

	@Override
	public Photo clone() {
		try {
			final Photo photo = (Photo) super.clone();

			if (user != null) {
				photo.user = user.clone();
			}

			return photo;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public static Photo setPropertiesFromMap(Photo photo, Map map, Boolean nameMapping) {

		photo.prefix = (String) map.get("prefix");
		photo.suffix = (String) map.get("suffix");
		photo.width = (Number) map.get("width");
		photo.height = (Number) map.get("height");
		photo.source = (String) map.get("source");
		photo.createdDate = (Number) map.get("createdDate");
		photo.name = (String) map.get("name");
		photo.color = (Integer) map.get("color");
		photo.colorize = (Boolean) map.get("colorize");
		photo.colorizeKey = (String) map.get("colorizeKey");
		photo.usingDefault = (Boolean) map.get("usingDefault");
		photo.bitmapKey = (String) map.get("bitmapKey");
		photo.bitmapLocalOnly = (Boolean) map.get("bitmapLocalOnly");

		if (map.get("user") != null) {
			photo.user = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("user"), nameMapping);
		}

		return photo;
	}

	// --------------------------------------------------------------------------------------------
	// Bitmap routines
	// --------------------------------------------------------------------------------------------

	public Boolean hasBitmap() {
		if (bitmapKey != null) {
			return (BitmapManager.getInstance().getBitmapFromMemoryCache(bitmapKey) != null);
		}
		return false;
	}

	public Bitmap getBitmap() {
		if (bitmapKey != null) {
			return BitmapManager.getInstance().getBitmapFromMemoryCache(bitmapKey);
		}
		return null;
	}

	public void removeBitmap() {
		if (hasBitmap()) {
			Bitmap bitmap = BitmapManager.getInstance().removeBitmapFromMemoryCache(bitmapKey);
			bitmap.recycle();
			bitmap = null;
		}
		bitmapKey = null;
		bitmapLocalOnly = false;
	}

	public void setBitmap(String key, Bitmap bitmap) {
		BitmapManager.getInstance().putBitmapInMemoryCache(key, bitmap);
		bitmapKey = key;
	}

	// --------------------------------------------------------------------------------------------
	// Set/get routines
	// --------------------------------------------------------------------------------------------

	public ImageResult getAsImageResult() {
		final ImageResult imageResult = new ImageResult();
		if (width != null) {
			imageResult.setWidth(width.longValue());
		}
		if (height != null) {
			imageResult.setHeight(height.longValue());
		}
		imageResult.setMediaUrl(getUri());
		final Thumbnail thumbnail = new Thumbnail();
		thumbnail.setUrl(getSizedUri(100, 100));
		imageResult.setThumbnail(thumbnail);
		return imageResult;
	}

	public String getUri() {
		String photoUri = prefix;
		if (suffix != null) {
			if (width != null && height != null) {
				photoUri = prefix + String.valueOf(width.intValue()) + "x" + String.valueOf(height.intValue()) + suffix;
			}
			else {
				photoUri = getSizedUri(250, 250);
			}
		}

		if (photoUri != null && !photoUri.startsWith("resource:")) {
			if (source != null) {
				if (source.equals(PhotoSource.aircandi)) {
					photoUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + photoUri;
				}
				else if (source.equals(PhotoSource.assets)) {
					photoUri = ProxiConstants.URL_PROXIBASE_SERVICE + photoUri;
				}
				else if (source.equals(PhotoSource.assets_categories)) {
					photoUri = ProxiConstants.URL_PROXIBASE_SERVICE + photoUri;
				}
			}
		}
		return photoUri;
	}

	public String getSizedUri(Number pWidth, Number pHeight) {
		String photoUri = prefix;
		if (prefix != null && suffix != null) {
			photoUri = prefix + String.valueOf(pWidth) + "x" + String.valueOf(pHeight) + suffix;
		}
		if (photoUri != null && !photoUri.startsWith("resource:") && source != null) {
			if (source.equals(PhotoSource.aircandi)) {
				photoUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + photoUri;
			}
		}
		return photoUri;
	}

	public Number getCreatedAt() {
		return createdDate;
	}

	public void setCreatedAt(Number createdAt) {
		this.createdDate = createdAt;
	}

	public Entity getUser() {
		return user;
	}

	public void setUser(Entity user) {
		this.user = user;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	// --------------------------------------------------------------------------------------------
	// Bitmap routines
	// --------------------------------------------------------------------------------------------

	public String getSourceName() {
		return source;
	}

	public void setSourceName(String sourceName) {
		this.source = sourceName;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public Boolean isBitmapLocalOnly() {
		return bitmapLocalOnly;
	}

	public void setBitmapLocalOnly(Boolean bitmapLocalOnly) {
		this.bitmapLocalOnly = bitmapLocalOnly;
	}

	public static class PhotoSource {
		public static String	external			= "external";
		public static String	aircandi			= "aircandi";
		public static String	assets				= "assets";
		public static String	assets_categories	= "assets.categories";
		public static String	foursquare			= "foursquare";
		public static String	facebook			= "facebook";
		public static String	twitter				= "twitter";
		public static String	resource			= "resource";
		public static String	cache				= "cache";
	}
}