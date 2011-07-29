package com.proxibase.aircandi.activities;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.proxibase.aircandi.candi.utils.CandiConstants;
import com.proxibase.aircandi.utils.AircandiUI;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;
import com.proxibase.aircandi.utils.ImageManager.OnImageReadyListener;
import com.proxibase.sdk.android.proxi.consumer.Command;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.SimpleQueryListener;
import com.proxibase.sdk.android.util.ProxiConstants;
import com.proxibase.sdk.android.util.Utilities;

public class Claim extends AircandiActivity {

	private TextView	mTextClaimedByName;
	private TextView	mTextBssid;
	private TextView	mTextSsid;
	private TextView	mTextLevel;
	private EditText	mEditLabel;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.claim);

		// Ui Hookup
		if (super.mEntityProxy != null)
			setupLayout(mEntityProxy);
	}

	public void onSaveClick(View view) {
		startTitlebarProgress();
		processClaim();
	}

	public void onCancelClick(View view) {
		super.doBackPressed();
	}

	public void processClaim() {

		Boolean changedLabel = false;
		Boolean changedRipplePoint = false;

		if (!mEntityProxy.label.equals(mEditLabel.getEditableText().toString()))
			changedLabel = true;

		if (!changedLabel)
			return;

		// Grab the values that might have been updated
		mEntityProxy.label = mEntityProxy.beacon.label = mEditLabel.getEditableText().toString();

		// A ripple point is getting turned off so delete it from the service
		if (changedRipplePoint) {
			Bundle parameters = new Bundle();
			if (mEntityProxy.beacon.isLocalOnly) {
				mEntityProxy.beacon.isLocalOnly = true;
				// parameters.putString("bssid", mEntity.bssid);
				// parameters.putString("ssid", mEntity.ssid);
				parameters.putString("label", mEntityProxy.label);
				parameters.putString("userId", "11111111");
				ProxibaseService.getInstanceOf().postAsync("InsertPoint", parameters, ResponseFormat.Json, CandiConstants.URL_AIRCANDI_SERVICE, new MyQueryListener());
			}
			else {
				mEntityProxy.beacon.isLocalOnly = false;
				parameters.putString("entityId", mEntityProxy.entityProxyId);
				ProxibaseService.getInstanceOf().postAsync("DeletePoint", parameters, ResponseFormat.Json, CandiConstants.URL_AIRCANDI_SERVICE, new MyQueryListener());
			}
		}
		else if (changedLabel) {
			Bundle parameters = new Bundle();
			parameters.putString("entityId", mEntityProxy.entityProxyId);
			parameters.putString("label", mEntityProxy.label);
			ProxibaseService.getInstanceOf().postAsync("UpdatePoint", parameters, ResponseFormat.Json, CandiConstants.URL_AIRCANDI_SERVICE, new MyQueryListener());
		}
	}

	public class MyQueryListener extends SimpleQueryListener {

		public void onComplete(String response) {
			// Post the processed result back to the UI thread
			Claim.this.runOnUiThread(new Runnable() {

				public void run() {
					stopTitlebarProgress();
					AircandiUI.showToastNotification(Claim.this, "Saved", Toast.LENGTH_SHORT);
					Intent intent = new Intent(Claim.this, CandiSearchActivity.class);
					startActivity(intent);
				}
			});
		}

		@Override
		public void onClientProtocolException(ClientProtocolException e) {
			super.onClientProtocolException(e);
			// Post the processed result back to the UI thread
			Claim.this.runOnUiThread(new Runnable() {

				public void run() {
					stopTitlebarProgress();
					AircandiUI.showToastNotification(Claim.this, "Failed to insert or modify point", Toast.LENGTH_SHORT);
				}
			});
		}

		@Override
		public void onIOException(IOException e) {
			super.onIOException(e);
			// Post the processed result back to the UI thread
			Claim.this.runOnUiThread(new Runnable() {

				public void run() {
					stopTitlebarProgress();
					AircandiUI.showToastNotification(Claim.this, "Network error, failed to insert or modify point", Toast.LENGTH_SHORT);
				}
			});
		}
	}

	private void setupLayout(final EntityProxy entityProxy) {

		if (entityProxy != null) {
			mTextClaimedByName = (TextView) findViewById(R.id.TextClaimedByName);
			mTextBssid = (TextView) findViewById(R.id.TextBssid);
			mTextSsid = (TextView) findViewById(R.id.TextSsid);
			mTextLevel = (TextView) findViewById(R.id.TextLevel);
			mEditLabel = (EditText) findViewById(R.id.EditLabel);

			mTextClaimedByName.setText(mUser.name);
			mTextBssid.setText(entityProxy.beacon.beaconId);
			mTextSsid.setText(entityProxy.beacon.label);
			mTextLevel.setText(Integer.toString(entityProxy.beacon.levelDb));
			mEditLabel.setText(entityProxy.beacon.label);
		}
		else {
			AircandiUI.showToastNotification(this, "EntityProxy is null.", Toast.LENGTH_SHORT);
			Utilities.Log(CandiConstants.APP_NAME, "Claim", "EntityProxy is null.");
		}

		if (entityProxy.imageUri != null && entityProxy.imageUri != "") {
			if (ImageManager.getInstanceOf().hasImage(entityProxy.imageUri)) {
				Bitmap bitmap = ImageManager.getInstanceOf().getImage(entityProxy.imageUri);
				if (bitmap != null)
					((ImageView) findViewById(R.id.Image)).setImageBitmap(bitmap);

				if (ImageManager.getInstanceOf().hasImage(entityProxy.imageUri + ".reflection")) {
					bitmap = ImageManager.getInstanceOf().getImage(entityProxy.imageUri + ".reflection");
					((ImageView) findViewById(R.id.ImageReflection)).setImageBitmap(bitmap);
				}
			}
			else {
				ImageRequest imageRequest = new ImageManager.ImageRequest();
				imageRequest.imageId = entityProxy.imageUri;
				imageRequest.imageUrl = ProxiConstants.URL_PROXIBASE_MEDIA + "3meters_images/" + entityProxy.imageUri;
				imageRequest.imageShape = "square";
				imageRequest.widthMinimum = 80;
				imageRequest.showReflection = false;
				imageRequest.imageReadyListener = new OnImageReadyListener() {

					@Override
					public void onImageReady(Bitmap bitmap) {
						Utilities.Log("Graffiti", "setupSummary", "Image fetched: " + entityProxy.imageUri);
						Bitmap bitmapNew = ImageManager.getInstanceOf().getImage(entityProxy.imageUri);
						((ImageView) findViewById(R.id.Image)).setImageBitmap(bitmapNew);
						Animation animation = AnimationUtils.loadAnimation(Claim.this, R.anim.fade_in_medium);
						animation.setFillEnabled(true);
						animation.setFillAfter(true);
						animation.setStartOffset(500);
						((ImageView) findViewById(R.id.Image)).startAnimation(animation);

					}
				};
				Utilities.Log(CandiConstants.APP_NAME, "Claim", "Fetching Image: " + entityProxy.imageUri);
				ImageManager.getInstanceOf().fetchImageAsynch(imageRequest);
			}
		}
	}

	class BuildMenuTask extends AsyncTask<FrameLayout, Void, TableLayout> {

		FrameLayout	frame;

		@Override
		protected TableLayout doInBackground(FrameLayout... params) {

			// We are on the background thread
			frame = params[0];
			boolean landscape = false;
			Display getOrient = getWindowManager().getDefaultDisplay();
			if (getOrient.getRotation() == Surface.ROTATION_90 || getOrient.getRotation() == Surface.ROTATION_270)
				landscape = true;
			TableLayout table = configureMenus(mEntityProxy, landscape, Claim.this);

			return table;
		}

		@Override
		protected void onPostExecute(TableLayout table) {

			// We are on the UI thread
			super.onPostExecute(table);
			FrameLayout frame = (FrameLayout) findViewById(R.id.frameMenu);
			frame.removeAllViews();
			frame.addView(table);
		}
	}

	private TableLayout configureMenus(EntityProxy entityProxy, boolean landscape, Context context) {

		Boolean needMoreButton = false;
		if (entityProxy.commands.size() > 6)
			needMoreButton = true;

		// Get the table we use for grouping and clear it
		final TableLayout table = new TableLayout(context);

		// Make the first row
		TableRow tableRow = (TableRow) this.getLayoutInflater().inflate(R.layout.temp_tablerow_commands, null);
		final TableRow.LayoutParams rowLp = new TableRow.LayoutParams();
		rowLp.setMargins(0, 0, 0, 0);
		TableLayout.LayoutParams tableLp;

		// Loop the streams
		Integer streamCount = 0;
		RelativeLayout streamButtonContainer;
		for (Command command : entityProxy.commands) {
			// Make a button and configure it
			streamButtonContainer = (RelativeLayout) this.getLayoutInflater().inflate(R.layout.temp_button_command, null);

			final TextView streamButton = (TextView) streamButtonContainer.findViewById(R.id.StreamButton);
			final TextView streamBadge = (TextView) streamButtonContainer.findViewById(R.id.StreamBadge);
			streamButtonContainer.setTag(command);
			if (needMoreButton && streamCount == 5) {
				streamButton.setText("More...");
				streamButton.setTag(command);
			}
			else {
				streamButton.setText(command.label);
				streamButton.setTag(command);
				streamBadge.setTag(command);
				streamBadge.setVisibility(View.INVISIBLE);
			}

			// Add button to row
			tableRow.addView(streamButtonContainer, rowLp);
			streamCount++;

			// If we have three in a row then commit it and make a new row
			int newRow = 2;
			if (landscape)
				newRow = 4;

			if (streamCount % newRow == 0) {
				tableLp = new TableLayout.LayoutParams();
				tableLp.setMargins(0, 0, 0, 0);
				table.addView(tableRow, tableLp);
				tableRow = (TableRow) this.getLayoutInflater().inflate(R.layout.temp_tablerow_commands, null);
			}
			else if (streamCount == 6)
				break;
		}

		// We might have an uncommited row with stream buttons in it
		if (streamCount != 3) {
			tableLp = new TableLayout.LayoutParams();
			tableLp.setMargins(0, 3, 0, 3);
			table.addView(tableRow, tableLp);
		}
		return table;
	}

}