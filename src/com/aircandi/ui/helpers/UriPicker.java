package com.aircandi.ui.helpers;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.SearchAdapter;
import com.aircandi.components.SearchAdapter.SearchListViewHolder;
import com.aircandi.components.SearchManager;
import com.aircandi.components.SearchManager.SearchItem;
import com.aircandi.service.HttpService.RequestType;
import com.aircandi.service.HttpService.ResponseFormat;
import com.aircandi.service.ServiceRequest;
import com.aircandi.ui.base.BaseBrowse;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Utilities;

@SuppressWarnings("ucd")
public class UriPicker extends BaseBrowse {

	private TextView				mName;
	private ListView				mListView;
	private EditText				mTextUri;
	private String					mUri;
	private String					mUriTitle;
	private String					mUriDescription;
	private Button					mOkButton;
	private Button					mTestButton;
	private Boolean					mVerifyUri		= false;
	private final List<SearchItem>	mSearchItems	= new ArrayList<SearchItem>();
	private SearchAdapter			mSearchAdapter;
	
	@Override
	protected void unpackIntent() {
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mVerifyUri = extras.getBoolean(Constants.EXTRA_VERIFY_URI, false);
			mUri = extras.getString(Constants.EXTRA_URI);
		}
	}

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mListView = (ListView) findViewById(R.id.form_list);
		mOkButton = (Button) findViewById(R.id.button_ok);
		mTestButton = (Button) findViewById(R.id.button_test);
		mTextUri = (EditText) findViewById(R.id.uri);

		mName = (TextView) findViewById(R.id.name);
		mName.setText(R.string.dialog_link_picker_title);

		if (mUri != null) {
			mTextUri.setText(mUri);
		}
		mTextUri.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mOkButton.setEnabled(mTextUri.getText().length() > 0);
				mTestButton.setEnabled(mTextUri.getText().length() > 0);
			}
		});
	}

	@Override
	protected void databind(Boolean refresh) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy(R.string.progress_searching);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("GetBookmarks");
				final List<SearchItem> bookmarks = SearchManager.getInstance().getBookmarks(getContentResolver());
				if (bookmarks != null) {
					mSearchItems.addAll(bookmarks);
				}
				return null;
			}

			@Override
			protected void onPostExecute(Object response) {
				mSearchAdapter = new SearchAdapter(UriPicker.this, mSearchItems, null);
				mListView.setAdapter(mSearchAdapter);
				mBusyManager.hideBusy();
			}

		}.execute();
	}

	@SuppressWarnings("ucd")
	public void onListItemClick(View view) {
		final SearchItem searchItem = (SearchItem) ((SearchListViewHolder) view.getTag()).data;
		mUri = searchItem.uri;
		mUriTitle = searchItem.name;
		mTextUri.setText(mUri);
	}

	@SuppressWarnings("ucd")
	public void onLinkTestButtonClick(View view) {
		String linkUri = mTextUri.getText().toString();

		if (!linkUri.startsWith("http://") && !linkUri.startsWith("https://")) {
			linkUri = "http://" + linkUri;
		}

		if (!Utilities.validWebUri(linkUri)) {
			Dialogs.alertDialogSimple(this, null, getString(R.string.error_weburi_invalid));
		}
		else {
			final Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
			intent.setData(Uri.parse(linkUri));
			startActivity(intent);
			Animate.doOverridePendingTransition(this, TransitionType.PageToSource);
		}
	}

	@SuppressWarnings("ucd")
	public void onOkButtonClick(View view) {
		mUri = mTextUri.getText().toString();
		doVerify();
	}

	private boolean validate() {
		/*
		 * We only validate the web address if the form had an input for it and
		 * the user set it to something.
		 */
		String linkUri = mUri;
		if (mUri != null && !mUri.equals("")) {

			if (!mUri.startsWith("http://") && !mUri.startsWith("https://")) {
				linkUri = "http://" + mUri;
			}

			if (!Utilities.validWebUri(linkUri)) {
				Dialogs.alertDialogSimple(this, null, getString(R.string.error_weburi_invalid));
				return false;
			}
		}
		return true;
	}

	private void doVerify() {

		if (validate()) {
			if (mVerifyUri) {
				new AsyncTask() {

					@Override
					protected void onPreExecute() {
						mBusyManager.showBusy(R.string.progress_verifying);
					}

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("GetHtml");
						ServiceResponse serviceResponse = new ServiceResponse();
						/*
						 * If using uri then we have already checked to see if it is a well formed
						 * web address by now
						 */

						String linkUri = mUri;
						if (mUri != null && !mUri.equals("")) {
							if (!mUri.startsWith("http://") && !mUri.startsWith("https://")) {
								linkUri = "http://" + mUri;
							}

							final ServiceRequest serviceRequest = new ServiceRequest()
									.setUri(linkUri)
									.setRequestType(RequestType.Get)
									.setResponseFormat(ResponseFormat.Html)
									.setSuppressUI(true);

							serviceResponse = NetworkManager.getInstance().request(serviceRequest, null);
						}

						return serviceResponse;
					}

					@Override
					protected void onPostExecute(Object response) {
						final ServiceResponse serviceResponse = (ServiceResponse) response;
						if (serviceResponse.responseCode == ResponseCode.Success) {

							final Document document = Jsoup.parse((String) serviceResponse.data);
							if (mUriTitle == null) {
								mUriTitle = document.title();
							}

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
								mUriDescription = description;
							}
							mBusyManager.hideBusy();

							final Intent intent = new Intent();
							intent.putExtra(Constants.EXTRA_URI, mUri);
							intent.putExtra(Constants.EXTRA_URI_TITLE, mUriTitle);
							intent.putExtra(Constants.EXTRA_URI_DESCRIPTION, mUriDescription);
							setResult(Activity.RESULT_OK, intent);
							finish();
						}
						else {
							Routing.serviceError(UriPicker.this, serviceResponse);
						}
					}
				}.execute();
			}
			else {
				final Intent intent = new Intent();
				intent.putExtra(Constants.EXTRA_URI, mUri);
				intent.putExtra(Constants.EXTRA_URI_TITLE, mUriTitle);
				intent.putExtra(Constants.EXTRA_URI_DESCRIPTION, mUriDescription);
				setResult(Activity.RESULT_OK, intent);
				finish();
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.picker_uri;
	}

	@Override
	protected Boolean isDialog() {
		return true;
	}

}