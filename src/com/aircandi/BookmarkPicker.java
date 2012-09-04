package com.aircandi;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.aircandi.components.BookmarkAdapter;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.Utilities;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.ServiceRequest;

public class BookmarkPicker extends FormActivity implements OnItemClickListener {

	private ListView		mListView;
	private EditText		mTextUri;
	private String			mUri;
	private String			mUriTitle;
	private String			mUriDescription;
	private Button			mOkButton;
	private BookmarkAdapter	mBookmarkAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
	}

	private void initialize() {

		mBookmarkAdapter = new BookmarkAdapter(this, "");
		mListView = (ListView) findViewById(R.id.list_bookmarks);
		mListView.setAdapter(mBookmarkAdapter);
		mListView.setOnItemClickListener(this);
		mListView.setDivider(null);

		mOkButton = (Button) findViewById(R.id.btn_ok);
		mTextUri = (EditText) findViewById(R.id.text_uri);
		mTextUri.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mOkButton.setEnabled(mOkButton.getText().length() > 0);
			}
		});
		
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		mUri = mBookmarkAdapter.getUrl(position);
		mUriTitle = mBookmarkAdapter.getTitle(position);
		mTextUri.setText(mUri);
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
						mCommon.handleServiceError(serviceResponse, ServiceOperation.PickBookmark);
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