package com.proxibase.service.consumer;

import java.util.Date;
import java.util.List;

import android.graphics.Bitmap;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.proxibase.service.util.ProxiConstants;

/**
 * Entity as described by the proxi protocol standards.
 * 
 * @author Jayma
 */
public class Entity extends ServiceEntry {

	protected String	mServiceUri	= ProxiConstants.URL_PROXIBASE_SERVICE;

	/* Annotation syntax: @Expose (serialize = false, deserialize = false) */

	/* Database fields */

	@Expose
	@SerializedName("_entity")
	public String		parentEntityId;											// id

	@Expose(serialize = false, deserialize = true)
	@SerializedName("entity")
	public Entity		parentEntity;

	@Expose
	public String		type;												// required
	@Expose
	public String		uri;
	@Expose
	public String		label;
	@Expose
	public String		title;
	@Expose
	public String		subtitle;
	@Expose
	public String		description;
	@Expose
	public String		imageUri;
	@Expose
	public String		imagePreviewUri;
	@Expose
	public String		linkUri;
	@Expose
	public Boolean		linkZoom;
	@Expose
	public Boolean		linkJavascriptEnabled;
	@Expose
	public Number		signalFence	= -200f;
	@Expose
	public Boolean		locked;
	@Expose
	public Boolean		enabled;
	@Expose
	public String		visibility;

	/* Non database fields */

	@Expose(serialize = false, deserialize = true)
	public List<Drop>	comments;

	@Expose(serialize = false, deserialize = true)
	public Integer		commentsCount;

	@Expose(serialize = false, deserialize = true)
	public List<Drop>	drops;

	@Expose(serialize = false, deserialize = true)
	public Integer		dropsCount;

	@Expose(serialize = false, deserialize = true)
	public List<Entity>	entities;

	@Expose(serialize = false, deserialize = true)
	public Integer		entitiesCount;

	/* For client use only */

	public Beacon		beacon;
	public Boolean		hidden		= false;
	public Boolean		dirty		= false;
	public Boolean		rookie		= true;
	public EntityState	state		= EntityState.Normal;
	public Date			discoveryTime;
	public Bitmap		imageBitmap;

	public Entity() {}

	public String getCollection() {
		return "entities";
	}

	public boolean hasVisibleChildren() {
		for (Entity childEntity : this.entities) {
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