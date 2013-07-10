package com.aircandi.ui.helpers;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ShortcutListAdapter;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequest.ImageResponse;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ObjectType;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.objects.Applink;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.widgets.BounceListView;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.Routing;

public class ShortcutPicker extends BaseActivity {

	private BounceListView			mList;
	private final List<Shortcut>	mShortcuts	= new ArrayList<Shortcut>();
	private Entity					mEntity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (!isFinishing()) {
			initialize();
			bind();
		}
	}

	private void initialize() {

		mActionBar.setDisplayHomeAsUpEnabled(true);

		/* We use this to access the source suggestions */
		final Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			final List<String> jsonShortcuts = extras.getStringArrayList(Constants.EXTRA_SHORTCUTS);
			if (jsonShortcuts != null) {
				for (String jsonShortcut : jsonShortcuts) {
					Shortcut shortcut = (Shortcut) HttpService.jsonToObject(jsonShortcut, ObjectType.Shortcut);
					mShortcuts.add(shortcut);
				}
			}

			final String jsonEntity = extras.getString(Constants.EXTRA_ENTITY);
			if (jsonEntity != null) {
				mEntity = (Entity) HttpService.jsonToObject(jsonEntity, ObjectType.Entity);
			}
		}

		mList = (BounceListView) findViewById(R.id.list);

		if (mShortcuts != null && mShortcuts.size() > 0) {
			mActionBar.setTitle(mShortcuts.get(0).app);

			/* Show default photo based on the type of the shortcut set */
			Photo photo = new Photo(Applink.getDefaultPhotoUri(mShortcuts.get(0).schema), null, null, null, PhotoSource.assets);
			final BitmapRequest bitmapRequest = new BitmapRequest();
			bitmapRequest.setImageUri(photo.getUri());
			bitmapRequest.setImageRequestor(this);
			bitmapRequest.setRequestListener(new RequestListener() {

				@Override
				public void onComplete(Object response) {

					final ServiceResponse serviceResponse = (ServiceResponse) response;
					if (serviceResponse.responseCode == ResponseCode.Success) {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								final ImageResponse imageResponse = (ImageResponse) serviceResponse.data;
								mActionBar.setIcon(new BitmapDrawable(Aircandi.applicationContext.getResources(), imageResponse.bitmap));
							}
						});
					}
				}
			});
			BitmapManager.getInstance().masterFetch(bitmapRequest);
		}
	}

	private void bind() {
		final ShortcutListAdapter adapter = new ShortcutListAdapter(this, mShortcuts, R.layout.temp_listitem_shortcut_picker);
		mList.setAdapter(adapter);
		mBusyManager.hideBusy(); // Visible by default
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onListItemClick(View view) {
		View label = (View) view.findViewById(R.id.photo);
		Shortcut shortcut = (Shortcut) label.getTag();
		Routing.shortcut(this, shortcut, mEntity);
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.accept) {
			return true;
		}
		else if (item.getItemId() == R.id.cancel) {
			setResult(Activity.RESULT_CANCELED);
			finish();
			Animate.doOverridePendingTransition(ShortcutPicker.this, TransitionType.FormToPage);
			return true;
		}

		/* In case we add general menu items later */
		super.onOptionsItemSelected(item);
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.picker_link;
	}
}