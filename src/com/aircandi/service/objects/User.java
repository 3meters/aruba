package com.aircandi.service.objects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aircandi.ProxiConstants;
import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class User extends Entity {

	private static final long	serialVersionUID	= 127428776257201065L;
	public static final String	collectionId		= "users";

	// --------------------------------------------------------------------------------------------
	// Service fields
	// --------------------------------------------------------------------------------------------

	@Expose
	public String				email;										// Required
	@Expose
	public String				role;
	@Expose
	public String				area;
	@Expose
	public String				bio;
	@Expose
	public String				webUri;
	@Expose
	public Boolean				developer;
	@Expose
	public Boolean				doNotTrack;
	@Expose
	public String				password;

	@Expose(serialize = false, deserialize = true)
	public String				facebookId;
	@Expose(serialize = false, deserialize = true)
	public String				twitterId;
	@Expose(serialize = false, deserialize = true)
	public String				googleId;

	@Expose(serialize = false, deserialize = true)
	public String				authSource;

	@Expose(serialize = false, deserialize = true)
	public String				oauthId;
	@Expose(serialize = false, deserialize = true)
	public String				oauthToken;
	@Expose(serialize = false, deserialize = true)
	public String				oauthSecret;
	@Expose(serialize = false, deserialize = true)
	public String				oauthData;

	@Expose(serialize = false, deserialize = true)
	public Number				lastSignedInDate;
	@Expose(serialize = false, deserialize = true)
	public Number				validationDate;
	@Expose(serialize = false, deserialize = true)
	public Number				validationNotifyDate;

	@Expose(serialize = false, deserialize = true)
	public List<Stat>			stats;

	// --------------------------------------------------------------------------------------------
	// Client fields
	// --------------------------------------------------------------------------------------------

	public Session				session;

	public User() {}

	// --------------------------------------------------------------------------------------------
	// Set and get
	// --------------------------------------------------------------------------------------------		

	@Override
	public String getPhotoUri() {

		/*
		 * If a special preview photo is available, we use it otherwise
		 * we use the standard photo.
		 * 
		 * Only posts and collections do not have photo objects
		 */
		String imageUri = "resource:img_placeholder_logo_bw";
		if (photo != null) {
			imageUri = photo.getSizedUri(250, 250); // sizing ignored if source doesn't support it
			if (imageUri == null) {
				imageUri = photo.getUri();
			}
		}
		else {
			if (creator != null) {
				if (creator.getPhotoUri() != null && !creator.getPhotoUri().equals("")) {
					imageUri = creator.getPhotoUri();
				}
			}
			if (!imageUri.startsWith("http:")
					&& !imageUri.startsWith("https:")
					&& !imageUri.startsWith("resource:")) {
				imageUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + imageUri;
			}
		}

		return imageUri;
	}

	@Override
	public List<Applink> getApplinks() {
		return new ArrayList<Applink>();
	}

	@Override
	public String getCollection() {
		return collectionId;
	}

	// --------------------------------------------------------------------------------------------
	// Copy and serialization
	// --------------------------------------------------------------------------------------------

	public static User setPropertiesFromMap(User entity, Map map) {

		synchronized (entity) {
			entity = (User) Entity.setPropertiesFromMap(entity, map);

			entity.area = (String) map.get("area");
			entity.email = (String) map.get("email");
			entity.role = (String) map.get("role");
			entity.bio = (String) map.get("bio");
			entity.webUri = (String) map.get("webUri");
			entity.developer = (Boolean) map.get("developer");
			entity.doNotTrack = (Boolean) map.get("doNotTrack");
			entity.password = (String) map.get("password");
			entity.lastSignedInDate = (Number) map.get("lastSignedInDate");
			entity.validationDate = (Number) map.get("validationDate");
			entity.validationNotifyDate = (Number) map.get("validationNotifyDate");

			if (map.get("stats") != null) {
				entity.stats = new ArrayList<Stat>();
				final List<LinkedHashMap<String, Object>> statMaps = (List<LinkedHashMap<String, Object>>) map.get("stats");
				for (Map<String, Object> statMap : statMaps) {
					entity.stats.add(Stat.setPropertiesFromMap(new Stat(), statMap));
				}
			}
		}

		return entity;
	}

	@Override
	public User clone() {
		final User user = (User) super.clone();
		if (stats != null) {
			user.stats = (List<Stat>) ((ArrayList) stats).clone();
		}
		return user;
	}

}