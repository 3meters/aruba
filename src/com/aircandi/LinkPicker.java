package com.aircandi;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.app.Activity;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.AnimUtils;
import com.aircandi.components.AnimUtils.TransitionType;
import com.aircandi.components.GeoLocationManager;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.SearchAdapter;
import com.aircandi.components.SearchAdapter.SearchListViewHolder;
import com.aircandi.components.SearchManager;
import com.aircandi.components.SearchManager.SearchItem;
import com.aircandi.components.Utilities;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.ServiceRequest;

public class LinkPicker extends FormActivity {

	private ListView			mListView;
	private EditText			mTextUri;
	private String				mUri;
	private String				mUriTitle;
	private String				mUriDescription;
	private Button				mOkButton;
	private Button				mTestButton;
	private Boolean				mVerifyUri		= false;
	private List<SearchItem>	mSearchItems	= new ArrayList<SearchItem>();
	private SearchAdapter		mSearchAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
	}

	private void initialize() {

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mVerifyUri = extras.getBoolean(getString(R.string.EXTRA_VERIFY_URI), false);
		}

		mListView = (ListView) findViewById(R.id.list_bookmarks);
		mOkButton = (Button) findViewById(R.id.btn_ok);
		mTestButton = (Button) findViewById(R.id.btn_link_test);
		mTextUri = (EditText) findViewById(R.id.text_uri);
		mTextUri.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mOkButton.setEnabled(mTextUri.getText().length() > 0);
				mTestButton.setEnabled(mTextUri.getText().length() > 0);
			}
		});

		/* Get location support setup */
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		GeoLocationManager.getInstance().ensureLocation(GeoLocationManager.MINIMUM_ACCURACY
				, GeoLocationManager.MAXIMUM_AGE
				, criteria, new RequestListener() {

					@Override
					public void onComplete(Object response) {
						Location location = (Location) response;
						if (location != null) {
							bind();
						}

					}
				});

	}

	public void bind() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, getString(R.string.progress_searching));
			}

			@Override
			protected Object doInBackground(Object... params) {

				List<SearchItem> placeSuggestions = SearchManager.getInstance().getPlaceSuggestions();
				if (placeSuggestions != null) {
					mSearchItems.addAll(placeSuggestions);
				}
				List<SearchItem> bookmarks = SearchManager.getInstance().getBookmarks(getContentResolver());
				if (bookmarks != null) {
					mSearchItems.addAll(bookmarks);
				}
				return null;
			}

			@Override
			protected void onPostExecute(Object response) {
				mSearchAdapter = new SearchAdapter(LinkPicker.this, mSearchItems, null);
				mListView.setAdapter(mSearchAdapter);
				mCommon.showProgressDialog(false, null);
			}

		}.execute();
	}

	public void onListItemClick(View view) {
		SearchItem searchItem = (SearchItem) ((SearchListViewHolder) view.getTag()).data;
		mUri = searchItem.uri;
		mUriTitle = searchItem.name;
		mTextUri.setText(mUri);
	}

	public void onLinkTestButtonClick(View view) {
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse(mTextUri.getText().toString()));
		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

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
				mCommon.showAlertDialogSimple(null, getString(R.string.error_weburi_invalid));
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
						mCommon.showProgressDialog(true, getString(R.string.progress_verifying));
					}

					@Override
					protected Object doInBackground(Object... params) {
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

							ServiceRequest serviceRequest = new ServiceRequest()
									.setUri(linkUri)
									.setRequestType(RequestType.Get)
									.setResponseFormat(ResponseFormat.Html)
									.setSuppressUI(true);

							serviceResponse = NetworkManager.getInstance().request(serviceRequest);
						}

						return serviceResponse;
					}

					@Override
					protected void onPostExecute(Object response) {
						ServiceResponse serviceResponse = (ServiceResponse) response;
						if (serviceResponse.responseCode == ResponseCode.Success) {

							Document document = Jsoup.parse((String) serviceResponse.data);
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
							mCommon.showProgressDialog(false, null);

							Intent intent = new Intent();
							intent.putExtra(getString(R.string.EXTRA_URI), mUri);
							intent.putExtra(getString(R.string.EXTRA_URI_TITLE), mUriTitle);
							intent.putExtra(getString(R.string.EXTRA_URI_DESCRIPTION), mUriDescription);
							setResult(Activity.RESULT_OK, intent);
							finish();
						}
						else {
							mCommon.handleServiceError(serviceResponse, ServiceOperation.PickBookmark);
						}
					}
				}.execute();
			}
			else {
				Intent intent = new Intent();
				intent.putExtra(getString(R.string.EXTRA_URI), mUri);
				intent.putExtra(getString(R.string.EXTRA_URI_TITLE), mUriTitle);
				intent.putExtra(getString(R.string.EXTRA_URI_DESCRIPTION), mUriDescription);
				setResult(Activity.RESULT_OK, intent);
				finish();
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected int getLayoutID() {
		return R.layout.link_picker;
	}

}