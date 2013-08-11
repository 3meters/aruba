package com.aircandi.ui.base;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.beta.R;
import com.aircandi.components.BusProvider;
import com.aircandi.components.BusyManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MenuManager;

public abstract class BaseFragment extends SherlockFragment {

	protected Bundle		mExtras;
	protected Resources		mResources;
	protected BusyManager	mBusyManager;

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
		mExtras = getSherlockActivity().getIntent().getExtras();
		mResources = getResources();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Logger.d(this, "Fragment create view");
		return inflater.inflate(getLayoutId(), container, false);
	}

	protected void draw() {}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	public void onRefresh() {}

	public void onAdd() {}

	public void onHelp() {}

	public void showBusy() {
		startBodyBusyIndicator();
	}

	public void hideBusy() {
		stopBodyBusyIndicator();
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
		final View progress = getView().findViewById(R.id.progress);
		if (progress != null) {
			progress.setVisibility(View.VISIBLE);
		}
	}

	private void stopBodyBusyIndicator() {
		final View progress = getView().findViewById(R.id.progress);
		if (progress != null) {
			progress.setVisibility(View.GONE);
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
			if (mBusyManager != null) {
				mBusyManager.setRefreshImage(refresh.getActionView().findViewById(R.id.refresh_image));
				mBusyManager.setRefreshProgress(refresh.getActionView().findViewById(R.id.refresh_progress));
			}

			refresh.getActionView().findViewById(R.id.refresh_frame).setTag(refresh);
			refresh.getActionView().findViewById(R.id.refresh_frame).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					MenuItem item = (MenuItem) view.getTag();
					onOptionsItemSelected(item);
				}
			});
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
		 * User might be leaving fragment so do any work needed
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

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public enum ServiceOperation {
		Signin,
		PasswordChange,
	}

	public static class SimpleTextWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {}
	}
}