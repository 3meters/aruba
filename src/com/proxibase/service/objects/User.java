package com.proxibase.service.objects;

import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class User extends ServiceEntry {

	/* syntax: @Expose (serialize = false, deserialize = false) */

	private static final long	serialVersionUID	= 127428776257201065L;
	
	@Expose
	public String	name;
	@Expose
	public String	email;						// Required
	@Expose
	public String	role;
	@Expose
	public String	imageUri;
	@Expose
	public String	linkUri;
	@Expose
	public String	location;
	@Expose
	public String	bio;
	@Expose
	public String	webUri;
	@Expose
	public Boolean	isDeveloper;
	
	@Expose
	public String	facebookId;
	@Expose
	public String	twitterId;
	@Expose
	public String	googleId;
	@Expose
	public String	password;
	@Expose
	public Number	lastSignedInDate;
	@Expose
	public String	authSource;
	@Expose
	public String	oauthId;
	@Expose
	public String	oauthToken;
	@Expose
	public String	oauthSecret;
	@Expose
	public String	oauthData;
	@Expose
	public Number	emailValidated;

	/* For client use only */
	public boolean	anonymous		= false;
	public boolean	keepSignedIn	= false;
	public Session	session;

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

	
	@Override
	public String getCollection() {
		return "users";
	}
}