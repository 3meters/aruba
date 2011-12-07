package com.proxibase.aircandi;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

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
import android.widget.TextView;
import android.widget.Toast;

import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.WebEntity;
import com.proxibase.aircandi.utils.Exceptions;
import com.proxibase.aircandi.utils.ImageUtils;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.IQueryListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class WebForm extends EntityBaseForm {

	@Override
	protected void bindEntity() {

		/* We handle all the elements that are different than the base entity. */
		if (mCommand.verb.equals("new")) {
			WebEntity entity = new WebEntity();
			entity.entityType = CandiConstants.TYPE_CANDI_WEB;
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
			String jsonResponse = null;
			try {
				jsonResponse = (String) ProxibaseService.getInstance().select(mEntityProxy.getEntryUri(), ResponseFormat.Json);
				mEntity = (WebEntity) ProxibaseService.convertJsonToObject(jsonResponse, WebEntity.class, GsonType.ProxibaseService);
			}
			catch (ProxibaseException exception) {
				Exceptions.Handle(exception);
			}
		}
		super.bindEntity();
	}

	@Override
	protected void drawEntity() {
		super.drawEntity();

		((TextView) findViewById(R.id.txt_header_title)).setText(getResources().getString(R.string.form_title_web));

		if (mCommand.verb.equals("edit")) {
			((EditText) findViewById(R.id.txt_uri)).setText(((WebEntity) mEntity).contentUri);
		}

		if (findViewById(R.id.chk_html_zoom) != null) {
			((CheckBox) findViewById(R.id.chk_html_zoom)).setVisibility(View.VISIBLE);
			if (((WebEntity)mEntity).imageFormat != null) {
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
		String uri = ((EditText) findViewById(R.id.txt_uri)).getText().toString().trim();
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
		entity.contentUri = ((EditText) findViewById(R.id.txt_uri)).getText().toString().trim();
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
												getResources().getString(R.string.dialog_link_bookmark),
												getResources().getString(R.string.dialog_link_search) };
				AlertDialog.Builder builder = new AlertDialog.Builder(WebForm.this);
				builder.setTitle(getResources().getString(R.string.dialog_link_message));
				builder.setCancelable(true);

				builder.setOnCancelListener(new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
					}
				});

				builder.setNegativeButton(getResources().getString(R.string.dialog_link_negative), new DialogInterface.OnClickListener() {

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

							((EditText) findViewById(R.id.txt_uri)).setText(data);

							mProgressDialog = new ProgressDialog(this);
							mProgressDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
							mProgressDialog.setMessage("Validating website...");
							mProgressDialog.show();

							ProxibaseService.getInstance().selectAsync(data, ResponseFormat.Html, new IQueryListener() {

								@Override
								public void onComplete(final String response) {
									runOnUiThread(new Runnable() {

										@Override
										public void run() {
											Document document = Jsoup.parse(response);
											((EditText) findViewById(R.id.txt_title)).setText(document.title());

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
												((EditText) findViewById(R.id.txt_content)).setText(description);
											}
											else {
												((EditText) findViewById(R.id.txt_content)).setText("");
											}
											mProgressDialog.dismiss();
										}
									});
								}

								@Override
								public void onProxibaseException(ProxibaseException exception) {
									if (!Exceptions.Handle(exception))
									{
										runOnUiThread(new Runnable() {

											@Override
											public void run() {
												mProgressDialog.dismiss();
												ImageUtils.showToastNotification(WebForm.this, "Website unavailable", Toast.LENGTH_SHORT);
											}
										});
									}
								}
							});
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