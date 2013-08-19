package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Map;

import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.service.Expose;
import com.aircandi.service.HttpService.ObjectType;
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
	
	public static Photo getDefaultPhoto(String type) {
		String photoUri = ProxiConstants.PATH_PROXIBASE_SERVICE_ASSETS_SOURCE_ICONS + type + ".png";
		Photo photo = new Photo(photoUri, null, null, null, PhotoSource.assets);
		return photo;
	}

	@Override
	public Photo getDefaultPhoto() {
		String photoUri = ProxiConstants.PATH_PROXIBASE_SERVICE_ASSETS_SOURCE_ICONS + this.type + ".png";
		Photo photo = new Photo(photoUri, null, null, null, PhotoSource.assets);
		return photo;
	}

	public static String getDefaultPhotoUri(String sourceType) {
		String photoUri = ProxiConstants.PATH_PROXIBASE_SERVICE_ASSETS_SOURCE_ICONS + sourceType + ".png";
		return photoUri;
	}

	@Override
	public String getCollection() {
		return collectionId;
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

	public static Applink builder(Entity entity, String type, String name, String image, Boolean synthetic) {

		final Applink applink = (Applink) EntityManager.getInstance().loadEntityFromResources(R.raw.applink_entity, ObjectType.Applink);
		applink.id = entity.id + "." + type;
		applink.schema = Constants.SCHEMA_ENTITY_APPLINK;
		applink.type = type;
		applink.name = name;
		applink.photo = new Photo(image, null, null, null, PhotoSource.resource);
		applink.toId = entity.id;
		applink.synthetic = synthetic;
		return applink;
	}
}