package com.aircandi.ui;

import android.os.Bundle;
import android.view.View;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.service.objects.LinkOptions.LinkProfile;
import com.aircandi.ui.base.BaseShortcutFragment;

@SuppressWarnings("ucd")
public class WatchingFragment extends BaseShortcutFragment {

	protected static int	mScrollX	= 0;
	protected static int	mScrollY	= -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mLinkProfile = LinkProfile.LINKS_FOR_USER_CURRENT;
		mShortcutType = Constants.TYPE_LINK_WATCH;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mEntityId = Aircandi.getInstance().getCurrentUser().id;
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	public void draw() {
		super.draw();
		if (mScrollView != null) {
			mScrollView.post(new Runnable() {
				@Override
				public void run() {
					mScrollView.scrollTo(mScrollX, mScrollY);
				}
			});
		}
	}

	@Override
	protected void showMessage(Boolean visible) {
		if (mMessage != null) {
			mMessage.setText(R.string.list_watching_empty);
			mMessage.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	public void onPause() {
		super.onPause();
		mScrollX = mScrollView.getScrollX();
		mScrollY = mScrollView.getScrollY();
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.shortcut_fragment;
	}

}