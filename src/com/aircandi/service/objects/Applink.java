package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Map;

import com.aircandi.ProxiConstants;
import com.aircandi.components.AndroidManager;
import com.aircandi.service.Copy;
import com.aircandi.service.Expose;
import com.aircandi.service.objects.Photo.PhotoSource;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Applink extends Entity implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 4362288672245819448L;
	public static final String	collectionId		= "applinks";

	@Expose
	public String				appId;
	@Expose
	public String				url;

	/* Client use only */
	@Copy(exclude = true)
	public Integer				count;			// Can be displayed in icon badge.
	@Copy(exclude = true)
	public Boolean				custom;
	@Copy(exclude = true)
	public Boolean				intentSupport;
	@Copy(exclude = true)
	public Boolean				installDeclined;

	public Applink() {}

	public Applink(Boolean intentSupport, Boolean installDeclined) {
		this.intentSupport = intentSupport;
		this.installDeclined = installDeclined;
	}

	// --------------------------------------------------------------------------------------------
	// Set and get
	// --------------------------------------------------------------------------------------------	

	@Override
	public String getPhotoUri() {
		String imageUri;
		if (photo != null) {
			imageUri = photo.getUri();
		}
		else {
			Photo photo = new Photo(getDefaultIcon(type), null, null, null, PhotoSource.assets);
			imageUri = photo.getUri();
		}
		return imageUri;
	}

	public static String getDefaultIcon(String sourceType) {
		String icon = ProxiConstants.PATH_PROXIBASE_SERVICE_ASSETS_SOURCE_ICONS + sourceType + ".png";
		return icon;
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

	@Override
	public String getCollection() {
		return collectionId;
	}

	// --------------------------------------------------------------------------------------------
	// Copy and serialization
	// --------------------------------------------------------------------------------------------

	public static Applink setPropertiesFromMap(Applink entity, Map map) {
		
		synchronized (entity) {
			entity = (Applink) Entity.setPropertiesFromMap(entity, map);
			entity.appId = (String) map.get("appId");
			entity.url = (String) map.get("url");
		}
		return entity;
	}

	@Override
	public Applink clone() {
		final Applink entity = (Applink) super.clone();
		return entity;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------	

	public static class SourceType {
		public static String	foursquare	= "foursquare";
		public static String	facebook	= "facebook";
		public static String	twitter		= "twitter";
		public static String	website		= "website";
	}
}