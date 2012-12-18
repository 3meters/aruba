package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;

import android.graphics.Bitmap;

import com.aircandi.service.Expose;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.objects.Entity.ImageFormat;

/**
 * @author Jayma
 */
public class Photo extends ServiceObject implements Cloneable, Serializable {
	/*
	 * format: binary, html
	 * sourceName: aircandi, foursquare, external
	 */
	private static final long	serialVersionUID	= 4979315562693226461L;

	@Expose
	protected String			prefix				= "resource:placeholder_logo";
	@Expose
	protected String			suffix;
	@Expose
	protected Number			width;
	@Expose
	protected Number			height;
	@Expose
	protected String			format				= "binary";
	@Expose
	protected String			sourceName			= "aircandi";
	@Expose
	protected Number			createdAt;
	@Expose
	protected Photo				detail;

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

			if (this.detail != null) {
				photo.detail = this.detail.clone();
			}

			if (this.user != null) {
				photo.user = this.user.clone();
			}

			return photo;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public Photo(String pImageUri, String pTitle, Number pCreatedAt, User pUser) {
		prefix = pImageUri;
		createdAt = pCreatedAt;
		title = pTitle;
		user = pUser;
	}

	public static Photo setPropertiesFromMap(Photo photo, HashMap map) {

		photo.prefix = (String) map.get("prefix");
		photo.suffix = (String) map.get("suffix");
		photo.width = (Number) map.get("width");
		photo.height = (Number) map.get("height");
		photo.format = (String) map.get("format");
		photo.sourceName = (String) map.get("sourceName");
		photo.createdAt = (Number) map.get("createdAt");

		if (map.get("detail") != null) {
			photo.detail = (Photo) Photo.setPropertiesFromMap(new Photo(), (HashMap<String, Object>) map.get("detail"));
		}

		if (map.get("user") != null) {
			photo.user = (User) User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("user"));
		}

		return photo;
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

	public Boolean isEmpty() {
		if (prefix == null || prefix.equals("") || prefix.equals("resource:placeholder_logo")) {
			return true;
		}
		return false;
	}

	public Boolean hasDetail() {
		if (detail == null || detail.isEmpty()) {
			return false;
		}
		return true;
	}

	public String getImageUri() {
		String imageUri = prefix;
		if (suffix != null) {
			if (width != null && height != null) {
				imageUri = prefix + String.valueOf(width.intValue()) + "x" + String.valueOf(height.intValue()) + suffix;
			}
			else {
				imageUri = prefix + suffix;
			}
		}
		if (imageUri != null && !imageUri.startsWith("resource:") && sourceName.equals("aircandi")) {
			imageUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + imageUri;
		}
		return imageUri;
	}

	public String getImageSizedUri(Number pWidth, Number pHeight) {
		String imageUri = prefix;
		if (prefix != null && suffix != null) {
			imageUri = prefix + String.valueOf(pWidth) + "x" + String.valueOf(pHeight) + suffix;
		}
		if (imageUri != null 
				&& !imageUri.startsWith("resource:") 
				&& sourceName != null 
				&& sourceName.equals("aircandi")) {
			imageUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + imageUri;
		}
		return imageUri;
	}

	public String getFormat() {
		return this.format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public ImageFormat getImageFormat() {
		ImageFormat imageFormat = ImageFormat.Binary;
		if (format != null && format.equals("html")) {
			imageFormat = ImageFormat.Html;
		}
		return imageFormat;
	}

	public void setImageFormat(ImageFormat imageFormat) {
		if (imageFormat == ImageFormat.Binary) {
			this.format = "binary";
		}
		else if (imageFormat == ImageFormat.Html) {
			this.format = "html";
		}
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

	public Photo getDetail() {
		if (detail == null) {
			detail = new Photo();
		}
		return detail;
	}

	public void setDetail(Photo detail) {
		this.detail = detail;
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}
}