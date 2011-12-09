/*
 * Copyright (C) 2009 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.proxibase.aircandi.widgets;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.QuickContact;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.proxibase.aircandi.R;
import com.proxibase.aircandi.candi.models.CandiModel;
import com.proxibase.aircandi.utils.ImageUtils;

/**
 * Window that shows QuickContact dialog for a specific {@link Contacts#_ID}.
 */
@SuppressWarnings("unused")
public class ActionsWindow {

	private final LayoutInflater	mInflater;
	private PopupWindow				mPopupWindow;
	private final Rect				mRect		= new Rect();
	private boolean					mActionStripShowing	= false;
	private long					mActionStripToggleTime;
	private RootLayout				mRootView;
	private ViewGroup				mTrackSet;
	private HorizontalScrollView	mTrackScroll;
	private Animation				mTrackAnim;

	private OnDismissListener		mDismissListener;

	private int						mScreenWidth;
	private int						mScreenHeight;
	private int						mRequestedY;

	private ImageView				mArrowUp;
	private ImageView				mArrowDown;

	/**
	 * Prepare a dialog to show in the given {@link Context}.
	 */
	public ActionsWindow(Context context) {
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		initialize(context);
	}

	private void initialize(Context context) {

		WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		mScreenWidth = windowManager.getDefaultDisplay().getWidth();
		mScreenHeight = windowManager.getDefaultDisplay().getHeight();

		final Resources res = context.getResources();
		int shadowHoriz = res.getDimensionPixelSize(R.dimen.quickcontact_shadow_horiz);
		int shadowVert = res.getDimensionPixelSize(R.dimen.quickcontact_shadow_vert);

		int width = mScreenWidth;
		// width = ImageUtils.getRawPixelsForDisplayPixels(context.getResources().getDisplayMetrics(), 200);
		// width = mScreenWidth;

		mPopupWindow = new PopupWindow(context, null, android.R.style.Widget_PopupWindow);
		mPopupWindow.setWidth(width);
		mPopupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
		mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
		mPopupWindow.setOutsideTouchable(true);
		mPopupWindow.setContentView(mInflater.inflate(R.layout.actionstrip, null));
		mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {

			@Override
			public void onDismiss() {
				setShowing(false);
			}
		});

		mRootView = (RootLayout) mPopupWindow.getContentView().findViewById(R.id.root);
		mRootView.mQuickContactWindow = this;
		mRootView.setFocusable(true);
		mRootView.setFocusableInTouchMode(true);
		mRootView.setDescendantFocusability(RootLayout.FOCUS_AFTER_DESCENDANTS);

		mArrowUp = (ImageView) mRootView.findViewById(R.id.arrow_up);
		mArrowDown = (ImageView) mRootView.findViewById(R.id.arrow_down);
		mTrackSet = (ViewGroup) mRootView.findViewById(R.id.track_set);
		mTrackScroll = (HorizontalScrollView) mRootView.findViewById(R.id.scroll);

		mTrackAnim = AnimationUtils.loadAnimation(context, R.anim.quickcontact);
		mTrackAnim.setInterpolator(new DecelerateInterpolator());
//		mTrackAnim.setInterpolator(new Interpolator() {
//
//			public float getInterpolation(float t) {
//				/*
//				 * Pushes past the target area, then snaps back into place.
//				 * Equation for graphing: 1.2-((x*1.6)-1.1)^2
//				 */
//				final float inner = (t * 1.55f) - 1.1f;
//				return 1.2f - inner * inner;
//			}
//		});
	}

	public synchronized void show(Rect anchor, View trackContent, View anchorView, int bodyOffsetX, int bodyOffsetY, int arrowOffsetX) {

		if (isShowing()) {
			dismiss();
			return;
		}

		/* Validate incoming parameters */
		if (anchor == null) {
			throw new IllegalArgumentException("Missing anchor rectangle");
		}

		if (trackContent == null) {
			throw new IllegalArgumentException("Content for action strip is null");
		}

		/* Prepare header view for requested mode */
		resetTrack();
		mTrackSet.addView(trackContent);

		/*
		 * We need to have a focused view inside the QuickContact window so
		 * that the BACK key event can be intercepted
		 */
		mRootView.requestFocus();

		setShowing(true);
		mTrackSet.startAnimation(mTrackAnim);

		mPopupWindow.showAsDropDown(anchorView, bodyOffsetX, bodyOffsetY);
		
		if (mPopupWindow.isAboveAnchor()) {
			mPopupWindow.setAnimationStyle(R.style.QuickContactAboveAnimation);
			showArrow(R.id.arrow_down, anchor.centerX(), arrowOffsetX);
		}
		else {
			mPopupWindow.setAnimationStyle(R.style.QuickContactBelowAnimation);
			showArrow(R.id.arrow_up, anchor.centerX(), arrowOffsetX);
		}
	}

	private void buildActionButtons() {}

	private void showArrow(int whichArrow, int requestedX, int offsetX) {

		final View showArrow = (whichArrow == R.id.arrow_up) ? mArrowUp : mArrowDown;
		final View hideArrow = (whichArrow == R.id.arrow_up) ? mArrowDown : mArrowUp;
		int arrowWidth = mArrowUp.getMeasuredWidth();
		arrowWidth = 32;
		int popupWidth = mPopupWindow.getWidth();

		showArrow.setVisibility(View.VISIBLE);

		RelativeLayout.MarginLayoutParams marginParams = (RelativeLayout.MarginLayoutParams) showArrow.getLayoutParams();
		marginParams.rightMargin = 10 + (offsetX * -1);
		marginParams.leftMargin = ((requestedX - (mScreenWidth - mPopupWindow.getWidth())) - arrowWidth / 2) + offsetX; 
		showArrow.setLayoutParams(marginParams);

		hideArrow.setVisibility(View.INVISIBLE);
	}

	public synchronized void dismiss() {
		setShowing(false);
		mPopupWindow.dismiss();
	}

	private void doBackPressed() {
		dismiss();
	}

	private void resetTrack() {
		mTrackSet.removeAllViews();
		mTrackScroll.fullScroll(View.FOCUS_LEFT);
	}

	public boolean isShowing() {
		return this.mActionStripShowing;
	}

	public void setShowing(boolean showing) {
		this.mActionStripShowing = showing;
		this.mActionStripToggleTime = System.currentTimeMillis();
	}

	public long getActionStripToggleTime() {
		return mActionStripToggleTime;
	}

	/**
	 * Interface used to allow the person showing a {@link ActionsWindow} to
	 * know when the window has been dismissed.
	 */
	public interface OnDismissListener {

		public void onDismiss(ActionsWindow dialog);
	}

	/**
	 * Custom layout the sole purpose of which is to intercept the BACK key and
	 * close QC even when the soft keyboard is open.
	 */
	public static class RootLayout extends RelativeLayout {

		ActionsWindow	mQuickContactWindow;

		public RootLayout(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		/**
		 * Intercepts the BACK key event and dismisses QuickContact window.
		 */
		@Override
		public boolean dispatchKeyEventPreIme(KeyEvent event) {
			if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
				mQuickContactWindow.doBackPressed();
				return true;
			}
			else {
				return super.dispatchKeyEventPreIme(event);
			}
		}
	}

}
