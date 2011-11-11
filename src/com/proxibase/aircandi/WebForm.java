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
import android.widget.EditText;
import android.widget.Toast;

import com.proxibase.aircandi.R;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.WebEntity;
import com.proxibase.aircandi.utils.ImageUtils;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.IQueryListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class WebForm extends EntityBase {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bindEntity();
		drawEntity();
	}

	protected void bindEntity() {

		/* We handle all the elements that are different than the base entity. */
		if (mCommand.verb.equals("new")) {
			mEntity = new WebEntity();
			((WebEntity) mEntity).entityType = CandiConstants.TYPE_CANDI_WEB;
		}
		else if (mCommand.verb.equals("edit")) {
			String jsonResponse = null;
			try {
				jsonResponse = (String) ProxibaseService.getInstance().select(mEntityProxy.getEntryUri(), ResponseFormat.Json);
			}
			catch (ProxibaseException exception) {
				exception.printStackTrace();
			}

			mEntity = (WebEntity) ProxibaseService.convertJsonToObject(jsonResponse, WebEntity.class, GsonType.ProxibaseService);
		}

		super.bindEntity();
	}

	protected void drawEntity() {
		super.drawEntity();

		if (mCommand.verb.equals("edit")) {
			((EditText) findViewById(R.id.txt_uri)).setText(((WebEntity) mEntity).contentUri);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onLinkBuilderClick(View view) {
		showLinkBuilderDialog();
	}

	// --------------------------------------------------------------------------------------------
	// Picker routines
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void doSave() {
		super.doSave();

		if (!validate()) {
			return;
		}

		if (mCommand.verb.equals("new")) {
			insertEntity();
		}
		else if (mCommand.verb.equals("edit")) {
			updateEntity();
		}
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
	protected void insertEntity() {
		final WebEntity entity = (WebEntity) mEntity;
		entity.contentUri = ((EditText) findViewById(R.id.txt_uri)).getText().toString().trim();
		entity.imageUri = entity.contentUri;
		entity.imageFormat = "html";
		super.insertEntity();
	}

	@Override
	protected void updateEntity() {
		final WebEntity entity = (WebEntity) mEntity;
		entity.contentUri = ((EditText) findViewById(R.id.txt_uri)).getText().toString().trim();
		entity.imageUri = entity.contentUri;
		entity.imageFormat = "html";
		super.updateEntity();
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	protected void showLinkBuilderDialog() {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {

				final CharSequence[] items = { "Select a bookmark", "Browse to find site" };
				AlertDialog.Builder builder = new AlertDialog.Builder(WebForm.this);
				builder.setTitle("Build link...");
				builder.setCancelable(true);

				builder.setOnCancelListener(new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						mProcessing = false;
					}
				});

				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						mProcessing = false;
					}
				});

				builder.setItems(items, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int item) {
						if (item == 0) {

							final Intent intent = new Intent(Intent.ACTION_MAIN, null);
							intent.addCategory(Intent.CATEGORY_LAUNCHER);
							final ComponentName cm = new ComponentName("com.android.browser", "com.android.browser.CombinedBookmarkHistoryActivity");
							intent.setComponent(cm);
							//intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivityForResult(intent, 1);

							//							Class clazz = null;
							//							try {
							//								clazz = Class.forName("com.android.browser.CombinedBookmarkHistoryActivity", false, this.getClass().getClassLoader());
							//							}
							//							catch (ClassNotFoundException exception) {
							//								exception.printStackTrace();
							//							}
							//							Intent intentFromClass = new Intent(WebForm.this, clazz);
							//							startActivityForResult(intentFromClass, CandiConstants.ACTIVITY_ENTITY_HANDLER);

							//							Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
							//							photoPickerIntent.setType("image/*");
							//							startActivityForResult(photoPickerIntent, CandiConstants.ACTIVITY_PHOTO_PICK);
							//
							//
							//							String[] projection = new String[] { Browser.BookmarkColumns._ID,
							//																	Browser.BookmarkColumns.TITLE,
							//																	Browser.BookmarkColumns.URL };
							//
							//							Cursor cursor = managedQuery(android.provider.Browser.BOOKMARKS_URI, projection, Browser.BookmarkColumns.BOOKMARK, null, null);

						}
						else if (item == 1) {
							//Uri uriUrl = Uri.parse("http://androidbook.blogspot.com/");
							//Intent launchBrowser = new Intent(Intent.ACTION_WEB_SEARCH, uriUrl);
							Intent launchBrowser = new Intent(Intent.ACTION_WEB_SEARCH);
							launchBrowser.putExtra(SearchManager.QUERY, "cat humor");
							startActivityForResult(launchBrowser, 10);
							overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
							mProcessing = false;
							dialog.dismiss();
						}
						else {
							mProcessing = false;
							Toast.makeText(getApplicationContext(), "Not implemented yet.", Toast.LENGTH_SHORT).show();
						}
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
			}
		});
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
									runOnUiThread(new Runnable() {

										@Override
										public void run() {
											mProgressDialog.dismiss();
											ImageUtils.showToastNotification(WebForm.this, "Website unavailable", Toast.LENGTH_SHORT);
										}
									});
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