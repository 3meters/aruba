package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aircandi.Constants;
import com.aircandi.components.AndroidManager;
import com.aircandi.service.Expose;
import com.aircandi.service.objects.Link.Direction;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Shortcut extends ServiceObject implements Cloneable, Serializable {

	private static final long						serialVersionUID	= 4979315562693226461L;
	public static final Map<String, ShortcutMeta>	shortcutMeta		= Collections.synchronizedMap(new HashMap<String, ShortcutMeta>());

	@Expose
	public String									id;
	@Expose
	public String									name;
	@Expose
	private String									type;
	@Expose
	public String									app;
	@Expose
	public String									appId;
	@Expose
	public String									appUrl;
	@Expose
	public Number									position;
	@Expose
	public Photo									photo;

	/* Client only properties */

	public Integer									count;
	public List<Shortcut>							group;
	public Entity									entity;
	private Boolean									synthetic			= false;

	public Shortcut() {}

	// --------------------------------------------------------------------------------------------
	// Copy and serialization
	// --------------------------------------------------------------------------------------------

	public static Shortcut setPropertiesFromMap(Shortcut shortcut, Map map, Boolean nameMapping) {

		shortcut.id = (String) map.get("id");
		shortcut.name = (String) map.get("name");
		shortcut.type = (String) map.get("type");
		shortcut.app = (String) map.get("app");
		shortcut.appId = (String) map.get("appId");
		shortcut.appUrl = (String) map.get("appUrl");
		shortcut.position = (Number) map.get("position");

		if (map.get("photo") != null) {
			shortcut.photo = Photo.setPropertiesFromMap(new Photo(), (HashMap<String, Object>) map.get("photo"), nameMapping);
		}
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

	public Boolean getIntentSupport() {
		Boolean intentSupport = (app.equals(Constants.TYPE_APPLINK_FOURSQUARE)
				|| app.equals(Constants.TYPE_APPLINK_TRIPADVISOR)
				|| app.equals(Constants.TYPE_APPLINK_TWITTER)
				|| app.equals(Constants.TYPE_APPLINK_YELP));
		return intentSupport;
	}

	public String getPackageName() {
		String packageName = null;
		if (type.equals("facebook")) {
			packageName = "com.facebook.katana";
		}
		else if (type.equals("twitter")) {
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
		if (this.app.equals(Constants.TYPE_APPLINK_MAP)) {
			if (entity.getLocation() == null) {
				return false;
			}
		}
		else if (this.app.equals(Constants.TYPE_APPLINK_LIKE)
				|| this.app.equals(Constants.TYPE_APPLINK_WATCH)) {
			Count count = entity.getCount(app, Direction.in);
			if (count == null || count.count.intValue() < 1) {
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

	public Photo getPhoto() {
		return photo;
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

	public String getType() {
		return type;
	}

	public Shortcut setType(String type) {
		this.type = type;
		return this;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public Boolean getSynthetic() {
		return synthetic;
	}

	public Shortcut setSynthetic(Boolean synthetic) {
		this.synthetic = synthetic;
		return this;
	}

	public enum IconStyle {
		normal,
		inset,
	}

	public static class SortByPosition implements Comparator<Shortcut> {

		@Override
		public int compare(Shortcut entity1, Shortcut entity2) {
			if (entity1.getPosition().intValue() < entity2.getPosition().intValue()) {
				return -1;
			}
			if (entity1.getPosition().intValue() == entity2.getPosition().intValue()) {
				return 0;
			}
			return 1;
		}
	}

}