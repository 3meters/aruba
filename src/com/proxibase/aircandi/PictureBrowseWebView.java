package com.proxibase.aircandi;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.aircandi.service.objects.Entity;
import com.proxibase.aircandi.components.Exceptions;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.widgets.AuthorBlock;

public class PictureBrowseWebView extends FormActivity {

	private WebView		mWebView;
	private ProgressBar	mProgress;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
		draw();
	}

	private void initialize() {
		mProgress = (ProgressBar) findViewById(R.id.progressBar);
		mWebView = (WebView) findViewById(R.id.webview);
		mWebView.getSettings().setLoadWithOverviewMode(true);
		mWebView.getSettings().setUseWideViewPort(true);
		mWebView.getSettings().setSupportZoom(true);
		mWebView.getSettings().setBuiltInZoomControls(true);
		mWebView.setVerticalScrollbarOverlay(true);

		mWebView.setWebChromeClient(new WebChromeClient() {

			@Override
			public void onProgressChanged(WebView view, int progress) {
				mProgress.setProgress(progress);
				if (progress == 100) {
					mProgress.setVisibility(View.GONE);
				}
			}
		});
	}

	protected void draw() {
		
		final Entity entity = ProxiExplorer.getInstance().getEntityModel().getEntityById(mCommon.mEntityId, mCommon.mParentId, mCommon.mEntityTree);

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


		mWebView.loadUrl(entity.imageUri);
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
		return R.layout.picture_browse_webview;
	}
}