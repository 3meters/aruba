package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Intent;

import com.aircandi.Constants;
import com.aircandi.components.AndroidManager;
import com.aircandi.service.Expose;
import com.aircandi.service.objects.Photo.PhotoSource;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Shortcut extends ServiceObject implements Cloneable, Serializable {

	private static final long						serialVersionUID	= 4979315562693226461L;
	public static final Map<String, ShortcutMeta>	shortcutMeta		= Collections.synchronizedMap(new HashMap<String, ShortcutMeta>());

	@Expose
	private String									id;
	@Expose
	public String									name;
	@Expose
	public String									schema;
	@Expose
	public String									app;			// usually maps to type property (if exists) on polymorphic entity like applinks 
	@Expose
	public String									appId;
	@Expose
	public String									appUrl;
	@Expose
	public Number									position;
	@Expose
	public Photo									photo;
	@Expose
	public AirLocation								location;
	@Expose
	public Boolean									content;
	@Expose
	public String									action;
	@Expose
	public Number									sortDate;

	/* client only properties */

	public Integer									count;
	public List<Shortcut>							group;
	public Boolean									synthetic			= false;
	public Boolean									inactive			= false;
	public String									linkType;		// so we know if entity shortcut represents is targeted via like/watch/content etc.
	public Intent									intent;

	public Shortcut() {}

	// --------------------------------------------------------------------------------------------
	// Copy and serialization
	// --------------------------------------------------------------------------------------------

	public static Shortcut setPropertiesFromMap(Shortcut shortcut, Map map, Boolean nameMapping) {
		/*
		 * Need to include any properties that need to survive encode/decoded between activities.
		 */
		shortcut.id = (String) map.get("id");
		shortcut.name = (String) map.get("name");
		shortcut.sortDate = (Number) map.get("sortDate");
		shortcut.schema = (String) map.get("schema");
		shortcut.app = (String) map.get("app");
		shortcut.appId = (String) map.get("appId");
		shortcut.appUrl = (String) map.get("appUrl");
		shortcut.position = (Number) map.get("position");
		shortcut.content = (Boolean) map.get("content");
		shortcut.action = (String) map.get("action");
		shortcut.synthetic = (Boolean) (map.get("synthetic") != null ? map.get("synthetic") : false);

		if (map.get("photo") != null) {
			shortcut.photo = Photo.setPropertiesFromMap(new Photo(), (HashMap<String, Object>) map.get("photo"), nameMapping);
		}

		if (map.get("location") != null) {
			shortcut.location = AirLocation.setPropertiesFromMap(new AirLocation(), (HashMap<String, Object>) map.get("location"), nameMapping);
		}

		return shortcut;
	}

	public static Shortcut builder(Entity entity, String schema, String type, String action, String name, String image, Integer position, Boolean content,
			Boolean synthetic) {

		Shortcut shortcut = new Shortcut()
				.setAppId(entity.id + "." + type)
				.setSchema(schema)
				.setApp(type)
				.setName(name)
				.setPhoto(new Photo(image, null, null, null, PhotoSource.resource))
				.setPosition(position)
				.setSynthetic(synthetic)
				.setContent(content)
				.setAction(action);

		return shortcut;
	}

	@Override
	public Shortcut clone() {
		try {
			final Shortcut shortcut = (Shortcut) super.clone();

			if (photo != null) {
				shortcut.photo = photo.clone();
			}

			return shortcut;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	public Photo getPhoto() {
		Photo photo = this.photo;
		if (photo == null) {
			photo = getDefaultPhoto();
		}
		photo.photoPlaceholder = getPlaceholderPhoto();
		photo.photoBroken = getBrokenPhoto();
		return photo;
	}

	public Photo getDefaultPhoto() {
		String prefix = "resource:img_placeholder_logo_bw";
		String source = PhotoSource.resource;
		if (this.schema != null && this.schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
			prefix = this.app.toLowerCase(Locale.US) + ".png";
			source = PhotoSource.assets_applinks;
		}
		Photo photo = new Photo(prefix, null, null, null, source);
		return photo;
	}

	public Photo getPlaceholderPhoto() {
		return getDefaultPhoto();
	}

	public Photo getBrokenPhoto() {
		String prefix = "resource:img_broken";
		String source = PhotoSource.resource;
		Photo photo = new Photo(prefix, null, null, null, source);
		return photo;
	}

	public Boolean getIntentSupport() {
		Boolean intentSupport = (app.equals(Constants.TYPE_APP_FOURSQUARE)
				|| app.equals(Constants.TYPE_APP_TRIPADVISOR)
				|| app.equals(Constants.TYPE_APP_TWITTER)
				|| app.equals(Constants.TYPE_APP_YELP));
		return intentSupport;
	}

	public String getPackageName() {
		String packageName = null;
		if (schema.equals("facebook")) {
			packageName = "com.facebook.katana";
		}
		else if (schema.equals("twitter")) {
			packageName = "com.twitter.android";
		}
		return packageName;
	}

	public Boolean appExists() {
		return (getPackageName() != null);
	}

	public Boolean appInstalled() {
		String packageName = getPackageName();
		final Boolean exists = AndroidManager.getInstance().doesPackageExist(packageName);
		return exists;
	}

	public Boolean isActive(Entity entity) {
		if (this.app.equals(Constants.TYPE_APP_MAP)) {
			if (entity.getLocation() == null) {
				return false;
			}
		}
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Set/get
	// --------------------------------------------------------------------------------------------

	public String getName() {
		return name;
	}

	public Shortcut setName(String name) {
		this.name = name;
		return this;
	}

	public String getAppId() {
		return appId;
	}

	public Shortcut setAppId(String appId) {
		this.appId = appId;
		return this;
	}

	public String getApp() {
		return app;
	}

	public Shortcut setApp(String app) {
		this.app = app;
		return this;
	}

	public String getAppUrl() {
		return appUrl;
	}

	public Shortcut setAppUrl(String appUrl) {
		this.appUrl = appUrl;
		return this;
	}

	public Shortcut setPhoto(Photo photo) {
		this.photo = photo;
		return this;
	}

	public Number getPosition() {
		return position != null ? position : 0;
	}

	public Shortcut setPosition(Number position) {
		this.position = position;
		return this;
	}

	public String getSchema() {
		return schema;
	}

	public Shortcut setSchema(String schema) {
		this.schema = schema;
		return this;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public Boolean isSynthetic() {
		return synthetic;
	}

	public Shortcut setSynthetic(Boolean synthetic) {
		this.synthetic = synthetic;
		return this;
	}

	public Boolean isContent() {
		return (content == null ? true : content);
	}

	public Shortcut setContent(Boolean content) {
		this.content = content;
		return this;
	}

	public String getAction() {
		return action;
	}

	public Shortcut setAction(String action) {
		this.action = action;
		return this;
	}

	public String getId() {
		return id;
	}

	public Shortcut setId(String id) {
		this.id = id;
		return this;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public static class SortByPositionSortDate implements Comparator<Shortcut> {

		@Override
		public int compare(Shortcut object1, Shortcut object2) {

			if (object1.getPosition().intValue() < object2.getPosition().intValue()) {
				return -1;
			}
			else if (object1.getPosition().intValue() == object2.getPosition().intValue()) {
				if (object1.sortDate == null || object2.sortDate == null) {
					return 0;
				}
				else {
					if (object1.sortDate.longValue() < object2.sortDate.longValue()) {
						return 1;
					}
					else if (object1.sortDate.longValue() == object2.sortDate.longValue()) {
						return 0;
					}
					return -1;
				}
			}
			return 1;
		}
	}
}