package com.proxibase.aircandi.activities;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.proxibase.aircandi.candi.utils.CandiConstants;
import com.proxibase.aircandi.utils.AircandiUI;
import com.proxibase.aircandi.utils.Utilities;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.service.ProxibaseRunner;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.BaseQueryListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.QueryFormat;

public class PointEditor extends AircandiActivity {

	@SuppressWarnings("unused")
	private TextView	mTextBssid;
	@SuppressWarnings("unused")
	private TextView	mTextSsid;
	private TextView	mTextLevel;
	private EditText	mEditLabel;
	private CheckBox	mChkIsRipplePoint;
	private Button		mBtnSave;
	private EntityProxy	mEntityProxy;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.point_editor);
		super.onCreate(savedInstanceState);

		if (mEntityProxy != null) {
			mTextBssid = (TextView) findViewById(R.id.TextBssid);
			mTextSsid = (TextView) findViewById(R.id.TextSsid);
			mTextLevel = (TextView) findViewById(R.id.TextLevel);
			mEditLabel = (EditText) findViewById(R.id.EditLabel);
			mChkIsRipplePoint = (CheckBox) findViewById(R.id.ChkIsRipplePoint);
			mBtnSave = (Button) findViewById(R.id.BtnSave);

			// mTextSsid.setText(mEntity.ssid);
			// mTextBssid.setText(mEntity.bssid);
			mTextLevel.setText(Integer.toString(mEntityProxy.beacon.levelDb));
			mEditLabel.setText(mEntityProxy.entity.label);
			mChkIsRipplePoint.setChecked(!mEntityProxy.beacon.isLocalOnly);

			mBtnSave.setTag(mEntityProxy);
		}
		else {
			AircandiUI.showToastNotification(this, "No current point", Toast.LENGTH_SHORT);
			Utilities.Log(CandiConstants.APP_NAME, "PointEditor", "No current point.");
		}
	}
	

	public void showBackButton(boolean show) {

		if (show) {
			mContextButton.setText("Back");
			mContextButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					doBackPressed();
				}
			});
		}
	}
	

	public void onSaveClick(View view) {
		startProgress();
		processPoint();
	}

	public void processPoint() {
		Boolean changedLabel = false;
		Boolean changedRipplePoint = false;

		if (!mEntityProxy.entity.label.equals(mEditLabel.getEditableText().toString()))
			changedLabel = true;
		if (mEntityProxy.isTagged != mChkIsRipplePoint.isChecked())
			changedRipplePoint = true;

		if (!changedLabel && !changedRipplePoint)
			return;

		// Grab the values that might have been updated
		mEntityProxy.label = mEditLabel.getEditableText().toString();
		mEntityProxy.isTagged = mChkIsRipplePoint.isChecked();

		// A ripple point is getting turned off so delete it from the service
		ProxibaseRunner ripple = new ProxibaseRunner();
		if (changedRipplePoint) {
			Bundle parameters = new Bundle();
			if (mEntityProxy.isTagged) {
				mEntityProxy.isDirty = true;
				mEntityProxy.isTagged = true;
				// parameters.putString("bssid", mEntity.bssid);
				// parameters.putString("ssid", mEntity.ssid);
				parameters.putString("label", mEntityProxy.label);
				parameters.putString("userId", "11111111");
				ripple.post("InsertPoint", parameters, QueryFormat.Json, Aircandi.URL_AIRCANDI_SERVICE, new QueryListener());
			}
			else {
				mEntityProxy.isTagged = false;
				parameters.putString("entityId", mEntityProxy.entityId);
				ripple.post("DeletePoint", parameters, QueryFormat.Json, Aircandi.URL_AIRCANDI_SERVICE, new QueryListener());
			}
		}
		else if (changedLabel) {
			Bundle parameters = new Bundle();
			parameters.putString("entityId", mEntityProxy.entityId);
			parameters.putString("label", mEntityProxy.label);
			ripple.post("UpdatePoint", parameters, QueryFormat.Json, Aircandi.URL_AIRCANDI_SERVICE, new QueryListener());
		}
	}

	public class QueryListener extends BaseQueryListener {

		public void onComplete(String response) {
			// Post the processed result back to the UI thread
			PointEditor.this.runOnUiThread(new Runnable() {

				public void run() {
					stopProgress();
					AircandiUI.showToastNotification(PointEditor.this, "Saved", Toast.LENGTH_SHORT);
					Intent intent = new Intent(PointEditor.this, Tricorder.class);
					startActivity(intent);
				}
			});
		}

		@Override
		public void onClientProtocolException(ClientProtocolException e) {
			super.onClientProtocolException(e);
			// Post the processed result back to the UI thread
			PointEditor.this.runOnUiThread(new Runnable() {

				public void run() {
					stopProgress();
					AircandiUI.showToastNotification(PointEditor.this, "Failed to insert or modify point", Toast.LENGTH_SHORT);
				}
			});
		}

		@Override
		public void onIOException(IOException e) {
			super.onIOException(e);
			// Post the processed result back to the UI thread
			PointEditor.this.runOnUiThread(new Runnable() {

				public void run() {
					stopProgress();
					AircandiUI.showToastNotification(PointEditor.this, "Network error, failed to insert or modify point", Toast.LENGTH_SHORT);
				}
			});
		}
	}

	public void onCancelClick(View view) {
		Intent intent = new Intent(this, Tricorder.class);
		startActivity(intent);
	}
}