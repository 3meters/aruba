package com.proxibase.aircandi;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.proxibase.aircandi.components.Exceptions;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.service.objects.Entity;

public class PictureBrowse extends FormActivity {

	private WebView		mWebView;
	private ProgressBar	mProgress;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
		draw();
	}

	private void initialize() {
		mCommon.mActionBar.setTitle(R.string.form_title_picture_browse);

		mProgress = (ProgressBar) findViewById(R.id.progressBar);
		mWebView = (WebView) findViewById(R.id.webview);
		mWebView.setBackgroundColor(0x00000000);
		mWebView.getSettings().setLoadWithOverviewMode(true);
		mWebView.getSettings().setUseWideViewPort(true);
		mWebView.getSettings().setSupportZoom(true);
		mWebView.getSettings().setBuiltInZoomControls(true);
		mWebView.setVerticalScrollbarOverlay(true);

		mWebView.setWebChromeClient(new WebChromeClient() {

			@Override
			public void onProgressChanged(WebView view, int progress) {
				mProgress.setProgress(progress);
			}
		});
	}

	protected void draw() {
		
		final Entity entity = ProxiExplorer.getInstance().getEntityModel().getEntityById(mCommon.mEntityId, mCommon.mCollectionType);

		/* Title */
		if (findViewById(R.id.text_title) != null) {
			((TextView) findViewById(R.id.text_title)).setText(entity.title);
		}

		/* Author block */
		if (entity.creator != null) {
			((AuthorBlock) findViewById(R.id.block_author)).bindToAuthor(entity.creator,
					entity.modifiedDate.longValue(), entity.locked);
		}
		else {
			((View) findViewById(R.id.block_author)).setVisibility(View.GONE);
		}

		/* Image html */
		String html = null;
		html = "<html>";
		html += "	<body bgcolor=\"transparent\" style=\"margin:0; background-color:transparent\">";
		html += "		<img style=\"width:100%\" src=\"" + entity.imageUri + "\">";
		html += "	</body>";
		html += "</html>";

		mWebView.loadDataWithBaseURL("fake://not/needed", html, "text/html", "utf-8", "fake://not/needed");
		mWebView.setBackgroundColor(0x00000000); // Transparent
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	protected void onDestroy() {
		/*
		 * This activity gets destroyed everytime we leave using back or
		 * finish().
		 */
		try {
			mCommon.doDestroy();
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