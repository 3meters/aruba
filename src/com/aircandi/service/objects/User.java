package com.aircandi.service.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aircandi.ServiceConstants;
import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class User extends Entity {

	private static final long	serialVersionUID	= 127428776257201065L;
	public static final String	collectionId		= "users";

	// --------------------------------------------------------------------------------------------
	// service fields
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

	@Expose
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
	// client fields
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
		String photoUri = "resource:img_placeholder_logo_bw";
		if (photo != null) {
			photoUri = photo.getSizedUri(250, 250); // sizing ignored if source doesn't support it
			if (photoUri == null) {
				photoUri = photo.getUri();
			}
		}
		else {
			if (creator != null) {
				if (creator.getPhotoUri() != null && !creator.getPhotoUri().equals("")) {
					photoUri = creator.getPhotoUri();
				}
			}
			if (!photoUri.startsWith("http:")
					&& !photoUri.startsWith("https:")
					&& !photoUri.startsWith("resource:")) {
				photoUri = ServiceConstants.URL_PROXIBASE_MEDIA_IMAGES + photoUri;
			}
		}

		return photoUri;
	}

	@Override
	public String getCollection() {
		return collectionId;
	}

	// --------------------------------------------------------------------------------------------
	// Copy and serialization
	// --------------------------------------------------------------------------------------------

	public static User setPropertiesFromMap(User entity, Map map, Boolean nameMapping) {

		synchronized (entity) {
			entity = (User) Entity.setPropertiesFromMap(entity, map, nameMapping);

			entity.area = (String) map.get("area");
			entity.email = (String) map.get("email");
			entity.role = (String) map.get("role");
			entity.bio = (String) map.get("bio");
			entity.webUri = (String) map.get("webUri");
			entity.developer = (Boolean) map.get("developer");
			entity.doNotTrack = (Boolean) map.get("doNotTrack");
			entity.password = (String) map.get("password");
			entity.authSource = (String) map.get("authSource");
			entity.lastSignedInDate = (Number) map.get("lastSignedInDate");
			entity.validationDate = (Number) map.get("validationDate");
			entity.validationNotifyDate = (Number) map.get("validationNotifyDate");
			
			if (map.get("session") != null) {
				entity.session = Session.setPropertiesFromMap(new Session(), (HashMap<String, Object>) map.get("session"), nameMapping);
			}

			if (map.get("stats") != null) {
				entity.stats = new ArrayList<Stat>();
				final List<LinkedHashMap<String, Object>> statMaps = (List<LinkedHashMap<String, Object>>) map.get("stats");
				for (Map<String, Object> statMap : statMaps) {
					entity.stats.add(Stat.setPropertiesFromMap(new Stat(), statMap, nameMapping));
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