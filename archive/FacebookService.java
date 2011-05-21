package com.proxibase.aircandi.controller;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.ClientProtocolException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.facebook.android.FacebookRunner;
import com.proxibase.sdk.android.core.ProxibaseService;
import com.proxibase.sdk.android.core.ProxibaseService.QueryFormat;

public class FacebookService
{
	public static final String		GRAPH_BASE_URL	= "https://graph.facebook.com/";

	public static FacebookRunner	facebookRunner;

	public static Bitmap getFacebookPicture(String userId, String size)
	{
		ProxibaseService ripple = new ProxibaseService();
		String url = FacebookService.GRAPH_BASE_URL + userId + "/picture?type=" + size;
		try
		{
			InputStream stream = ripple.getStream(url, QueryFormat.Xml);
			Bitmap bm = BitmapFactory.decodeStream(stream);
			return bm;
		}
		catch (ClientProtocolException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
}