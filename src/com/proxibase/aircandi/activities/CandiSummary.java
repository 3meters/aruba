package com.proxibase.aircandi.activities;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.proxibase.aircandi.activities.R;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;
import com.proxibase.aircandi.utils.ImageManager.IImageReadyListener;
import com.proxibase.sdk.android.proxi.consumer.Command;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.util.ProxiConstants;
import com.proxibase.sdk.android.util.Utilities;

public class CandiSummary extends Activity {

	private FrameLayout	mCandiSummaryView;
	private ImageView	mProgressIndicator;
	private ImageView	mButtonRefresh;
	private EntityProxy	mEntityProxy;
	private TextView	mContextButton;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.candi_summary);

		// Ui Hookup
		mCandiSummaryView = (FrameLayout) findViewById(R.id.CandiSummaryView);

		mProgressIndicator = (ImageView) findViewById(R.id.Application_ProgressIndicator);
		if (mProgressIndicator != null)
			mProgressIndicator.setVisibility(View.INVISIBLE);

		mButtonRefresh = (ImageView) findViewById(R.id.Application_Button_Refresh);
		if (mButtonRefresh != null)
			mButtonRefresh.setVisibility(View.VISIBLE);

		mContextButton = (TextView) findViewById(R.id.Context_Button);
		if (mContextButton != null)
			mContextButton.setVisibility(View.VISIBLE);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			String jsonStream = extras.getString("ProxiEntity");
			if (jsonStream != "") {
				showBackButton(true);
				mEntityProxy = ProxibaseService.getGson(GsonType.ProxibaseService).fromJson(getIntent().getExtras().getString("EntityProxy"),
						EntityProxy.class);
			}
			setupSummary(mEntityProxy);
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

	public void onSummaryViewClick(View v) {
		doBackPressed();
	}

	private void setupSummary(final EntityProxy entityProxy) {

		new BuildMenuTask().execute(mCandiSummaryView);

		if (entityProxy.imageUri != null && entityProxy.imageUri != "") {
			if (ImageManager.getInstance().hasImage(entityProxy.imageUri)) {
				Bitmap bitmap = ImageManager.getInstance().getImage(entityProxy.imageUri);
				if (bitmap != null)
					((ImageView) mCandiSummaryView.findViewById(R.id.Image)).setImageBitmap(bitmap);

				if (ImageManager.getInstance().hasImage(entityProxy.imageUri + ".reflection")) {
					bitmap = ImageManager.getInstance().getImage(entityProxy.imageUri + ".reflection");
					((ImageView) mCandiSummaryView.findViewById(R.id.ImageReflection)).setImageBitmap(bitmap);
				}
			}
			else {
				ImageRequest imageRequest = new ImageManager.ImageRequest();
				imageRequest.imageId = entityProxy.imageUri;
				imageRequest.imageUri = ProxiConstants.URL_PROXIBASE_MEDIA + "3meters_images/" + entityProxy.imageUri;
				imageRequest.imageShape = "square";
				imageRequest.widthMinimum = 80;
				imageRequest.showReflection = false;
				imageRequest.imageReadyListener = new IImageReadyListener() {

					@Override
					public void onImageReady(Bitmap bitmap) {
						Utilities.Log("Graffiti", "setupSummary", "Image fetched: " + entityProxy.imageUri);
						Bitmap bitmapNew = ImageManager.getInstance().getImage(entityProxy.imageUri);
						((ImageView) mCandiSummaryView.findViewById(R.id.Image)).setImageBitmap(bitmapNew);
						Animation animation = AnimationUtils.loadAnimation(CandiSummary.this, R.anim.fade_in_medium);
						animation.setFillEnabled(true);
						animation.setFillAfter(true);
						animation.setStartOffset(500);
						((ImageView) mCandiSummaryView.findViewById(R.id.Image)).startAnimation(animation);

					}
				};
				Utilities.Log("Graffiti", "setupSummary", "Fetching Image: " + entityProxy.imageUri);
				ImageManager.getInstance().fetchImageAsynch(imageRequest);
			}
		}

		((TextView) mCandiSummaryView.findViewById(R.id.Title)).setText(entityProxy.title);
		((TextView) mCandiSummaryView.findViewById(R.id.Subtitle)).setText(Html.fromHtml(entityProxy.subtitle));
		((TextView) mCandiSummaryView.findViewById(R.id.Description)).setText(Html.fromHtml(entityProxy.description));
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
			TableLayout table = configureMenus(mEntityProxy, landscape, CandiSummary.this);

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

	protected void startTitlebarProgress() {
		if (mProgressIndicator != null) {
			mProgressIndicator.setVisibility(View.VISIBLE);
			mButtonRefresh.setVisibility(View.INVISIBLE);
			mProgressIndicator.bringToFront();
			AnimationDrawable animation = (AnimationDrawable) mProgressIndicator.getBackground();
			animation.start();
			mProgressIndicator.invalidate();
		}
	}

	protected void stopTitlebarProgress() {
		if (mProgressIndicator != null) {
			mProgressIndicator.setAnimation(null);
			mButtonRefresh.setVisibility(View.VISIBLE);
			mButtonRefresh.bringToFront();
			mProgressIndicator.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onBackPressed() {
		Utilities.Log("Graffiti", "onBackPressed", "Back pressed");
		doBackPressed();
	}

	public void doBackPressed() {
		startTitlebarProgress();
		super.onBackPressed();
		overridePendingTransition(R.anim.fade_in_medium, R.anim.summary_out);

		// overridePendingTransition(R.anim.fade_in_medium, R.anim.fade_out_medium);
	}

}