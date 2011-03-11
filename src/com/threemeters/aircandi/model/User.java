package com.threemeters.aircandi.model;

import com.google.gson.annotations.Expose;
import com.threemeters.sdk.android.core.RippleService;

public class User
{
	@Expose
	public String	userId;
	@Expose
	public String	userName;
	@Expose
	public String	loweredEmail	= "";
	@Expose
	public String	lastLoginDate	= "";

	public UserType	userType		= UserType.Anonymous;
	public String	userPwd			= "";
	public String	userLogin		= "";

	public String getUriOdata()
	{
		String root = RippleService.URL_RIPPLESERVICE_ODATA;
		String entity = "Users";
		String uri = root + entity + "(guid'" + this.userId + "')";
		return uri;
	}

	public User() {}

	public User(String userId, String userName, UserType userType) {
		this.userId = userId;
		this.userName = userName;
		this.userType = userType;
	}

	public User(String userId, String userName, UserType userType, String userLogin, String userPwd) {
		this.userId = userId;
		this.userName = userName;
		this.userType = userType;
		this.userLogin = userLogin;
		this.userPwd = userPwd;
	}

	public enum UserType
	{
		Anonymous, User, Owner
	}
}
