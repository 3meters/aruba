package com.proxibase.aircandi.model;

import com.google.gson.annotations.Expose;
import com.proxibase.sdk.android.core.ProxibaseService;

/**
 * @author Jayma
 */
public class UserWithFriend
{
	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public String	userWithFriendId;
	@Expose
	public String	userId;
	@Expose
	public String	friendId;
	@Expose
	public String	friendName;
	@Expose
	public Boolean	deleted;

	public UserWithFriend() {}
	
	public UserWithFriend(String userId, String friendId, String friendName)
	{
		this.userId = userId;
		this.friendId = friendId;
		this.friendName = friendName;
	}

	public String getUriOdata()
	{
		String root = ProxibaseService.URL_PROXIBASE_SERVICE_ODATA;
		String entity = "UsersWithFriends";
		String uri = root + entity + "(guid'" + this.userWithFriendId + "')";
		return uri;
	}
}