package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Map;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.service.Expose;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.utilities.Json;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Applink extends Entity implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 4362288672245819448L;
	public static final String	collectionId		= "applinks";

	// --------------------------------------------------------------------------------------------
	// service fields
	// --------------------------------------------------------------------------------------------

	@Expose
	public String				appId;
	@Expose
	public String				appUrl;
	@Expose
	public String				origin;
	@Expose
	public String				originId;
	@Expose
	public Number				validatedDate;
	@Expose
	public Number				popularity;

	public Applink() {}

	// --------------------------------------------------------------------------------------------
	// Set and get
	// --------------------------------------------------------------------------------------------
	
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
			entity.origin = (String) map.get("origin");
			entity.originId = (String) map.get("originId");
			entity.validatedDate = (Number) map.get("validatedDate");
			entity.popularity = (Number) map.get("popularity");
		}
		return entity;
	}

	@Override
	public Applink clone() {
		final Applink entity = (Applink) super.clone();
		return entity;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------	

	public static Applink builder(Entity entity, String type, String name, String image, Boolean synthetic) {

		final Applink applink = (Applink) EntityManager.getInstance().loadEntityFromResources(R.raw.applink_entity, Json.ObjectType.APPLINK);
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