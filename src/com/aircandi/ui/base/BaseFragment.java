package com.aircandi.ui.base;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.components.BusProvider;
import com.aircandi.components.BusyManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MenuManager;
import com.aircandi.components.Tracker;
import com.aircandi.service.objects.CacheStamp;

public abstract class BaseFragment extends SherlockFragment implements IForm {

	protected Resources		mResources;
	protected BusyManager	mBusyManager;
	protected CacheStamp	mCacheStamp;
	protected Handler		mHandler	= new Handler();

	@Override
	public void onAttach(Activity activity) {
		/* Called when the fragment has been associated with the activity. */
		super.onAttach(activity);
		mBusyManager = new BusyManager(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * Triggers fragment menu construction in some android versions
		 * so mBusyManager must have already been created.
		 */
		setHasOptionsMenu(true);
		mResources = getResources();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Logger.d(this, "Fragment create view");
		if (getSherlockActivity() == null || getSherlockActivity().isFinishing()) {
			return null;
		}		
		return inflater.inflate(getLayoutId(), container, false);
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void unpackIntent() {}

	@Override
	public void initialize(Bundle savedInstanceState) {}

	public void beforeDatabind() {}

	@Override
	public void databind(BindingMode mode) {}

	public void afterDatabind() {}

	@Override
	public void draw() {}

	@Override
	public void onRefresh() {
		databind(BindingMode.SERVICE); // Called from Routing
	}

	@Override
	public void onAdd() {}

	@Override
	public void onHelp() {}

	@Override
	public void onError() {}

	@Override
	public void showBusy() {
		showBusy(null, false);
	}

	@Override
	public void showBusy(final Object message, final Boolean actionBarOnly) {
		if (getSherlockActivity() != null) {
			getSherlockActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (mBusyManager != null) {
						mBusyManager.showBusy(message);
						if (actionBarOnly == null || !actionBarOnly) {
							startBodyBusyIndicator();
						}
					}
				}
			});
		}
	}

	@Override
	public void hideBusy() {

		if (getSherlockActivity() != null) {
			getSherlockActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (mBusyManager != null) {
						mBusyManager.hideBusy();
						stopBodyBusyIndicator();
					}
				}
			});
		}
	}

	@Override
	public void showBusyTimed(final Integer duration, final Boolean actionBarOnly) {
		getSherlockActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (mBusyManager != null) {
					mBusyManager.showBusy();
					if (actionBarOnly == null || !actionBarOnly) {
						startBodyBusyIndicator();
					}
					mHandler.postDelayed(new Runnable() {

						@Override
						public void run() {
							hideBusy();
						}
					}, duration);
				}
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// UI
	// --------------------------------------------------------------------------------------------

	protected int getLayoutId() {
		return 0;
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	private void startBodyBusyIndicator() {
		if (getView() != null) {
			final View progress = getView().findViewById(R.id.progress);
			if (progress != null) {
				progress.setVisibility(View.VISIBLE);
			}
		}
	}

	private void stopBodyBusyIndicator() {
		if (getView() != null) {
			final View progress = getView().findViewById(R.id.progress);
			if (progress != null) {
				progress.setVisibility(View.GONE);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MenuManager.onCreateOptionsMenu(this, menu, inflater);

		MenuItem refresh = menu.findItem(R.id.refresh);
		if (refresh != null) {
			View actionView = refresh.getActionView();
			if (actionView != null) {
				if (mBusyManager != null) {
					mBusyManager.setRefreshImage(actionView.findViewById(R.id.refresh_image));
					mBusyManager.setRefreshProgress(actionView.findViewById(R.id.refresh_progress));
				}

				actionView.findViewById(R.id.refresh_frame).setTag(refresh);
				actionView.findViewById(R.id.refresh_frame).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						MenuItem item = (MenuItem) view.getTag();
						onOptionsItemSelected(item);
					}
				});
			}
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/*
		 * The action bar home/up action should open or close the drawer.
		 * ActionBarDrawerToggle will take care of this.
		 */
		if (item.getItemId() == R.id.refresh) {
			Tracker.sendEvent("ui_action", "form_refresh_by_user", this.getClass().getSimpleName(), 0, Aircandi.getInstance().getCurrentUser());
			onRefresh();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	public void onStart() {
		Logger.d(this, "Fragment start");
		super.onStart();
	}

	@Override
	public void onResume() {
		Logger.d(this, "Fragment resume");
		BusProvider.getInstance().register(this);
		super.onResume();
	}

	@Override
	public void onPause() {
		/*
		 * user might be leaving fragment so do any work needed
		 * because they might not come back.
		 */
		Logger.d(this, "Fragment pause");
		BusProvider.getInstance().unregister(this);
		super.onPause();
	}

	@Override
	public void onStop() {
		Logger.d(this, "Fragment stop");
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		Logger.d(this, "Fragment destroy view");
		super.onDestroy();
	}

	@Override
	public void onDestroy() {
		Logger.d(this, "Fragment destroy");
		super.onDestroy();
	}
}