package com.proxibase.aircandi.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.WebEntity;
import com.proxibase.aircandi.utils.Logger;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class WebBrowse extends EntityBase {

	WebView		mWebView;
	ProgressBar	mProgressBar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mWebView = (WebView) findViewById(R.id.webview);
		mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

		bindEntity();
		drawEntity();
	}

	@Override
	protected void bindEntity() {
		/*
		 * We handle all the elements that are different than the base entity.
		 */
		String jsonResponse = null;
		try {
			jsonResponse = (String) ProxibaseService.getInstance().select(mEntityProxy.getEntryUri(), ResponseFormat.Json);
		}
		catch (ProxibaseException exception) {
			exception.printStackTrace();
		}

		mEntity = (WebEntity) ProxibaseService.convertJsonToObject(jsonResponse, WebEntity.class, GsonType.ProxibaseService);
		super.bindEntity();
	}

	protected void drawEntity() {
		super.drawEntity();

		mWebView.setWebViewClient(new WebViewClient() {

			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				return false;
			}
		});

		mWebView.setDownloadListener(new DownloadListener() {

			public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(url));
				startActivity(intent);
			}
		});

		mWebView.setWebChromeClient(new WebChromeClient() {

			public void onProgressChanged(WebView view, int progress) {
				mProgressBar.setProgress(progress);
				if (progress == 100) {
					mProgressBar.setVisibility(View.GONE);
				}
			}
		});

		mWebView.getSettings().setSupportZoom(true);
		mWebView.getSettings().setBuiltInZoomControls(true);
		mWebView.getSettings().setLoadWithOverviewMode(true);
		mWebView.getSettings().setUseWideViewPort(true);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setPluginsEnabled(true);

		Uri uri = Uri.parse(((WebEntity) mEntity).contentUri);
		if (!URLUtil.isValidUrl(uri.toString())) {
			Toast.makeText(this, "Invalid URL specified", Toast.LENGTH_SHORT).show();
			startTitlebarProgress();
			setResult(Activity.RESULT_CANCELED);
			finish();
			overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
		}

		Logger.i(CandiConstants.APP_NAME, getClass().getSimpleName(), "Loading uri: " + uri.toString());
		mWebView.loadUrl(uri.toString());
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------
	@Override
	public void onBackPressed() {
		if (mWebView.canGoBack()) {
			mWebView.goBack();
		}
		else {
			super.onBackPressed();
		}
	}

	protected void onDestroy() {
		super.onDestroy();

		/* This activity gets destroyed everytime we leave using back or finish(). */
		try {
			mEntity = null;
		}
		catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	@Override
	protected int getLayoutID() {
		return R.layout.web_browse;
	}
}