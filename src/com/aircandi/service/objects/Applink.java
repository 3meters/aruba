package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.EntityManager;
import com.aircandi.service.Expose;
import com.aircandi.service.objects.Photo.PhotoSource;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Applink extends Entity implements Cloneable, Serializable {

	private static final long						serialVersionUID	= 4362288672245819448L;
	public static final String						collectionId		= "applinks";
	public static final Map<String, ApplinkMeta>	applinkMeta			= Collections.synchronizedMap(new HashMap<String, ApplinkMeta>());

	// --------------------------------------------------------------------------------------------
	// Service fields
	// --------------------------------------------------------------------------------------------

	@Expose
	public String									appId;
	@Expose
	public String									url;

	public Applink() {}

	// --------------------------------------------------------------------------------------------
	// Set and get
	// --------------------------------------------------------------------------------------------	

	public static Boolean getIntentSupport(String type) {
		Boolean intentSupport = (type.equals(Constants.TYPE_APPLINK_FOURSQUARE)
				|| type.equals(Constants.TYPE_APPLINK_TRIPADVISOR)
				|| type.equals(Constants.TYPE_APPLINK_TWITTER)
				|| type.equals(Constants.TYPE_APPLINK_YELP));
		return intentSupport;
	}

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

	@Override
	public List<Applink> getApplinks() {
		return new ArrayList<Applink>();
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
	
	public static Applink builder(Entity entity, String type, String name, String image, Integer count) {
		
		final Applink applink = (Applink) EntityManager.getInstance().loadEntityFromResources(R.raw.applink_entity);
		applink.id = entity.id + "." + type;
		applink.name = name;
		applink.photo = new Photo(image, null, null, null, PhotoSource.resource);
		applink.type = type;
		applink.synthetic = true;
		applink.tagSecondary = String.valueOf(count);
		applink.toId = entity.id;
		return applink;
	}
}