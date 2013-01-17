package com.aircandi.service.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
public class User extends ServiceEntryBase {

	/* syntax: @Expose (serialize = false, deserialize = false) */

	private static final long	serialVersionUID	= 127428776257201065L;

	@Expose
	public String				email;										// Required
	@Expose
	public String				role;
	@Expose
	public String				location;
	@Expose
	public String				bio;
	@Expose
	public String				webUri;
	@Expose
	public Boolean				isDeveloper;
	@Expose
	public Photo				photo;
	@Expose
	public String				password;

	@Expose(serialize = false, deserialize = true)
	public String				facebookId;
	@Expose(serialize = false, deserialize = true)
	public String				twitterId;
	@Expose(serialize = false, deserialize = true)
	public String				googleId;
	@Expose(serialize = false, deserialize = true)
	public Number				lastSignedInDate;
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
	public Number				validationDate;
	@Expose(serialize = false, deserialize = true)
	public Number				validationNotifyDate;

	@Expose(serialize = false, deserialize = true)
	public List<Stat>			stats;

	/* For client use only */
	public boolean				keepSignedIn		= false;
	public Session				session;
	public String				firstName;
	public String				lastName;

	public User() {}

	@Override
	public User clone() {
		try {
			final User user = (User) super.clone();
			if (this.stats != null) {
				user.stats = (List<Stat>) ((ArrayList) this.stats).clone();
			}

			return user;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public static User setPropertiesFromMap(User user, HashMap map) {
		/*
		 * These base properties are done here instead of calling ServiceEntry
		 * because of a recursion problem.
		 */
		user.id = (String) (map.get("_id") != null ? map.get("_id") : map.get("id"));
		user.ownerId = (String) (map.get("_owner") != null ? map.get("_owner") : map.get("ownerId"));
		user.creatorId = (String) (map.get("_creator") != null ? map.get("_creator") : map.get("creatorId"));
		user.modifierId = (String) (map.get("_modifier") != null ? map.get("_modifier") : map.get("modifierId"));

		user.createdDate = (Number) map.get("createdDate");
		user.modifiedDate = (Number) map.get("modifiedDate");

		user.name = (String) map.get("name");
		user.firstName = (String) map.get("firstName");
		user.lastName = (String) map.get("lastName");
		user.location = (String) map.get("location");
		user.email = (String) map.get("email");
		user.role = (String) map.get("role");
		user.bio = (String) map.get("bio");
		user.webUri = (String) map.get("webUri");
		user.isDeveloper = (Boolean) map.get("isDeveloper");
		user.password = (String) map.get("password");
		user.lastSignedInDate = (Number) map.get("lastSignedInDate");
		user.validationDate = (Number) map.get("validationDate");
		user.validationNotifyDate = (Number) map.get("validationNotifyDate");
		if (map.get("photo") != null) {
			user.photo = (Photo) Photo.setPropertiesFromMap(new Photo(), (HashMap<String, Object>) map.get("photo"));
		}

		if (map.get("stats") != null) {
			user.stats = new ArrayList<Stat>();
			List<LinkedHashMap<String, Object>> statMaps = (List<LinkedHashMap<String, Object>>) map.get("stats");
			for (LinkedHashMap<String, Object> statMap : statMaps) {
				user.stats.add(Stat.setPropertiesFromMap(new Stat(), statMap));
			}
		}

		return user;
	}

	public Photo getPhoto() {
		return photo != null ? photo : new Photo();
	}

	public Photo getPhotoForSet() {
		if (photo == null) {
			photo = new Photo();
		}
		return photo;
	}

	public String getUserPhotoUri() {
		String imageUri = "resource:placeholder_logo_bw";
		if (this.photo != null) {
			imageUri = photo.getUri();
		}
		return imageUri;
	}

	@Override
	public String getCollection() {
		return "users";
	}
}