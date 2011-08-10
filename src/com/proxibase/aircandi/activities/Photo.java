package com.proxibase.aircandi.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.proxibase.aircandi.candi.utils.CandiConstants;
import com.proxibase.aircandi.models.ClaimEntity;
import com.proxibase.aircandi.utils.AircandiUI;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.consumer.Beacon.BeaconType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.SimpleModifyListener;
import com.proxibase.sdk.android.util.ProxiConstants;

public class Photo extends AircandiActivity {

	private TextView	mTextClaimedByName;
	private TextView	mTextBssid;
	private TextView	mTextSsid;
	private EditText	mEditLabel;
	private ClaimEntity	mClaimEntity;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		super.onCreate(savedInstanceState);

		// Ui Hookup
		if (super.mEntityProxy != null) {
			bindEntity();
			bindLayout();
		}
	}

	public void onSaveButtonClick(View view) {
		startTitlebarProgress();
		doSave();
	}

	public void onCancelButtonClick(View view) {
		startTitlebarProgress();
		setResult(Activity.RESULT_CANCELED);
		finish();
		overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
	}

	public void bindEntity() {
		if (mEntityProxy != null && mEntityProxy.entityUri == "") {
			/*
			 * Creating a new claim
			 */
			mClaimEntity = new ClaimEntity();
			mClaimEntity.beaconId = mEntityProxy.beacon.id;
			mClaimEntity.entityType = CandiConstants.TYPE_CANDI_CLAIM;
			mClaimEntity.label = mEntityProxy.label;
			mClaimEntity.imageUri = mUser.imageUri;
			mClaimEntity.signalFence = -100f;
			mClaimEntity.dataBound = false;
			mClaimEntity.createdById = String.valueOf(mUser.id);
			mClaimEntity.claimedById = String.valueOf(mUser.id);
		}
		else {
			/*
			 * Binding to existing claim
			 */
			String jsonResponse = ProxibaseService.getInstance().selectAsString(mEntityProxy.entityUri, ResponseFormat.Json);
			mClaimEntity = (ClaimEntity) ProxibaseService.convertJsonToObject(jsonResponse, ClaimEntity.class);
			mClaimEntity.dataBound = true;
		}
	}

	private void bindLayout() {

		if (mActivityMode == ActivityMode.New) {
			if (mEntityProxy != null) {
				mTextClaimedByName = (TextView) findViewById(R.id.TextClaimedByName);
				mTextBssid = (TextView) findViewById(R.id.TextBssid);
				mTextSsid = (TextView) findViewById(R.id.TextSsid);
				mEditLabel = (EditText) findViewById(R.id.EditLabel);

				mTextClaimedByName.setText(mUser.name);
				mTextBssid.setText(mClaimEntity.beaconId);
				mTextSsid.setText(mEntityProxy.beacon.ssid);
				mEditLabel.setText(mClaimEntity.label);
			}
		}
	}

	public void doSave() {

		// Insert beacon if it isn't already registered
		if (!mEntityProxy.beaconRegistered) {
			mEntityProxy.beacon.registeredById = String.valueOf(mUser.id);
			mEntityProxy.beacon.beaconType = BeaconType.Fixed.name().toLowerCase();
			mEntityProxy.beacon.beaconSetId = ProxiConstants.BEACONSET_WORLD;
			mEntityProxy.beacon.insert();
		}
		if (mClaimEntity.dataBound) {
			updateEntity();
		}
		else {
			insertEntity();
		}
	}

	private void insertEntity() {

		mClaimEntity.label = mEditLabel.getEditableText().toString();
		mClaimEntity.insertAsync(new SimpleModifyListener() {

			@Override
			public void onComplete(String jsonResponse) {
				if (jsonResponse != "") {
					ClaimEntity claimEntityInserted = (ClaimEntity) ProxibaseService.convertJsonToObject(jsonResponse, ClaimEntity.class);

					final EntityProxy entityProxy = claimEntityInserted.getEntityProxy();
					entityProxy.insertAsync(new SimpleModifyListener() {

						@Override
						public void onComplete(final String jsonResponse) {

							// Post the processed result back to the UI thread
							Photo.this.runOnUiThread(new Runnable() {

								public void run() {
									stopTitlebarProgress();
									AircandiUI.showToastNotification(Photo.this, "Saved", Toast.LENGTH_SHORT);
									Intent intent = new Intent();
									String jsonEntityProxy = ProxibaseService.getGson(GsonType.Internal).toJson(entityProxy);

									if (jsonEntityProxy != "") {
										intent.putExtra("EntityProxyUpdated", jsonEntityProxy);
									}

									setResult(Activity.RESULT_FIRST_USER, intent);
									finish();
									overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
								}
							});

						}
					});

				}
			}
		});
	}

	private void updateEntity() {
		mClaimEntity.label = mEditLabel.getEditableText().toString();
		mClaimEntity.updateAsync(new SimpleModifyListener() {

			@Override
			public void onComplete(String response) {
				// Post the processed result back to the UI thread
				Photo.this.runOnUiThread(new Runnable() {

					public void run() {
						stopTitlebarProgress();
						AircandiUI.showToastNotification(Photo.this, "Saved", Toast.LENGTH_SHORT);
						Intent intent = new Intent();
						EntityProxy entityProxy = mClaimEntity.getEntityProxy();
						String jsonEntityProxy = ProxibaseService.getGson(GsonType.Internal).toJson(entityProxy);

						if (jsonEntityProxy != "") {
							intent.putExtra("EntityProxyUpdated", jsonEntityProxy);
						}

						setResult(Activity.RESULT_FIRST_USER, intent);
						finish();
						overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
					}
				});

			}
		});
	}

	@Override
	protected int getLayoutID() {
		return R.layout.claim;
	}
}