package com.aircandi.ui.builders;

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

import com.aircandi.CandiConstants;
import com.aircandi.R;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.FontManager;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.SearchAdapter;
import com.aircandi.components.SearchAdapter.SearchListViewHolder;
import com.aircandi.components.SearchManager;
import com.aircandi.components.SearchManager.SearchItem;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.ServiceRequest;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.MiscUtils;

@SuppressWarnings("ucd")
public class LinkPicker extends FormActivity {

	private TextView			mTitle;
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
			mVerifyUri = extras.getBoolean(CandiConstants.EXTRA_VERIFY_URI, false);
			mUri = extras.getString(CandiConstants.EXTRA_URI);
		}

		mListView = (ListView) findViewById(R.id.form_list);
		mOkButton = (Button) findViewById(R.id.button_ok);
		mTestButton = (Button) findViewById(R.id.button_test);
		mTextUri = (EditText) findViewById(R.id.uri);

		mTitle = (TextView) findViewById(R.id.title);
		mTitle.setText(R.string.dialog_link_picker_title);

		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.title));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_ok));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_test));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_cancel));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.uri));

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

		bind();
	}

	private void bind() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy(R.string.progress_searching);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("GetBookmarks");				
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
				mCommon.hideBusy(false);
			}

		}.execute();
	}

	@SuppressWarnings("ucd")
	public void onListItemClick(View view) {
		SearchItem searchItem = (SearchItem) ((SearchListViewHolder) view.getTag()).data;
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

		if (!MiscUtils.validWebUri(linkUri)) {
			mCommon.showAlertDialogSimple(null, getString(R.string.error_weburi_invalid));
		}
		else {
			Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
			intent.setData(Uri.parse(linkUri));
			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
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

			if (!MiscUtils.validWebUri(linkUri)) {
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
						mCommon.showBusy(R.string.progress_verifying);
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
							mCommon.hideBusy(false);

							Intent intent = new Intent();
							intent.putExtra(CandiConstants.EXTRA_URI, mUri);
							intent.putExtra(CandiConstants.EXTRA_URI_TITLE, mUriTitle);
							intent.putExtra(CandiConstants.EXTRA_URI_DESCRIPTION, mUriDescription);
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
				intent.putExtra(CandiConstants.EXTRA_URI, mUri);
				intent.putExtra(CandiConstants.EXTRA_URI_TITLE, mUriTitle);
				intent.putExtra(CandiConstants.EXTRA_URI_DESCRIPTION, mUriDescription);
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
		return R.layout.picker_link;
	}

	@Override
	protected Boolean isDialog() {
		return true;
	}

}