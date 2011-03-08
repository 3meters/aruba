package com.georain.ripple.model;

import android.graphics.Bitmap;
import com.georain.ripple.controller.Ripple;
import com.google.gson.annotations.Expose;

public class UserFb
{
	@Expose
	public String	id;
	@Expose
	public String	name;
	@Expose
	public String	token			= "";
	@Expose
	public String	tokenDate		= "";

	// Fields that get read from Ripple but don't get sent back
	public String	label			= "";
	public String	hookupDate;
	public String	first_name		= "";
	public String	last_name		= "";
	public String	link			= "";
	public String	gender			= "";
	public int		timezone		= 0;
	public String	locale			= "";
	public Boolean	verified		= false;
	public String	picture_link	= "";
	public Bitmap	picture_bitmap	= null;
	public String	friendsDate		= "";

	public UserFb() {}

	public String getUriOdata()
	{
		String root = Ripple.URL_RIPPLESERVICE_ODATA;
		String entity = "Users";
		String uri = root + entity + "('" + this.id + "')";
		return uri;
	}
}
