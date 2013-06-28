package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.EntityManager;
import com.aircandi.service.Expose;
import com.aircandi.service.HttpService.ServiceDataType;
import com.aircandi.service.objects.Photo.PhotoSource;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Applink extends Entity implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 4362288672245819448L;
	public static final String	collectionId		= "applinks";

	// --------------------------------------------------------------------------------------------
	// Service fields
	// --------------------------------------------------------------------------------------------

	@Expose
	public String				appId;
	@Expose
	public String				appUrl;

	public Applink() {}

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
			Photo photo = new Photo(getDefaultPhoto(type), null, null, null, PhotoSource.assets);
			imageUri = photo.getUri();
		}
		return imageUri;
	}

	public static String getDefaultPhoto(String sourceType) {
		String icon = ProxiConstants.PATH_PROXIBASE_SERVICE_ASSETS_SOURCE_ICONS + sourceType + ".png";
		return icon;
	}

	@Override
	public String getCollection() {
		return collectionId;
	}

	@Override
	public List<Applink> getClientApplinks() {
		return new ArrayList<Applink>();
	}

	@Override
	public Shortcut getShortcut() {

		Shortcut shortcut = super.getShortcut();
		shortcut.setApp(type != null ? type : null)
				.setAppId(appId != null ? appId : null)
				.setAppUrl(appUrl != null ? appUrl : null);

		return shortcut;
	}

	// --------------------------------------------------------------------------------------------
	// Copy and serialization
	// --------------------------------------------------------------------------------------------

	public static Applink setPropertiesFromMap(Applink entity, Map map, Boolean nameMapping) {

		synchronized (entity) {
			entity = (Applink) Entity.setPropertiesFromMap(entity, map, nameMapping);
			entity.appId = (String) map.get("appId");
			entity.appUrl = (String) map.get("appUrl");
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

	public static Applink builder(Entity entity, String type, String name, String image, Boolean synthetic) {

		final Applink applink = (Applink) EntityManager.getInstance().loadEntityFromResources(R.raw.applink_entity, ServiceDataType.Applink);
		applink.id = entity.id + "." + type;
		applink.schema = Constants.SCHEMA_ENTITY_APPLINK;
		applink.type = type;
		applink.name = name;
		applink.photo = new Photo(image, null, null, null, PhotoSource.resource);
		applink.synthetic = true;
		applink.toId = entity.id;
		applink.synthetic = synthetic;
		return applink;
	}
}