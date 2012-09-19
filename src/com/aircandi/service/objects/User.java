package com.aircandi.service.objects;

import java.util.HashMap;

import com.aircandi.service.Expose;


/**
 * @author Jayma
 */
public class User extends ServiceEntry {

	/* syntax: @Expose (serialize = false, deserialize = false) */

	private static final long	serialVersionUID	= 127428776257201065L;

	@Expose
	public String				name;
	@Expose
	public String				email;										// Required
	@Expose
	public String				role;
	@Expose
	public String				imageUri;
	@Expose
	public String				linkUri;
	@Expose
	public String				location;
	@Expose
	public String				bio;
	@Expose
	public String				webUri;
	@Expose
	public Boolean				isDeveloper;

	@Expose(serialize = false, deserialize = true)
	public String				facebookId;
	@Expose(serialize = false, deserialize = true)
	public String				twitterId;
	@Expose(serialize = false, deserialize = true)
	public String				googleId;
	@Expose(serialize = false, deserialize = true)
	public String				password;
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

	/* For client use only */
	public boolean				keepSignedIn		= false;
	public Session				session;

	public User() {}

	@Override
	public User clone() {
		try {
			final User user = (User) super.clone();
			return user;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public Boolean isAnonymous() {
		return this.id.equals("0000.000000.00000.000.000000");
	}

	public static User setFromPropertiesFromMap(User user, HashMap map) {
		/*
		 * These base properties are done here instead of calling ServiceEntry
		 * because of a recursion problem.
		 */
		user.id = (String) map.get("_id");
		user.creatorId = (String) map.get("_creator");
		user.ownerId = (String) map.get("_owner");
		user.modifierId = (String) map.get("modifierId");
		user.createdDate = (Number) map.get("createdDate");
		user.modifiedDate = (Number) map.get("modifiedDate");

		user.name = (String) map.get("name");
		user.location = (String) map.get("location");
		user.imageUri = (String) map.get("imageUri");
		user.email = (String) map.get("email");
		user.role = (String) map.get("role");
		user.linkUri = (String) map.get("linkUri");
		user.bio = (String) map.get("bio");
		user.webUri = (String) map.get("webUri");
		user.isDeveloper = (Boolean) map.get("isDeveloper");
		user.password = (String) map.get("password");
		user.lastSignedInDate = (Number) map.get("lastSignedInDate");
		user.validationDate = (Number) map.get("validationDate");
		user.validationNotifyDate = (Number) map.get("validationNotifyDate");
		
		return user;
	}

	@Override
	public String getCollection() {
		return "users";
	}
}