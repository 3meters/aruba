package com.proxibase.aircandi;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.URLUtil;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.WebEntity;
import com.proxibase.aircandi.utils.ImageUtils;
import com.proxibase.aircandi.utils.NetworkManager;
import com.proxibase.aircandi.utils.NetworkManager.ResponseCode;
import com.proxibase.aircandi.utils.NetworkManager.ServiceResponse;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class WebForm extends EntityBaseForm {

	@Override
	protected void bindEntity() {

		/* We handle all the elements that are different than the base entity. */

		if (mCommand.verb.equals("new")) {
			WebEntity entity = new WebEntity();
			entity.entityType = CandiConstants.TYPE_CANDI_WEB_BOOKMARK;
			if (mParentEntityId != 0) {
				entity.parentEntityId = mParentEntityId;
			}
			else {
				entity.parentEntityId = null;
			}
			entity.enabled = true;
			mEntity = entity;
		}
		else if (mCommand.verb.equals("edit")) {

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
		super.bindEntity();
	}

	@Override
	protected void drawEntity() {
		super.drawEntity();

		if (mCommand.verb.equals("edit")) {
			((EditText) findViewById(R.id.text_uri)).setText(((WebEntity) mEntity).contentUri);
		}

		if (findViewById(R.id.chk_html_zoom) != null) {
			((CheckBox) findViewById(R.id.chk_html_zoom)).setVisibility(View.VISIBLE);
			if (((WebEntity) mEntity).imageFormat != null) {
				((CheckBox) findViewById(R.id.chk_html_zoom)).setChecked(((WebEntity) mEntity).imageFormat.equals("htmlzoom") ? true : false);
			}
		}

		if (findViewById(R.id.chk_html_javascript) != null) {
			((CheckBox) findViewById(R.id.chk_html_javascript)).setVisibility(View.VISIBLE);
			((CheckBox) findViewById(R.id.chk_html_javascript)).setChecked(((WebEntity) mEntity).javascriptEnabled);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onLinkBuilderClick(View view) {
		showBookmarkActivity();
		//showLinkBuilderDialog();
	}

	// --------------------------------------------------------------------------------------------
	// Picker routines
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void doSave(boolean updateImages) {
		if (!validate()) {
			return;
		}

		super.doSave(false);
	}

	private boolean validate() {
		// Validate URL
		String uri = ((EditText) findViewById(R.id.text_uri)).getText().toString().trim();
		if (!URLUtil.isValidUrl(uri)) {
			Toast.makeText(this, "Invalid URL specified", Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}

	@Override
	protected void gather() {
		/*
		 * Handle properties that are not part of the base entity
		 */
		final WebEntity entity = (WebEntity) mEntity;
		entity.contentUri = ((EditText) findViewById(R.id.text_uri)).getText().toString().trim();
		entity.imageUri = entity.contentUri;

		entity.javascriptEnabled = ((CheckBox) findViewById(R.id.chk_html_javascript)).isChecked();
		boolean zoomHtml = ((CheckBox) findViewById(R.id.chk_html_zoom)).isChecked();
		entity.imageFormat = zoomHtml ? "htmlzoom" : "html";

		super.gather();
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	protected void showLinkBuilderDialog() {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {

				final CharSequence[] items = {
												getResources().getString(R.string.web_dialog_link_bookmark),
												getResources().getString(R.string.web_dialog_link_search) };
				AlertDialog.Builder builder = new AlertDialog.Builder(WebForm.this);
				builder.setTitle(getResources().getString(R.string.web_dialog_link_message));
				builder.setCancelable(true);

				builder.setOnCancelListener(new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
					}
				});

				builder.setNegativeButton(getResources().getString(R.string.web_dialog_link_negative), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});

				builder.setItems(items, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int item) {
						if (item == 0) {
							showBookmarkActivity();
						}
						else if (item == 1) {
							Intent launchBrowser = new Intent(Intent.ACTION_WEB_SEARCH);
							launchBrowser.putExtra(SearchManager.QUERY, "cat humor");
							startActivityForResult(launchBrowser, 10);
							dialog.dismiss();
						}
						else {
							Toast.makeText(getApplicationContext(), "Not implemented yet.", Toast.LENGTH_SHORT).show();
						}
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
			}
		});
	}

	private void showBookmarkActivity() {
		final Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		ComponentName cm = new ComponentName("com.android.browser", "com.android.browser.CombinedBookmarkHistoryActivity");
		intent.setComponent(cm);
		try {
			startActivityForResult(intent, 1);
		}
		catch (Exception exception) {
			/* We fallback to try a different way to construct the component */
			cm = new ComponentName("com.android.browser", "CombinedBookmarkHistoryActivity");
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		switch (requestCode) {
			case 1 :
				if (resultCode == RESULT_OK && intent != null) {
					String data = intent.getAction();
					Bundle extras = intent.getExtras();
					if (extras != null && extras.getBoolean("new_window", false)) {
					}
					else {
						if (data != null && data.length() != 0) {

							((EditText) findViewById(R.id.text_uri)).setText(data);

							mProgressDialog = new ProgressDialog(this);
							mProgressDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
							mProgressDialog.setMessage(getResources().getString(R.string.web_progress_validating));
							mProgressDialog.show();

							ServiceRequest serviceRequest = new ServiceRequest();
							serviceRequest.setUri(data);
							serviceRequest.setRequestType(RequestType.Get);
							serviceRequest.setResponseFormat(ResponseFormat.Html);
							serviceRequest.setRequestListener(new RequestListener() {

								@Override
								public void onComplete(Object response) {
									
									ServiceResponse serviceResponse = (ServiceResponse) response;
									if (serviceResponse.responseCode != ResponseCode.Success) {
										mProgressDialog.dismiss();
										ImageUtils.showToastNotification(getResources().getString(R.string.web_alert_website_unavailable),
												Toast.LENGTH_SHORT);
									}
									else {
										Document document = Jsoup.parse((String) response);
										((EditText) findViewById(R.id.text_title)).setText(document.title());

										String description = null;
										Element element = document.select("meta[name=description]").first();
										if (element != null) {
											description = element.attr("content");
										}

										if (description == null) {
											element = document.select("p[class=description]").first();
											if (element != null) {
												description = element.text();
											}
										}
										if (description == null) {
											element = document.select("p").first();
											if (element != null) {
												description = element.text();
											}
										}

										if (description != null) {
											((EditText) findViewById(R.id.text_content)).setText(description);
										}
										else {
											((EditText) findViewById(R.id.text_content)).setText("");
										}
										mProgressDialog.dismiss();
									}
								}
							});

							NetworkManager.getInstance().requestAsync(serviceRequest);
						}
					}
				}
			default :
				break;
		}
	}

	@Override
	protected int getLayoutID() {
		return R.layout.web_form;
	}
}