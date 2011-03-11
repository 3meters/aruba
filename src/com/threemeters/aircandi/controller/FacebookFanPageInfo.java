package com.threemeters.aircandi.controller;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.threemeters.aircandi.model.PageFb;
import com.threemeters.sdk.android.core.BaseQueryListener;
import com.threemeters.sdk.android.core.RippleRunner;
import com.threemeters.sdk.android.core.RippleService;
import com.threemeters.sdk.android.core.RippleService.QueryFormat;

public class FacebookFanPageInfo extends AircandiActivity
{
	private PageFb						mPageFb			= null;
	private String						mFriendId		= "";
	private ProgressDialog				mProgressDialog	= null;
	public static FacebookFanPageInfo	mSelf			= null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setContentView(R.layout.facebook_fan_page_info);
		super.onCreate(savedInstanceState);
		mSelf = this;

		// Pull out the friendId if it was sent
		if (getIntent().getExtras() != null)
		{
			String friendId = getIntent().getExtras().getString("FriendId");
			if (friendId != null)
				mFriendId = getIntent().getExtras().getString("FriendId");
		}

		// Load the content for display
		mProgressDialog = ProgressDialog.show(FacebookFanPageInfo.this, "", "Loading...", true);
		startProgress();
		loadInfo();
	}

	public void loadInfo()
	{
		RippleRunner ripple = new RippleRunner();
		Bundle parameters = new Bundle();

		String method = "GetFacebookPage";
		parameters.putString("entityId", getCurrentEntity() != null ? getCurrentEntity().entityId : "");
		parameters.putString("userId", getCurrentUser() != null ? getCurrentUser().id : "");
		parameters.putString("friendId", mFriendId);

		ripple.post(method, parameters, QueryFormat.Json, new InfoQueryListener());
	}

	// For this activity, refresh means rescan and reload point data from the service
	@Override
	public void onRefreshClick(View view)
	{
		mProgressDialog = ProgressDialog.show(FacebookFanPageInfo.this, "", "Loading...", true);
		startProgress();
		loadInfo();
	}

	public String renderFacebookInfo(PageFb pageFb)
	{
		String html = "";
		html += "<html>";
		html += "<head>";
		html += "<link href='" + Aircandi.URL_AIRCANDI_SERVICE + "styles/rippleclient.css' rel='stylesheet' type='text/css' />";
		
		html += "</head>";
		html += "<body>";

		html += "<div class='header'>";
		html += "<table cellpadding='0' cellspacing='2' border='0'>";
		html += "	<tr>";

		if (pageFb.picture != null)
			html += "		<td class='pictureCell'><img class='picture' src='" + pageFb.picture + "' /></td>";
		else
			html += "		<td class='pictureCell'><img class='picture' src='https://graph.facebook.com/" + pageFb.id + "/picture?type=square' /></td>";

		html += "		<td class='nameCell'><span class='name'>" + pageFb.name + "</span></td>";
		html += "	</tr>";
		html += "</table>";
		html += "</div>";

		html += "<div class='basic'>";
		html += "<table cellpadding='0' cellspacing='2' border='0'>";
		if (pageFb.founded != null)
		{
			html += "	<tr>";
			html += "		<td class='labelCell'><span class='label'>Founded:</span></td>";
			html += "		<td class='valueCell'><span class='value'>" + pageFb.founded + "</span></td>";
			html += "	</tr>";
		}
		if (pageFb.location != null && pageFb.location.state != null)
		{
			html += "	<tr>";
			html += "		<td class='labelCell'><span class='label'>Location:</span></td>";
			html += "		<td><span class='value'>" + pageFb.location.street + "</span></td>";
			html += "	</tr>";
		}
		if (pageFb.location != null && pageFb.location.city != null)
		{
			html += "	<tr>";
			html += "		<td class='labelCell'></td>";
			html += "		<td class='valueCell'><span class='value'>" + pageFb.location.city
					+ ", "
					+ pageFb.location.state
					+ ", "
					+ pageFb.location.zip
					+ "</span></td>";
			html += "	</tr>";
		}
		if (pageFb.location != null && pageFb.location.city != null)
		{
			html += "	<tr>";
			html += "		<td class='labelCell'><span class='label'>Phone:</span></td>";
			html += "		<td class='valueCell'><span class='value'>" + pageFb.phone + "</span></td>";
			html += "	</tr>";
		}
		html += "</table>";
		html += "</div>";

		html += "<div class='basic'>";
		html += "<table cellpadding='0' cellspacing='2' border='0'>";

		if (pageFb.website != null)
		{
			String replaced = pageFb.website.replaceAll("\n", "<br />");
			html += "	<tr>";
			html += "		<td class='labelCell'><span class='label'>Website:</span></td>";
			html += "		<td class='valueCell'><span class='value'>" + replaced + "</span></td>";
			html += "	</tr>";
		}
		if (pageFb.companyOverview != null)
		{
			String replaced = pageFb.companyOverview.replaceAll("\n", "<br />");
			html += "	<tr>";
			html += "		<td class='labelCell'><span class='label'>Company Overview:</span></td>";
			html += "		<td class='valueCell'><span class='value'>" + replaced + "</span></td>";
			html += "	</tr>";
		}
		if (pageFb.mission != null)
		{
			html += "	<tr>";
			html += "		<td class='labelCell'><span class='label'>Mission:</span></td>";
			html += "		<td class='valueCell'><span class='value'>" + pageFb.mission + "</span></td>";
			html += "	</tr>";
		}
		if (pageFb.products != null)
		{
			html += "	<tr>";
			html += "		<td class='labelCell'><span class='label'>Products:</span></td>";
			html += "		<td class='valueCell'><span class='value'>" + pageFb.products + "</span></td>";
			html += "	</tr>";
		}
		if (pageFb.link != null)
		{
			// html += "	<tr>";
			// html += "		<td class='labelCell'><span class='label'>Facebook:</span></td>";
			// html += "		<td class='valueCell'><a href='" + pageFb.link +
			// "'><span class='value'>facebook.com</span></a></td>";
			// html += "	</tr>";
		}
		html += "</table>";
		html += "<br /><br /><br />";
		html += "</div>";

		html += "</body>";
		html += "</html>";

		return html;
	}

	public class InfoQueryListener extends BaseQueryListener
	{
		public void onComplete(final String response)
		{
			mPageFb = (PageFb) RippleService.convertJsonToObjects(response, PageFb.class).get(0);

			// Post the processed result back to the UI thread
			FacebookFanPageInfo.this.runOnUiThread(new Runnable() {
				public void run()
				{
					if (mPageFb != null)
					{
						populateWebView();
						mProgressDialog.dismiss();
						stopProgress();
					}
				}
			});
		}
	}

	public void populateWebView()
	{
		WebView webView = (WebView) findViewById(R.id.WebView);
		webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		webView.loadData(renderFacebookInfo(mPageFb), "text/html", "UTF-8");
	}
}