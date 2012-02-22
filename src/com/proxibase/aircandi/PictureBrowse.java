package com.proxibase.aircandi;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import com.proxibase.aircandi.components.Exceptions;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.sdk.android.proxi.consumer.Entity;

public class PictureBrowse extends FormActivity {

	private WebView	mWebView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
		bind();
		draw();
		Tracker.trackPageView("/PictureBrowse");

	}

	protected void bind() {
		final Entity entity = mCommon.mEntity;

		/* Author block */
		if (entity.author != null) {
			((AuthorBlock) findViewById(R.id.block_author)).bindToAuthor(entity.author, entity.modifiedDate.longValue(), entity.locked);
		}
		else {
			((View) findViewById(R.id.block_author)).setVisibility(View.GONE);
		}

		String imageUri = entity.imageUri;
		String html = null;

		html = "<html>";
		html += "	<body bgcolor=\"transparent\" style=\"margin:0; background-color:transparent\">";
//		html += "		<div style=\"width:100%;height:100%;display:table\">";
//		html += "		<div style=\"display:table-cell;vertical-align:middle;width:100%;text-align:center\">";
		html += "		<img style=\"width:100%\" src=\"" + imageUri + "\">";
//		html += "		</div>";
//		html += "		</div>";
		html += "	</body>";
		html += "</html>";
		
		mWebView.loadDataWithBaseURL("fake://not/needed", html, "text/html", "utf-8", "fake://not/needed");
		mWebView.setBackgroundColor(0x00000000);
	}

	protected void draw() {

		if (findViewById(R.id.text_title) != null) {
			((TextView) findViewById(R.id.text_title)).setText(mCommon.mEntity.title);
		}
	}

	private void initialize() {
		mWebView = (WebView) findViewById(R.id.webview);
		mWebView.setBackgroundColor(0x00000000);
		mWebView.getSettings().setLoadWithOverviewMode(true);
		mWebView.getSettings().setUseWideViewPort(true);
		mWebView.getSettings().setSupportZoom(true);
		mWebView.getSettings().setBuiltInZoomControls(true);
		mWebView.setVerticalScrollbarOverlay(true);
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------
	
	protected void onDestroy() {

		/* This activity gets destroyed everytime we leave using back or finish(). */
		try {
			if (mCommon.mEntity.imageBitmap != null) {
				mCommon.mEntity.imageBitmap.recycle();
			}
		}
		catch (Exception exception) {
			Exceptions.Handle(exception);
		}
		finally {
			super.onDestroy();
		}
	}

	@Override
	protected int getLayoutID() {
		return R.layout.picture_browse;
	}
}