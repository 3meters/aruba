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

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.models.WebEntity;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
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
		GoogleAnalyticsTracker.getInstance().trackPageView("/WebBrowse");
	}

	protected void bindEntity() {
		/*
		 * We handle all the elements that are different than the base entity.
		 */
		ServiceResponse serviceResponse = NetworkManager.getInstance().request(
				new ServiceRequest(mEntityProxy.getEntryUri(), RequestType.Get, ResponseFormat.Json));

		if (serviceResponse.responseCode != ResponseCode.Success) {
			setResult(Activity.RESULT_CANCELED);
			finish();
			overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
		}
		else {
			String jsonResponse = (String) serviceResponse.data;
			mEntity = (WebEntity) ProxibaseService.convertJsonToObject(jsonResponse, WebEntity.class, GsonType.ProxibaseService);
			GoogleAnalyticsTracker.getInstance().dispatch();
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