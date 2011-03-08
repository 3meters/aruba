package com.georain.ripple.controller;

import android.os.Bundle;
import android.webkit.WebView;
import com.georain.ripple.model.Post;
import com.georain.ripple.model.RippleService;
import com.georain.ripple.model.RippleService.GsonType;

public class InfoForm extends RippleActivity {
	
	private Post post;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.info_form);
		super.onCreate(savedInstanceState);

		// Get the post we are rooted on
		if (getIntent() != null & getIntent().getExtras() != null)
		{
			post = RippleService.getGson(GsonType.Internal).fromJson(getIntent().getExtras().getString("post"), Post.class);
		}

		WebView webView = (WebView) findViewById(R.id.WebView);
		webView.setBackgroundColor(0);

		String data = "<font color='white'><h2>" + post.title + "</h2>";
		data += post.postContent + "</font>";
		webView.loadData(data, "text/html", "UTF-8");
	}
}