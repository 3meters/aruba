package com.proxibase.aircandi;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.BookmarkAdapter;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.components.Utilities;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.ServiceRequest;

public class BookmarkPicker extends FormActivity implements OnItemClickListener {

	private ListView		mListView;
	private EditText		mTextUri;
	private String			mUri;
	private String			mUriTitle;
	private String			mUriDescription;
	private BookmarkAdapter	mBookmarkAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
	}

	private void initialize() {
		mCommon.track();

		/* Action bar */
		mCommon.mActionBar.setTitle(R.string.form_title_bookmarks);

		mBookmarkAdapter = new BookmarkAdapter(this, "");
		mListView = (ListView) findViewById(R.id.list_bookmarks);
		mListView.setAdapter(mBookmarkAdapter);
		mListView.setOnItemClickListener(this);
		mListView.setDivider(null);

		mTextUri = (EditText) findViewById(R.id.text_uri);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		mUri = mBookmarkAdapter.getUrl(position);
		mUriTitle = mBookmarkAdapter.getTitle(position);

		doVerify();
	}

	public void onLinkGoClick(View view) {
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
				AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert, null,
						getResources().getString(R.string.alert_weburi_invalid), this, android.R.string.ok, null, null);
				return false;
			}
		}
		return true;
	}

	private void doVerify() {

		if (validate()) {

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

						ServiceRequest serviceRequest = new ServiceRequest();
						serviceRequest.setUri(linkUri)
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
						AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert, null,
								getResources().getString(R.string.alert_weburi_unreachable), BookmarkPicker.this, android.R.string.ok, null, null);
					}
				}
			}.execute();
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
		return R.layout.bookmark_picker;
	}

}