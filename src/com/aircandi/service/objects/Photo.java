package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;

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
	private Bitmap				bitmap;

	public Photo() {}

	@Override
	public Photo clone() {
		try {
			final Photo photo = (Photo) super.clone();

			if (this.user != null) {
				photo.user = this.user.clone();
			}

			return photo;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	

	public static Photo setPropertiesFromMap(Photo photo, HashMap map) {

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
		imageResult.setWidth(width.longValue());
		imageResult.setHeight(height.longValue());
		imageResult.setMediaUrl(getUri());
		final Thumbnail thumbnail = new Thumbnail();
		thumbnail.setUrl(getSizedUri(100, 100));
		imageResult.setThumbnail(thumbnail);
		return imageResult;
	}

	public void setImageUri(String imageUri) {
		setImageUri(imageUri, null, null, null);
	}

	public void setImageUri(String prefix, String suffix, Number width, Number height) {
		this.prefix = prefix;
		this.suffix = suffix;
		this.width = width;
		this.height = height;
		this.sourceName = "aircandi";
		if (prefix.startsWith("http:") || prefix.startsWith("https:")) {
			this.sourceName = "external";
		}
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
			if (sourceName.equals("aircandi")) {
				imageUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + imageUri;
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
			if (sourceName.equals("aircandi")) {
				imageUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + imageUri;
			}
		}
		return imageUri;
	}

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
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
}