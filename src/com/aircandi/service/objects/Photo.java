package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;

import com.aircandi.ProxiConstants;
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
	protected String			prefix;
	@Expose
	protected String			suffix;
	@Expose
	protected Number			width;
	@Expose
	protected Number			height;
	@Expose
	protected String			sourceName;
	@Expose
	protected Number			createdAt;

	/* Only comes from foursquare */
	@Expose(serialize = false, deserialize = true)
	private User				user;

	/* client only */
	private String				title;
	@SuppressWarnings("unused")
	private String				description;
	private Bitmap				bitmap;

	public Photo() {}

	public Photo(String prefix, String suffix, Number width, Number height, String sourceName) {
		this.prefix = prefix;
		this.suffix = suffix;
		this.width = width;
		this.height = height;
		this.sourceName = sourceName;
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

	public static Photo setPropertiesFromMap(Photo photo, Map map) {

		photo.prefix = (String) map.get("prefix");
		photo.suffix = (String) map.get("suffix");
		photo.width = (Number) map.get("width");
		photo.height = (Number) map.get("height");
		photo.sourceName = (String) map.get("sourceName");
		photo.createdAt = (Number) map.get("createdAt");

		if (map.get("user") != null) {
			photo.user = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("user"));
		}

		return photo;
	}

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
		String imageUri = prefix;
		if (suffix != null) {
			if (width != null && height != null) {
				imageUri = prefix + String.valueOf(width.intValue()) + "x" + String.valueOf(height.intValue()) + suffix;
			}
			else {
				imageUri = prefix + suffix;
			}
		}
		if (imageUri != null && !imageUri.startsWith("resource:")) {
			if (sourceName != null) {
				if (sourceName.equals(PhotoSource.aircandi)) {
					imageUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + imageUri;
				}
				else if (sourceName.equals(PhotoSource.assets)) {
					imageUri = ProxiConstants.URL_PROXIBASE_SERVICE + imageUri;
				}
			}
		}
		return imageUri;
	}

	public String getSizedUri(Number pWidth, Number pHeight) {
		String imageUri = prefix;
		if (prefix != null && suffix != null) {
			imageUri = prefix + String.valueOf(pWidth) + "x" + String.valueOf(pHeight) + suffix;
		}
		if (imageUri != null && !imageUri.startsWith("resource:") && sourceName != null) {
			if (sourceName.equals(PhotoSource.aircandi)) {
				imageUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + imageUri;
			}
		}
		return imageUri;
	}

	public Number getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Number createdAt) {
		this.createdAt = createdAt;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public static class PhotoSource {
		public static String	external	= "external";
		public static String	aircandi	= "aircandi";
		public static String	assets		= "assets";
		public static String	foursquare	= "foursquare";
		public static String	facebook	= "facebook";
		public static String	twitter		= "twitter";
		public static String	resource	= "resource";
	}
}