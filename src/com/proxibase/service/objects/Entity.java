package com.proxibase.service.objects;

import java.util.Date;
import java.util.List;

import android.graphics.Bitmap;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.proxibase.service.ProxiConstants;

/**
 * Entity as described by the proxi protocol standards.
 * 
 * @author Jayma
 */
public class Entity extends ServiceEntry {

	protected String		mServiceUri	= ProxiConstants.URL_PROXIBASE_SERVICE;

	/* Annotation syntax: @Expose (serialize = false, deserialize = false) */

	/* Database fields */

	@Expose
	public String			type;
	@Expose
	public Boolean			root;
	@Expose
	public String			uri;
	@Expose
	public String			label;
	@Expose
	public String			title;
	@Expose
	public String			subtitle;
	@Expose
	public String			description;
	@Expose
	public String			imageUri;
	@Expose
	public String			imagePreviewUri;
	@Expose
	public String			linkUri;
	@Expose
	public Boolean			linkZoom;
	@Expose
	public Boolean			linkJavascriptEnabled;
	@Expose
	public Number			signalFence	= -200f;
	@Expose
	public Boolean			locked;
	@Expose
	public Boolean			enabled;
	@Expose
	public String			visibility;
	@Expose
	public List<Comment>	comments;

	/* Synthetic service fields */

	@Expose(serialize = false, deserialize = true)
	@SerializedName("_beacon")
	public String			beaconId;											/* Used to connect beacon object */

	@Expose(serialize = false, deserialize = true)
	public GeoLocation		location;

	@Expose(serialize = false, deserialize = true)
	public Integer			commentsCount;

	@Expose(serialize = false, deserialize = true)
	public List<Entity>		children;

	@Expose(serialize = false, deserialize = true)
	public Integer			childrenCount;

	/* For client use only */

	public Beacon			beacon;
	public Entity			parent;
	/* So we don't have to serialize the parent across activities */	
	public String			parentId;											

	public Boolean			hidden		= false;
	public Boolean			dirty		= false;
	public Boolean			rookie		= true;
	public EntityState		state		= EntityState.Normal;
	public Date				discoveryTime;
	public Bitmap			imageBitmap;

	public Entity() {}

	public String getCollection() {
		return "entities";
	}

	public boolean hasVisibleChildren() {
		for (Entity childEntity : this.children) {
			if (!childEntity.hidden) {
				return true;
			}
		}
		return false;
	}

	public String getMasterImageUri() {
		String masterImageUri = null;
		if (imagePreviewUri != null && !imagePreviewUri.equals("")) {
			masterImageUri = imagePreviewUri;
		}
		else if (linkUri != null && !linkUri.equals("")) {
			masterImageUri = linkUri;
		}
		else if (creator != null) {
			if (creator.imageUri != null && !creator.imageUri.equals("")) {
				masterImageUri = creator.imageUri;
			}
			else if (creator.linkUri != null && !creator.linkUri.equals("")) {
				masterImageUri = creator.linkUri;
			}
		}
		return masterImageUri;
	}

	public ImageFormat getMasterImageFormat() {
		ImageFormat imageFormat = ImageFormat.Binary;
		if (imagePreviewUri != null && !imagePreviewUri.equals("")) {
			imageFormat = ImageFormat.Binary;
		}
		else if (linkUri != null && !linkUri.equals("")) {
			imageFormat = ImageFormat.Html;
		}
		else if (creator != null) {
			if (creator.imageUri != null && !creator.imageUri.equals("")) {
				imageFormat = ImageFormat.Binary;
			}
			else if (creator.linkUri != null && !creator.linkUri.equals("")) {
				imageFormat = ImageFormat.Html;
			}
		}
		return imageFormat;
	}

	public static enum ImageFormat {
		Binary, Html
	}

	public static enum EntityState {
		Normal,
		New,
		Refreshed,
		Missing
	}

	public static enum Visibility {
		Public,
		Private
	}
}