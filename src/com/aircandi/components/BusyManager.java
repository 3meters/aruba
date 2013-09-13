package com.aircandi.components;

import android.app.Activity;
import android.app.ProgressDialog;
import android.location.Location;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.widget.FrameLayout;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.utilities.UI;

public class BusyManager {

	private Activity		mActivity;
	private ProgressDialog	mProgressDialog;
	private View			mRefreshImage;
	private View			mRefreshProgress;
	private View			mAccuracyIndicator;

	public BusyManager(Activity activity) {
		mActivity = activity;
	}

	// --------------------------------------------------------------------------------------------
	// Location accuracy
	// --------------------------------------------------------------------------------------------

	public void updateAccuracyIndicator() {

		if (mAccuracyIndicator != null) {

			final Location location = LocationManager.getInstance().getLocationLocked();
			mActivity.runOnUiThread(new Runnable() {

				@Override
				public void run() {

					int sizeDip = 35;

					if (location != null && location.hasAccuracy()) {

						sizeDip = 35;

						if (location.getAccuracy() <= 100) {
							sizeDip = 25;
						}
						if (location.getAccuracy() <= 50) {
							sizeDip = 13;
						}
						if (location.getAccuracy() <= 30) {
							sizeDip = 7;
						}
						Logger.v(this, "Location accuracy: >>> " + String.valueOf(sizeDip));
					}

					final int sizePixels = UI.getRawPixelsForDisplayPixels(mActivity, sizeDip);
					final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(sizePixels, sizePixels, Gravity.CENTER);
					mAccuracyIndicator.setLayoutParams(layoutParams);
					mAccuracyIndicator.setBackgroundResource(R.drawable.bg_accuracy_indicator);
				}
			});
		}
	}

	public View getAccuracyIndicator() {
		return mAccuracyIndicator;
	}

	public void setAccuracyIndicator(View accuracyIndicator) {
		mAccuracyIndicator = accuracyIndicator;
	}

	// --------------------------------------------------------------------------------------------
	// UI progress and notifications
	// --------------------------------------------------------------------------------------------

	public void showBusy() {
		showBusy(null);
	}

	public void showBusy(Object message) {

		try {

			if (mActivity != null) {
				startActionbarBusyIndicator();
			}

			if (message != null) {
				final ProgressDialog progressDialog = getProgressDialog();
				if (message instanceof Integer) {
					progressDialog.setMessage(mActivity.getString((Integer) message));
				}
				else {
					progressDialog.setMessage((String) message);
				}
				if (!progressDialog.isShowing()) {
					progressDialog.setCancelable(false);
					progressDialog.show();
					if (Aircandi.displayMetrics != null) {
						progressDialog.getWindow().setLayout((int) (Aircandi.displayMetrics.widthPixels * 0.7), WindowManager.LayoutParams.WRAP_CONTENT);
					}
				}
			}
		}
		catch (BadTokenException e) {
			/*
			 * Sometimes the activity has been destroyed OUT from under us
			 * so we trap this and continue.
			 */
			e.printStackTrace();
		}
	}

	public void hideBusy() {
		final ProgressDialog progressDialog = getProgressDialog();
		if (progressDialog.isShowing() && progressDialog.getWindow().getWindowManager() != null) {
			try {
				progressDialog.dismiss();
			}
			catch (Exception e) {
				/*
				 * Sometime we get a harmless exception that the view is not attached to window manager.
				 * It could be that the activity is getting destroyed before the dismiss can happen.
				 * We catch it and move on.
				 */
				Logger.v(mActivity, e.getMessage());
			}
		}

		if (mActivity != null) {
			stopActionbarBusyIndicator();
		}
	}

	private void startActionbarBusyIndicator() {
		if (mRefreshImage != null) {
			mRefreshImage.setVisibility(View.GONE);
			mRefreshProgress.setVisibility(View.VISIBLE);
		}
	}

	private void stopActionbarBusyIndicator() {
		if (mRefreshImage != null) {
			mRefreshProgress.setVisibility(View.GONE);
			mRefreshImage.setVisibility(View.VISIBLE);
		}
	}

	public void startBodyBusyIndicator() {
		final View progress = mActivity.findViewById(R.id.progress);
		if (progress != null) {
			progress.setVisibility(View.VISIBLE);
		}
	}

	public void stopBodyBusyIndicator() {
		final View progress = mActivity.findViewById(R.id.progress);
		if (progress != null) {
			progress.setVisibility(View.GONE);
		}
	}

	public ProgressDialog getProgressDialog() {

		if (mProgressDialog == null) {
			/* Dialogs */
			mProgressDialog = new ProgressDialog(mActivity);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setCancelable(true);
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
			mProgressDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
					WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
		}

		return mProgressDialog;
	}

	public View getRefreshImage() {
		return mRefreshImage;
	}

	public void setRefreshImage(View refreshImage) {
		mRefreshImage = refreshImage;
	}

	public View getRefreshProgress() {
		return mRefreshProgress;
	}

	public void setRefreshProgress(View refreshProgress) {
		mRefreshProgress = refreshProgress;
	}

}
