package com.aircandi.ui.builders;

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
import com.aircandi.components.LinkListAdapter;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequest.ImageResponse;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.HttpService.ServiceDataType;
import com.aircandi.service.objects.Applink;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.ui.widgets.BounceListView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

public class LinkPicker extends FormActivity {

	private BounceListView		mList;
	private final List<Entity>	mApplinks	= new ArrayList<Entity>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (!isFinishing()) {
			initialize();
			bind();
			mCommon.hideBusy(true); // Visible by default
		}
	}

	private void initialize() {

		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);

		/* We use this to access the source suggestions */
		final Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			final List<String> jsonApplinks = extras.getStringArrayList(Constants.EXTRA_ENTITIES);
			if (jsonApplinks != null) {
				for (String jsonApplink : jsonApplinks) {
					Applink entity = (Applink) HttpService.convertJsonToObjectInternalSmart(jsonApplink, ServiceDataType.Applink);
					mApplinks.add(entity);
				}
			}
		}
		if (mApplinks != null && mApplinks.size() > 0) {
			mCommon.mActionBar.setTitle(mApplinks.get(0).type);
			Photo photo = new Photo(Applink.getDefaultIcon(mApplinks.get(0).type), null, null, null, PhotoSource.assets);
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
								mCommon.mActionBar.setIcon(new BitmapDrawable(Aircandi.applicationContext.getResources(), imageResponse.bitmap));
							}
						});
					}
				}
			});
			BitmapManager.getInstance().masterFetch(bitmapRequest);
		}
		mList = (BounceListView) findViewById(R.id.list);
	}

	private void bind() {
		final LinkListAdapter adapter = new LinkListAdapter(this, mApplinks, R.layout.temp_listitem_link_picker);
		mList.setAdapter(adapter);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {
		setResult(Activity.RESULT_CANCELED);
		finish();
		AnimUtils.doOverridePendingTransition(LinkPicker.this, TransitionType.FormToPage);
	}

	@SuppressWarnings("ucd")
	public void onListItemClick(View view) {
		View label = (View) view.findViewById(R.id.image);
		Applink entity = (Applink) label.getTag();
		mCommon.routeApplink(entity, null);
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.accept) {
			return true;
		}
		else if (item.getItemId() == R.id.cancel) {
			setResult(Activity.RESULT_CANCELED);
			finish();
			AnimUtils.doOverridePendingTransition(LinkPicker.this, TransitionType.FormToPage);
			return true;
		}

		/* In case we add general menu items later */
		mCommon.doOptionsItemSelected(item);
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