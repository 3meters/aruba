package com.proxibase.aircandi;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.proxibase.aircandi.models.WebEntity;
import com.proxibase.aircandi.utils.Exceptions;
import com.proxibase.aircandi.utils.Logger;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class WebBrowse extends AircandiActivity {

	WebView		mWebView;
	ProgressBar	mProgressBar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		configure();
		bindEntity();
		drawEntity();
	}

	protected void bindEntity() {
		/*
		 * We handle all the elements that are different than the base entity.
		 */
		String jsonResponse = null;
		try {
			jsonResponse = (String) ProxibaseService.getInstance().select(mEntityProxy.getEntryUri(), ResponseFormat.Json);
			mEntity = (WebEntity) ProxibaseService.convertJsonToObject(jsonResponse, WebEntity.class, GsonType.ProxibaseService);
		}
		catch (ProxibaseException exception) {
			Exceptions.Handle(exception);
		}
	}

	protected void drawEntity() {

		mWebView.setWebViewClient(new WebViewClient() {

			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				return false;
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

		Logger.i(this, "Loading uri: " + uri.toString());
		mWebView.loadUrl(uri.toString());
	}

	private void configure() {
		mWebView = (WebView) findViewById(R.id.webview);
		mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

		mContextButton = (Button) findViewById(R.id.btn_context);
		if (mContextButton != null) {
			mContextButton.setVisibility(View.INVISIBLE);
			showBackButton(true, getString(R.string.form_button_back));
		}
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

	@Override
	protected int getLayoutID() {
		return R.layout.web_browse;
	}
}