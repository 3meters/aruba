package com.proxibase.aircandi.activities;

import android.os.Bundle;
import android.webkit.WebView;

import com.proxibase.aircandi.controllers.R;
import com.proxibase.aircandi.models.Post;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;

public class InfoForm extends AircandiActivity {
	
	private Post post;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.info_form);
		super.onCreate(savedInstanceState);

		// Get the post we are rooted on
		if (getIntent() != null & getIntent().getExtras() != null)
		{
			post = ProxibaseService.getGson(GsonType.Internal).fromJson(getIntent().getExtras().getString("post"), Post.class);
		}

		WebView webView = (WebView) findViewById(R.id.WebView);
		webView.setBackgroundColor(0);

		String data = "<font color='white'><h2>" + post.title + "</h2>";
		data += post.postContent + "</font>";
		webView.loadData(data, "text/html", "UTF-8");
	}
}