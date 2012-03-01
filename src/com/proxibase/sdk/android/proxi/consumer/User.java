package com.proxibase.sdk.android.proxi.consumer;

import android.graphics.Bitmap;

import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class User extends ServiceEntry {

	/* syntax: @Expose (serialize = false, deserialize = false) */

	@Expose
	public String	name;
	@Expose
	public String	email;						// Required
	@Expose
	public String	role;
	@Expose
	public String	password;
	@Expose
	public String	imageUri;
	@Expose
	public String	linkUri;
	@Expose
	public String	location;
	@Expose
	public String	facebookId;
	@Expose
	public Boolean	isDeveloper;

	/* For client use only */
	public boolean	anonymous		= false;
	public boolean	keepSignedIn	= false;
	public Bitmap	imageBitmap;

	public User() {}

	@Override
	public String getCollection() {
		return "users";
	}
}