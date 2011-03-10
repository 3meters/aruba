package com.threemeters.aircandi.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.threemeters.aircandi.model.PostFb;
import com.threemeters.aircandi.model.PostsFb;
import com.threemeters.aircandi.ripple.utilities.DateUtils;
import com.threemeters.sdk.android.core.BaseQueryListener;
import com.threemeters.sdk.android.core.RippleRunner;
import com.threemeters.sdk.android.core.RippleService;
import com.threemeters.sdk.android.core.RippleService.QueryFormat;

public class FacebookFanPageFeed extends AircandiActivity
{
	private PostsFb						mPostsFb		= null;
	private String						mFriendId		= "";
	private ProgressDialog				mProgressDialog	= null;
	public static FacebookFanPageFeed	mSelf			= null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setContentView(R.layout.facebook_fan_page_feed);
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
		mProgressDialog = ProgressDialog.show(FacebookFanPageFeed.this, "", "Loading...", true);
		startProgress();
		loadPosts();
	}

	public void loadPosts()
	{
		RippleRunner ripple = new RippleRunner();
		Bundle parameters = new Bundle();

		String method = "GetFacebookPosts";
		parameters.putString("entityId", getCurrentEntity() != null ? getCurrentEntity().entityId : "");
		parameters.putString("userId", getCurrentUser() != null ? getCurrentUser().id : "");
		parameters.putString("friendId", mFriendId);

		ripple.post(method, parameters, QueryFormat.Json, new PostsQueryListener());
	}

	// For this activity, refresh means rescan and reload point data from the service
	@Override
	public void onRefreshClick(View view)
	{
		mProgressDialog = ProgressDialog.show(FacebookFanPageFeed.this, "", "Loading...", true);
		startProgress();
		loadPosts();
	}

	public String renderFacebookFeed(PostsFb postsFb) throws ParseException
	{
		String html = "";
		html += "<html>";
		html += "<head>";
		html += "<meta http-equiv='content-type' content='text/html; charset=utf-8'>";
		html += "<link href='" + Aircandi.URL_RIPPLEZANIA + "styles/rippleclient.css' rel='stylesheet' type='text/css' />";
		html += "</head>";
		html += "<body>";

		// html += "<div class='header'>";
		// html += "<table cellpadding='0' cellspacing='2' border='0'>";
		// html += "	<tr>";
		// html += "		<td class='pictureCell'><img class='picture' src='" + pageFb.picture + "' /></td>";
		// html += "		<td class='nameCell'><span class='name'>" + pageFb.name + "</span></td>";
		// html += "	</tr>";
		// html += "</table>";
		// html += "</div>";

		html += "<div class='basic'>";
		html += "<table cellpadding='0' cellspacing='2' border='0'>";

		for (PostFb post : postsFb.posts)
		{
			if (post.type.equals("status"))
			{
				// 2010-11-19T16:07:17+0000
				SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
				Date postDate = sdf.parse(post.updatedTime);
				String interval = DateUtils.intervalSince(postDate, DateUtils.nowDate());
				html += "	<tr>";
				if (post.from != null)
				{
					html += "		<td class='postUserImageCell'><img class='postUserImage' src='https://graph.facebook.com/" + post.from.id
							+ "/picture?type=square' /></td>";
				}
				else
				{
					html += "		<td class='postUserImageCell'>&nbsp;</td>";
				}
				html += "		<td class='postBodyCell'>";
				html += "			<table cellpadding='0' cellspacing='2' border='0'>";
				html += "				<tr>";
				if (post.from != null)
				{
					html += "					<td class='postMessageCell'><span class='postUserName'>" + post.from.name
							+ "</span>&nbsp;<span class='postMessage'>"
							+ clean(post.message)
							+ "</span></td>";
				}
				else
				{
					html += "					<td class='postMessageCell'><span class='postUserName'>" + "Unknown"
					+ "</span>&nbsp;<span class='postMessage'>"
					+ clean(post.message)
					+ "</span></td>";
				}
				html += "				</tr>";
				html += "				<tr>";
				html += "					<td class='postDateCell'><span class='postDate'>" + interval + "</span></td>";
				html += "				</tr>";
				html += "			</table>";
				html += "		</td>";
				html += "	</tr>";
			}

			if (post.type.equals("link"))
			{
				// 2010-11-19T16:07:17+0000
				SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
				Date postDate = sdf.parse(post.updatedTime);
				String interval = DateUtils.intervalSince(postDate, DateUtils.nowDate());
				html += "	<tr>";
				html += "		<td class='postUserImageCell'><img class='postUserImage' src='https://graph.facebook.com/" + post.from.id
						+ "/picture?type=square' /></td>";
				html += "		<td class='postBodyCell'>";
				html += "			<table cellpadding='0' cellspacing='2' border='0'>";
				html += "				<tr>";
				html += "					<td class='postMessageCell'><span class='postUserName'>" + post.from.name;
				if (post.message != null)
					html += "</span>&nbsp;<span class='postMessage'>" + clean(post.message) + "</span></td>";
				else
					html += "</span></td>";
				html += "				</tr>";
				html += "				<tr>";
				html += "					<td class='postDateCell'>";

				html += "			<table cellpadding='0' cellspacing='2' border='0'>";
				html += "				<tr>";
				html += "					<td class='linkImageCell'><a href='" + post.link + "'><img class='linkImage' src='" + post.picture + "' /></a></td>";

				if (post.name != null)
					html += "					<td class='linkNameCell'><a href='" + post.link + "'><span class='linkName' >" + post.name + "</span></a></td>";
				else
					html += "					<td class='linkNameCell'></td>";

				html += "				</tr>";

				if (post.caption != null)
				{
					html += "				<tr>";
					html += "					<td class='linkCaptionCell' colspan='2'><span class='linkCaption'>" + clean(post.caption) + "</span>";
					html += "					</td>";
					html += "				</tr>";
				}
				if (post.description != null)
				{
					html += "				<tr>";
					html += "					<td class='linkDescriptionCell' colspan='2'><span class='linkDescription'>" + clean(post.description)
							+ "</span></td>";
					html += "				</tr>";
				}
				html += "			</table>";

				html += "					</td>";
				html += "				</tr>";
				html += "				<tr>";
				html += "					<td class='postDateCell' colspan='2'><img class='postDateIcon' src='" + post.icon
						+ "' />&nbsp;<span class='postDate'>"
						+ interval
						+ "</span></td>";
				html += "				</tr>";
				html += "			</table>";
				html += "		</td>";
				html += "	</tr>";
			}

		}

		html += "</table>";
		html += "<br /><br /><br />";
		html += "</div>";

		html += "</body>";
		html += "</html>";

		return html;

	}

	public String clean(String html)
	{
		String cleanHtml = html;
		cleanHtml = cleanHtml.replaceAll("%", "&#37;");
		cleanHtml = cleanHtml.replaceAll("\"", "&quot;");
		cleanHtml = cleanHtml.replaceAll("&", "&amp;");
		cleanHtml = cleanHtml.replaceAll("<", "&lt;");
		cleanHtml = cleanHtml.replaceAll(">", "&gt;");
		cleanHtml = cleanHtml.replaceAll("'", "&#39;");
		return cleanHtml;
	}

	public class PostsQueryListener extends BaseQueryListener
	{
		public void onComplete(final String response)
		{
			mPostsFb = (PostsFb) RippleService.convertJsonToObjects(response, PostsFb.class).get(0);

			// Post the processed result back to the UI thread
			FacebookFanPageFeed.this.runOnUiThread(new Runnable() {
				public void run()
				{
					if (mPostsFb != null)
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
		try
		{
			WebView webView = (WebView) findViewById(R.id.WebView);
			webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
			webView.loadData(renderFacebookFeed(mPostsFb), "text/html", "UTF-8");
		}
		catch (ParseException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}