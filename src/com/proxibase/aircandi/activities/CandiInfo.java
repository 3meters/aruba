package com.proxibase.aircandi.activities;

import java.util.List;

import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
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
import android.widget.Toast;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.proxibase.aircandi.candi.utils.CandiConstants;
import com.proxibase.aircandi.models.BaseEntity.SubType;
import com.proxibase.aircandi.utils.AircandiUI;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ProxiHandlerManager;
import com.proxibase.aircandi.utils.ImageManager.IImageReadyListener;
import com.proxibase.aircandi.utils.ImageManager.ImageFormat;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;
import com.proxibase.sdk.android.proxi.consumer.Beacon;
import com.proxibase.sdk.android.proxi.consumer.Command;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.consumer.ProxiExplorer;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.util.ProxiConstants;
import com.proxibase.sdk.android.util.Utilities;

public class CandiInfo extends AircandiActivity {

	private static String		COMPONENT_NAME	= "CandiDetail";
	private FrameLayout			mCandiSummaryView;
	private ScreenOrientation	mScreenOrientation;
	private ProxiHandlerManager	mProxiHandlerManager;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		super.onCreate(savedInstanceState);

		// Ui Hookup
		mCandiSummaryView = (FrameLayout) findViewById(R.id.CandiSummaryView);
		if (mEntityProxy != null) {
			setupSummary(mEntityProxy);
		}
	}

	@Override
	public void onBackPressed() {
		doBackPressed();
	}

	public void doBackPressed() {
		startTitlebarProgress();
		setResult(Activity.RESULT_FIRST_USER);
		finish();
		overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
	}

	@Override
	protected int getLayoutID() {
		return R.layout.candi_info;
	}

	private Intent buildIntent(Verb verb, EntityProxy entityProxy, SubType subType, int parentEntityId, Beacon beacon, User user, Class<?> clazz) {
		Intent intent = new Intent(CandiInfo.this, clazz);

		// We want to make sure that any child entities don't get serialized
		Gson gson = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {

			@Override
			public boolean shouldSkipClass(Class<?> clazz) {
				return (clazz == (Class<List<EntityProxy>>) (Class<?>) List.class);
			}

			@Override
			public boolean shouldSkipField(FieldAttributes f) {
				return (f.getDeclaredClass() == (Class<List<EntityProxy>>) (Class<?>) List.class);
			}
		}).create();

		if (verb != null)
			intent.putExtra(getString(R.string.EXTRA_VERB), verb);

		if (subType != null)
			intent.putExtra(getString(R.string.EXTRA_SUBTYPE), subType);

		if (parentEntityId != 0)
			intent.putExtra(getString(R.string.EXTRA_PARENT_ENTITY_ID), parentEntityId);

		if (beacon != null) {
			String jsonBeacon = gson.toJson(beacon);
			if (jsonBeacon != "")
				intent.putExtra(getString(R.string.EXTRA_BEACON), jsonBeacon);
		}

		if (entityProxy != null) {
			String jsonEntityProxy = gson.toJson(entityProxy);
			if (jsonEntityProxy != "")
				intent.putExtra(getString(R.string.EXTRA_ENTITY), jsonEntityProxy);
		}

		if (user != null) {
			String jsonUser = ProxibaseService.getGson(GsonType.Internal).toJson(user);
			if (jsonUser != "")
				intent.putExtra(getString(R.string.EXTRA_USER), jsonUser);
		}

		return intent;
	}

	public void onCommandButtonClick(View view) {

		Command command = (Command) view.getTag();
		String commandHandler = "com.proxibase.aircandi.activities." + command.handler;
		String commandName = command.name.toLowerCase();

		if (command.type.toLowerCase().equals("list")) {
			AircandiUI.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
			return;
		}

		try {

			if (command.verb.toLowerCase().equals("edit")) {
				SubType subType = mEntityProxy.entityType.equals(CandiConstants.TYPE_CANDI_TOPIC) ? SubType.Topic : SubType.Comment;
				Intent intent = buildIntent(Verb.Edit, mEntityProxy, subType, 0, null, mUser, Post.class);
				startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
				overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
				return;

			}

			if (commandName.toLowerCase().equals("audio")) {
				String audioFile = "";
				// if (command.entityResourceId.contains("hopper_house"))
				// audioFile = "hopper_house_by_the_railroad.mp3";
				// else if (command.entityResourceId.contains("klimt_hope"))
				// audioFile = "klimt_hope.mp3";
				// else if (command.entityResourceId.contains("monet_japanese"))
				// audioFile = "monet_japanese_footbridge.mp3";
				// else if (command.entityResourceId.contains("starry_night"))
				// audioFile = "vangogh_starry_night.mp3";
				// else if (command.entityResourceId.contains("cezanne"))
				// audioFile = "cezanne2.mp3";

				// MediaPlayer mediaPlayer = new MediaPlayer();
				// Uri uri = Uri.parse(Aircandi.URL_AIRCANDI_MEDIA + "audio.3meters.com/signsoflove.mp3");

				@SuppressWarnings("unused")
				Uri uri = Uri.parse("http://dev.aircandi.com/media/" + audioFile);

				// Uri url = Uri.parse("http://dev.aircandi.com/media/cezanne2.mp3");
				// AsyncPlayer player = new AsyncPlayer("Aircandi");
				// player.stop();
				// player.play(Tricorder.this, url, false, AudioManager.STREAM_MUSIC);

			}
			else if (commandName.toLowerCase().equals("video")) {
				// String movieUrl = Aircandi.URL_RIPPLEMEDIA +
				// "video/cezanne.mp3";
				// //String movieUrl =
				// "http://www.youtube.com/watch?v=q2mMSTlgWcU";
				// Intent tostart = new Intent(Intent.ACTION_VIEW);
				// tostart.setDataAndType(Uri.parse(movieUrl), "video/*");
				// startActivity(tostart);
			}
			else {
				Class clazz = Class.forName(commandHandler, false, this.getClass().getClassLoader());
				Intent intent = new Intent(this, clazz);

				GsonBuilder gsonb = new GsonBuilder();
				gsonb.setExclusionStrategies(new ExclusionStrategy() {

					@Override
					public boolean shouldSkipClass(Class<?> clazz) {
						return (clazz == (Class<List<EntityProxy>>) (Class<?>) List.class);
					}

					@Override
					public boolean shouldSkipField(FieldAttributes f) {
						return (f.getDeclaredClass() == (Class<List<EntityProxy>>) (Class<?>) List.class);
					}
				});

				Gson gson = gsonb.create();

				String jsonCommand = ProxibaseService.getGson(GsonType.Internal).toJson(command);

				// beacon has a circular ref back to entity proxy so fails when using internal
				String jsonEntityProxy = gson.toJson(mEntityProxy);
				String jsonUser = ProxibaseService.getGson(GsonType.Internal).toJson(this.mUser);
				intent.putExtra("Command", jsonCommand);
				intent.putExtra("EntityProxy", jsonEntityProxy);
				intent.putExtra("User", jsonUser);

				startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
				overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
			}
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		catch (IllegalStateException e) {
			e.printStackTrace();
		}
		catch (SecurityException exception) {
			exception.printStackTrace();
		}
	}

	public void onSummaryViewClick(View v) {
		doBackPressed();
	}

	private void setupSummary(final EntityProxy entityProxy) {

		new BuildMenuTask().execute(mCandiSummaryView);

		if (entityProxy.imageUri != null && entityProxy.imageUri != "") {
			if (ImageManager.getInstance().hasImage(entityProxy.imageUri)) {
				/*
				 * ImageView holds onto a reference to the bitmap inside a BitmapDrawable. 
				 * We don't want to recycle the bitmap while the ImageView needs it to 
				 * draw. Recyle will have to handled separately in the destroy
				 */
				Bitmap bitmap = ImageManager.getInstance().getImage(entityProxy.imageUri);
				if (bitmap != null) {
					((ImageView) mCandiSummaryView.findViewById(R.id.Image)).setImageBitmap(bitmap);

					if (ImageManager.getInstance().hasImage(entityProxy.imageUri + ".reflection")) {
						bitmap = ImageManager.getInstance().getImage(entityProxy.imageUri + ".reflection");
						((ImageView) mCandiSummaryView.findViewById(R.id.ImageReflection)).setImageBitmap(bitmap);
					}
				}
			}
			else {
				ImageRequest imageRequest = new ImageManager.ImageRequest();
				imageRequest.imageId = entityProxy.imageUri;
				imageRequest.imageUri = ProxiConstants.URL_PROXIBASE_MEDIA + "3meters_images/" + entityProxy.imageUri;
				imageRequest.imageFormat = entityProxy.imageFormat.equals("html") ? ImageFormat.Html : ImageFormat.Binary;

				imageRequest.imageShape = "square";
				imageRequest.widthMinimum = 250;
				imageRequest.showReflection = true;
				imageRequest.imageReadyListener = new IImageReadyListener() {

					@Override
					public void onImageReady(Bitmap bitmap) {

						Utilities.Log(CandiConstants.APP_NAME, "buildCandiDetailView", "Image fetched: " + entityProxy.imageUri);
						Bitmap bitmapNew = ImageManager.getInstance().getImage(entityProxy.imageUri);
						if (bitmapNew != null) {
							((ImageView) findViewById(R.id.Image)).setImageBitmap(bitmapNew);
							Bitmap bitmapReflection = ImageManager.getInstance().getImage(entityProxy.imageUri + ".reflection");
							if (bitmapReflection != null)
								((ImageView) findViewById(R.id.ImageReflection)).setImageBitmap(bitmapReflection);

							Animation animation = AnimationUtils.loadAnimation(CandiInfo.this, R.anim.fade_in_medium);
							animation.setFillEnabled(true);
							animation.setFillAfter(true);
							animation.setStartOffset(500);
							((ImageView) findViewById(R.id.Image)).startAnimation(animation);
							((ImageView) findViewById(R.id.ImageReflection)).startAnimation(animation);
						}
					}
				};
				Utilities.Log("Graffiti", "setupSummary", "Fetching Image: " + entityProxy.imageUri);
				ImageManager.getInstance().fetchImageAsynch(imageRequest);
			}
		}

		((TextView) findViewById(R.id.Subtitle)).setText("");
		((TextView) findViewById(R.id.Description)).setText("");

		((TextView) findViewById(R.id.Title)).setText(entityProxy.title);
		if (entityProxy.subtitle != null)
			((TextView) findViewById(R.id.Subtitle)).setText(Html.fromHtml(entityProxy.subtitle));
		if (entityProxy.description != null)
			((TextView) findViewById(R.id.Description)).setText(Html.fromHtml(entityProxy.description));

	}

	class PackageReceiver extends BroadcastReceiver {

		@Override
		// This is on the main UI thread
		public void onReceive(final Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
				String publicName = mProxiHandlerManager.getPublicName(intent.getData().getEncodedSchemeSpecificPart());
				AircandiUI.showToastNotification(CandiInfo.this, publicName + getText(R.string.toast_package_installed), Toast.LENGTH_SHORT);
			}
		}
	}

	public ScreenOrientation getScreenOrientation() {
		return this.mScreenOrientation;
	}

	public void setScreenOrientation(ScreenOrientation screenOrientation) {
		this.mScreenOrientation = screenOrientation;
	}

	class BuildMenuTask extends AsyncTask<FrameLayout, Void, TableLayout> {

		FrameLayout	frame;

		@Override
		protected TableLayout doInBackground(FrameLayout... params) {

			// We are on the background thread
			frame = params[0];
			boolean landscape = false;
			Display getOrient = getWindowManager().getDefaultDisplay();
			if (getOrient.getOrientation() == Surface.ROTATION_90 || getOrient.getOrientation() == Surface.ROTATION_270)
				landscape = true;
			TableLayout table = configureMenus(landscape, CandiInfo.this);

			return table;
		}

		@Override
		protected void onPostExecute(TableLayout table) {

			// We are on the UI thread
			super.onPostExecute(table);
			FrameLayout frame = (FrameLayout) findViewById(R.id.frameMenu);
			frame.removeAllViews();
			if (table != null)
				frame.addView(table);
		}
	}

	private TableLayout configureMenus(boolean landscape, Context context) {

		Boolean needMoreButton = false;

		if (mEntityProxy.commands == null || mEntityProxy.commands.size() == 0)
			return null;

		if (mEntityProxy.commands.size() > 6)
			needMoreButton = true;

		// Get the table we use for grouping and clear it
		final TableLayout table = new TableLayout(context);

		// Make the first row
		TableRow tableRow = (TableRow) this.getLayoutInflater().inflate(R.layout.temp_tablerow_commands, null);
		final TableRow.LayoutParams rowLp = new TableRow.LayoutParams();
		rowLp.setMargins(0, 0, 0, 0);
		TableLayout.LayoutParams tableLp;

		// Loop the streams
		Integer commandCount = 0;
		RelativeLayout commandButtonContainer;
		for (Command command : mEntityProxy.commands) {
			// Make a button and configure it
			commandButtonContainer = (RelativeLayout) this.getLayoutInflater().inflate(R.layout.temp_button_command, null);

			final TextView commandButton = (TextView) commandButtonContainer.findViewById(R.id.CommandButton);
			final TextView commandBadge = (TextView) commandButtonContainer.findViewById(R.id.CommandBadge);
			commandButtonContainer.setTag(command);
			if (needMoreButton && commandCount == 5) {
				commandButton.setText("More...");
				commandButton.setTag(command);
			}
			else {
				commandButton.setText(command.label);
				commandButton.setTag(command);
				commandBadge.setTag(command);
				commandBadge.setVisibility(View.INVISIBLE);
			}

			// Add button to row
			tableRow.addView(commandButtonContainer, rowLp);
			commandCount++;

			// If we have three in a row then commit it and make a new row
			int newRow = 2;
			if (landscape)
				newRow = 4;

			if (commandCount % newRow == 0) {
				tableLp = new TableLayout.LayoutParams();
				tableLp.setMargins(0, 0, 0, 0);
				table.addView(tableRow, tableLp);
				tableRow = (TableRow) this.getLayoutInflater().inflate(R.layout.temp_tablerow_commands, null);
			}
			else if (commandCount == 6)
				break;
		}

		// We might have an uncommited row with stream buttons in it
		if (commandCount != 3) {
			tableLp = new TableLayout.LayoutParams();
			tableLp.setMargins(0, 3, 0, 3);
			table.addView(tableRow, tableLp);
		}
		return table;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		/*
		 * TODO: Not getting called because setRequestedOrientation() is beinging
		 * called in BaseGameActivity
		 */
		Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "onConfigurationChanged called: " + newConfig.orientation);

		boolean landscape = false;
		Display getOrient = getWindowManager().getDefaultDisplay();
		if (getOrient.getOrientation() == Surface.ROTATION_90 || getOrient.getOrientation() == Surface.ROTATION_270)
			landscape = true;

		super.onConfigurationChanged(newConfig);
		TableLayout table = configureMenus(landscape, this);
		FrameLayout frame = (FrameLayout) findViewById(R.id.frameMenu);
		frame.removeAllViews();
		frame.addView(table);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		/*
		 * Called before onResume. If we are returning from the market app, we
		 * get a zero result code whether the user decided to start an install
		 * or not.
		 */
		if (requestCode == CandiConstants.ACTIVITY_ENTITY_HANDLER) {
			if (resultCode == Activity.RESULT_FIRST_USER) {
				Bundle extras = data.getExtras();
				if (extras != null) {
					String dirtyBeaconId = extras.getString(getString(R.string.EXTRA_BEACON_DIRTY));
					Integer dirtyEntityId = extras.getInt(getString(R.string.EXTRA_ENTITY_DIRTY));
					if (dirtyBeaconId != null && !dirtyBeaconId.equals("")) {
						for (Beacon beacon : ProxiExplorer.getInstance().getBeacons()) {
							if (beacon.id.equals(dirtyBeaconId)) {
								beacon.isDirty = true;
							}
						}
					}
					else if (dirtyEntityId != null) {
						for (EntityProxy entityProxy : ProxiExplorer.getInstance().getEntityProxiesFlat()) {
							if (entityProxy.id.equals(dirtyEntityId)) {
								entityProxy.isDirty = true;
							}
						}
					}
				}
			}
		}
		else if (requestCode == CandiConstants.ACTIVITY_MARKET) {
		}
	}

	@Override
	protected void onDestroy() {
		BitmapDrawable bd = (BitmapDrawable) ((ImageView) findViewById(R.id.Image)).getDrawable();
		Bitmap bm = bd.getBitmap();
		bm.recycle();
		super.onDestroy();
	}
}