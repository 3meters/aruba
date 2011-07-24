package com.proxibase.aircandi.controller;

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

import com.proxibase.aircandi.utilities.Utilities;
import com.proxibase.sdk.android.core.BaseQueryListener;
import com.proxibase.sdk.android.core.Entity;
import com.proxibase.sdk.android.core.EntityProxy;
import com.proxibase.sdk.android.core.ProxibaseRunner;
import com.proxibase.sdk.android.core.ProxibaseService.QueryFormat;

public class PointEditor extends AircandiActivity
{
	@SuppressWarnings("unused")
	private TextView	mTextBssid;
	@SuppressWarnings("unused")
	private TextView	mTextSsid;
	private TextView	mTextLevel;
	private EditText	mEditLabel;
	private CheckBox	mChkIsRipplePoint;
	private Button		mBtnSave;
	private EntityProxy		mEntity;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setContentView(R.layout.point_editor);
		super.onCreate(savedInstanceState);

		// Get the point we are rooted on
		mEntity = getCurrentEntity();
		if (mEntity != null)
		{
			mTextBssid = (TextView) findViewById(R.id.TextBssid);
			mTextSsid = (TextView) findViewById(R.id.TextSsid);
			mTextLevel = (TextView) findViewById(R.id.TextLevel);
			mEditLabel = (EditText) findViewById(R.id.EditLabel);
			mChkIsRipplePoint = (CheckBox) findViewById(R.id.ChkIsRipplePoint);
			mBtnSave = (Button) findViewById(R.id.BtnSave);

//			mTextSsid.setText(mEntity.ssid);
//			mTextBssid.setText(mEntity.bssid);
			mTextLevel.setText(Integer.toString(mEntity.beacon.levelDb));
			mEditLabel.setText(mEntity.entity.label);
			mChkIsRipplePoint.setChecked(!mEntity.beacon.isLocalOnly);

			mBtnSave.setTag(mEntity);
		}
		else
		{
			AircandiUI.showToastNotification(this, "No current point", Toast.LENGTH_SHORT);
			Utilities.Log(CandiConstants.APP_NAME, "PointEditor", "No current point.");
		}
	}

	public void onSaveClick(View view)
	{
		startProgress();
		processPoint();
	}

	public void processPoint()
	{
		Boolean changedLabel = false;
		Boolean changedRipplePoint = false;

		if (!mEntity.entity.label.equals(mEditLabel.getEditableText().toString()))
			changedLabel = true;
		if (mEntity.isTagged != mChkIsRipplePoint.isChecked())
			changedRipplePoint = true;

		if (!changedLabel && !changedRipplePoint)
			return;

		// Grab the values that might have been updated
		mEntity.label = mEditLabel.getEditableText().toString();
		mEntity.isTagged = mChkIsRipplePoint.isChecked();

		// A ripple point is getting turned off so delete it from the service
		ProxibaseRunner ripple = new ProxibaseRunner();
		if (changedRipplePoint)
		{
			Bundle parameters = new Bundle();
			if (mEntity.isTagged)
			{
				mEntity.isDirty = true;
				mEntity.isTagged = true;
//				parameters.putString("bssid", mEntity.bssid);
//				parameters.putString("ssid", mEntity.ssid);
				parameters.putString("label", mEntity.label);
				parameters.putString("userId", "11111111");
				ripple.post("InsertPoint", parameters, QueryFormat.Json, Aircandi.URL_AIRCANDI_SERVICE, new QueryListener());
			}
			else
			{
				mEntity.isTagged = false;
				parameters.putString("entityId", mEntity.entityId);
				ripple.post("DeletePoint", parameters, QueryFormat.Json, Aircandi.URL_AIRCANDI_SERVICE, new QueryListener());
			}
		}
		else if (changedLabel)
		{
			Bundle parameters = new Bundle();
			parameters.putString("entityId", mEntity.entityId);
			parameters.putString("label", mEntity.label);
			ripple.post("UpdatePoint", parameters, QueryFormat.Json, Aircandi.URL_AIRCANDI_SERVICE, new QueryListener());
		}
	}

	public class QueryListener extends BaseQueryListener
	{
		public void onComplete(String response)
		{
			// Post the processed result back to the UI thread
			PointEditor.this.runOnUiThread(new Runnable() {
				public void run()
				{
					stopProgress();
					AircandiUI.showToastNotification(PointEditor.this, "Saved", Toast.LENGTH_SHORT);
					Intent intent = new Intent(PointEditor.this, Tricorder.class);
					startActivity(intent);
				}
			});
		}

		@Override
		public void onClientProtocolException(ClientProtocolException e)
		{
			super.onClientProtocolException(e);
			// Post the processed result back to the UI thread
			PointEditor.this.runOnUiThread(new Runnable() {
				public void run()
				{
					stopProgress();
					AircandiUI.showToastNotification(PointEditor.this, "Failed to insert or modify point", Toast.LENGTH_SHORT);
				}
			});
		}

		@Override
		public void onIOException(IOException e)
		{
			super.onIOException(e);
			// Post the processed result back to the UI thread
			PointEditor.this.runOnUiThread(new Runnable() {
				public void run()
				{
					stopProgress();
					AircandiUI.showToastNotification(PointEditor.this, "Network error, failed to insert or modify point", Toast.LENGTH_SHORT);
				}
			});
		}
	}
	
	public void onCancelClick(View view)
	{
		Intent intent = new Intent(this, Tricorder.class);
		startActivity(intent);
	}
}