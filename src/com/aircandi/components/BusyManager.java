package com.aircandi.components;

import android.app.Activity;
import android.app.ProgressDialog;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;

import com.aircandi.Aircandi;
import com.aircandi.beta.R;

public class BusyManager {

	private Activity		mActivity;
	private ProgressDialog	mProgressDialog;
	private View			mRefreshImage;
	private View			mRefreshProgress;

	public BusyManager(Activity activity) {
		mActivity = activity;
	}

	// --------------------------------------------------------------------------------------------
	// UI progress and notifications
	// --------------------------------------------------------------------------------------------

	public void showBusy() {
		showBusy(null);
	}

	public void showBusy(Integer messageResId) {

		try {

			startActionbarBusyIndicator();

			if (messageResId != null) {
				final ProgressDialog progressDialog = getProgressDialog();
				progressDialog.setMessage(mActivity.getString(messageResId));
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
			 * Sometimes the activity has been destroyed out from under us
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

		stopActionbarBusyIndicator();
		stopBodyBusyIndicator();
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

	private void stopBodyBusyIndicator() {
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
