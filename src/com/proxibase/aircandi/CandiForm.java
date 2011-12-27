package com.proxibase.aircandi;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.proxibase.aircandi.utils.CandiPagerAdapter;
import com.proxibase.aircandi.utils.CandiListAdapter.CandiListViewHolder;
import com.proxibase.aircandi.widgets.ViewPagerIndicator;
import com.proxibase.sdk.android.proxi.consumer.Command;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;

public class CandiForm extends AircandiActivity {

	private ViewFlipper			mCandiFlipper;
	private ViewPager			mCandiPager;
	private ViewPagerIndicator	mCandiPagerIndicator;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		initialize();
		bind();
		draw();
		GoogleAnalyticsTracker.getInstance().trackPageView("/CandiForm");
	}

	protected void initialize() {}

	protected void bind() {
		mCandiFlipper = (ViewFlipper) findViewById(R.id.flipper_candi);

		mCandiPager = (ViewPager) findViewById(R.id.pager);
		mCandiPager.setOnPageChangeListener(mCandiPagerIndicator);

		mCandiPagerIndicator = (ViewPagerIndicator) findViewById(R.id.pager_indicator);
		mCandiPagerIndicator.bindToView((View) getLayoutInflater().inflate(R.layout.temp_page_indicator, null));
		mCandiPager.setOnPageChangeListener(mCandiPagerIndicator);

		mCandiPager.setAdapter(new CandiPagerAdapter(this, mCandiPager, mCandiPagerIndicator));
		((CandiPagerAdapter) mCandiPager.getAdapter()).setEntity(mEntityProxy);
		((CandiPagerAdapter) mCandiPager.getAdapter()).setUser(mUser);
		
		mCandiPager.setCurrentItem(0);
		mCandiPager.getAdapter().notifyDataSetChanged();
	}

	protected void draw() {}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onCandiInfoClick(View v) {
	/* Do nothing */
	}

	public void onListItemClick(View view) {
		CandiListViewHolder holder = (CandiListViewHolder) view.getTag();
		showCandiInfoChild((EntityProxy) holder.data);
	}
	
	public void onCommentsClick(View view) {
		EntityProxy entity = (EntityProxy) view.getTag();
		if (entity.commentCount > 0) {
			Intent intent = Aircandi.buildIntent(this, entity, 0, false, new Command("view"), null, mUser, CommentList.class);
			startActivity(intent);
		}
	}


	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void showCandiInfoChild(EntityProxy entityProxy) {
		ViewGroup candiInfoView = (ViewGroup) getLayoutInflater().inflate(R.layout.temp_candi_info, null);
		LinearLayout candiInfoChildView = (LinearLayout) findViewById(R.id.view_candi_info_child);
		candiInfoView = ((CandiPagerAdapter) mCandiPager.getAdapter()).buildCandiInfo(entityProxy, candiInfoView);
		candiInfoChildView.removeAllViews();
		candiInfoChildView.addView(candiInfoView);
		mCandiFlipper.clearAnimation();
		mCandiFlipper.setInAnimation(this, R.anim.slide_in_right);
		mCandiFlipper.setOutAnimation(this, R.anim.slide_out_left);
		mCandiFlipper.showNext();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected int getLayoutID() {
		return R.layout.candi_form;
	}
}