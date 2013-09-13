package com.aircandi.ui.edit;

import java.util.List;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.applications.Candigrams;
import com.aircandi.applications.Candigrams.PropertyType;
import com.aircandi.components.EntityManager;
import com.aircandi.components.FontManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.LocationManager;
import com.aircandi.components.SpinnerData;
import com.aircandi.components.TabManager;
import com.aircandi.service.objects.AirLocation;
import com.aircandi.service.objects.Candigram;
import com.aircandi.service.objects.Entity;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.Dialogs;

public class CandigramEdit extends BaseEntityEdit {

	private TabManager	mTabManager;
	private ViewFlipper	mViewFlipper;

	private Integer		mSpinnerItem;
	private Spinner		mSpinnerType;
	private Spinner		mSpinnerRange;
	private Spinner		mSpinnerDuration;
	private Spinner		mSpinnerHops;

	private TextView	mHintType;
	private TextView	mHintRange;
	private TextView	mHintDuration;
	private TextView	mHintHops;

	private SpinnerData	mSpinnerTypeData;
	private SpinnerData	mSpinnerRangeData;
	private SpinnerData	mSpinnerDurationData;
	private SpinnerData	mSpinnerHopsData;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper_form);
		mSpinnerItem = getThemeTone().equals("dark") ? R.layout.spinner_item_dark : R.layout.spinner_item_light;

		mSpinnerTypeData = Candigrams.getSpinnerData(this, PropertyType.TYPE);
		mSpinnerRangeData = Candigrams.getSpinnerData(this, PropertyType.RANGE);
		mSpinnerDurationData = Candigrams.getSpinnerData(this, PropertyType.DURATION);
		mSpinnerHopsData = Candigrams.getSpinnerData(this, PropertyType.HOPS);

		mHintType = (TextView) findViewById(R.id.hint_type);
		mHintRange = (TextView) findViewById(R.id.hint_range);
		mHintDuration = (TextView) findViewById(R.id.hint_duration);
		mHintHops = (TextView) findViewById(R.id.hint_hops);

		mSpinnerType = (Spinner) findViewById(mEditing ? R.id.spinner_type : R.id.wizard_spinner_type);
		mSpinnerRange = (Spinner) findViewById(R.id.spinner_range);
		mSpinnerDuration = (Spinner) findViewById(R.id.spinner_duration);
		mSpinnerHops = (Spinner) findViewById(R.id.spinner_hops);

		mSpinnerType.setAdapter(new SpinnerAdapter(this, mSpinnerItem, mSpinnerTypeData.getEntries(), R.string.candigram_type_hint));
		mSpinnerType.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				if (position < mSpinnerType.getAdapter().getCount()) {
					Candigram candigram = (Candigram) mEntity;
					String type = mSpinnerTypeData.getEntryValues().get(position);

					if (!mEditing) {
						if (type.equals(Constants.TYPE_APP_TOUR)) {
							findViewById(R.id.help_touring).setVisibility(View.VISIBLE);
							findViewById(R.id.help_bouncing).setVisibility(View.GONE);
						}
						else {
							findViewById(R.id.help_touring).setVisibility(View.GONE);
							findViewById(R.id.help_bouncing).setVisibility(View.VISIBLE);
						}
						findViewById(R.id.type_image_next).setVisibility(View.VISIBLE);
					}
					else {
						mHintType.setText(mSpinnerTypeData.getDescriptions().get(position));
						mHintType.setVisibility(View.VISIBLE);
					}

					findViewById(R.id.duration_holder).setVisibility(type.equals(Constants.TYPE_APP_TOUR) ? View.VISIBLE : View.GONE);

					if (candigram.type == null || !candigram.type.equals(type)) {
						if (!mFirstDraw) {
							mDirty = true;
						}
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		mSpinnerRange.setAdapter(new SpinnerAdapter(this, mSpinnerItem, mSpinnerRangeData.getEntries(), R.string.candigram_range_hint));
		mSpinnerRange.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				if (position < mSpinnerRange.getAdapter().getCount()) {
					Candigram candigram = (Candigram) mEntity;
					mHintRange.setText(mSpinnerRangeData.getDescriptions().get(position));
					mHintRange.setVisibility(View.VISIBLE);

					if (candigram.range == null
							|| candigram.range.intValue() != Integer.parseInt(mSpinnerRangeData.getEntryValues().get(position))) {
						if (!mFirstDraw) {
							mDirty = true;
						}
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		mSpinnerDuration.setAdapter(new SpinnerAdapter(this, mSpinnerItem, mSpinnerDurationData.getEntries(), R.string.candigram_duration_hint));
		mSpinnerDuration.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				if (position < mSpinnerDuration.getAdapter().getCount()) {
					Candigram candigram = (Candigram) mEntity;
					mHintDuration.setText(mSpinnerDurationData.getDescriptions().get(position));
					mHintDuration.setVisibility(View.VISIBLE);
					if (candigram.duration == null
							|| candigram.duration.intValue() != Integer.parseInt(mSpinnerDurationData.getEntryValues().get(position))) {
						if (!mFirstDraw) {
							mDirty = true;
						}
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		mSpinnerHops.setAdapter(new SpinnerAdapter(this, mSpinnerItem, mSpinnerHopsData.getEntries(), R.string.candigram_hops_hint));
		mSpinnerHops.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				if (position < mSpinnerHops.getAdapter().getCount()) {
					Candigram candigram = (Candigram) mEntity;
					mHintHops.setText(mSpinnerHopsData.getDescriptions().get(position));
					mHintHops.setVisibility(View.VISIBLE);
					if (candigram.hopsMax == null
							|| candigram.hopsMax.intValue() != Integer.parseInt(mSpinnerHopsData.getEntryValues().get(position))) {
						if (!mFirstDraw) {
							mDirty = true;
						}
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		if (mEditing) {
			/*
			 * If editing, mEntity is already initialized from intent extras.
			 */
			if (mEntity.ownerId != null && (mEntity.ownerId.equals(Aircandi.getInstance().getUser().id))) {
				mTabManager = new TabManager(Constants.TABS_ENTITY_FORM_ID, mActionBar, mViewFlipper);
				mTabManager.initialize();
				mTabManager.doRestoreInstanceState(savedInstanceState);
			}
			((ViewFlipper) findViewById(R.id.flipper_form)).removeViewAt(0);
			findViewById(R.id.content_message).setVisibility(View.GONE);
			findViewById(R.id.settings_message).setVisibility(View.GONE);
		}
		else {
			findViewById(R.id.type_holder).setVisibility(View.GONE);
			findViewById(R.id.authoring_holder).setVisibility(View.GONE);
			mViewFlipper.setDisplayedChild(0);
			Integer colorResId = R.color.accent_gray_dark;
			((ImageView) findViewById(R.id.type_image_next)).setColorFilter(mResources.getColor(colorResId), PorterDuff.Mode.SRC_ATOP);
			((ImageView) findViewById(R.id.content_image_next)).setColorFilter(mResources.getColor(colorResId), PorterDuff.Mode.SRC_ATOP);
			((ImageView) findViewById(R.id.content_image_previous)).setColorFilter(mResources.getColor(colorResId), PorterDuff.Mode.SRC_ATOP);
			((ImageView) findViewById(R.id.settings_image_previous)).setColorFilter(mResources.getColor(colorResId), PorterDuff.Mode.SRC_ATOP);
			findViewById(R.id.content_image_previous).setVisibility(View.VISIBLE);
			findViewById(R.id.content_image_next).setVisibility(View.VISIBLE);
			findViewById(R.id.settings_button_finish).setVisibility(View.VISIBLE);
			findViewById(R.id.settings_image_previous).setVisibility(View.VISIBLE);
		}

	}

	@Override
	protected void configureActionBar() {
		super.configureActionBar();
		/*
		 * Navigation setup for action bar icon and title
		 */
		Drawable icon = getResources().getDrawable(R.drawable.img_candigram_temp);
		icon.setColorFilter(Aircandi.getInstance().getResources().getColor(Candigrams.ICON_COLOR), PorterDuff.Mode.SRC_ATOP);
		mActionBar.setIcon(icon);
	}

	@Override
	public void draw() {
		super.draw();

		/* PLACE content */
		Candigram candigram = (Candigram) mEntity;

		if (mSpinnerType != null) {
			if (candigram.type != null) {
				int i = 0;
				for (String type : mSpinnerTypeData.getEntryValues()) {
					if (type.equals(candigram.type)) {
						mSpinnerType.setSelection(i);
					}
					i++;
				}
			}
			else {
				/* Show last item which is the hint */
				mSpinnerType.setSelection(mSpinnerType.getAdapter().getCount());
			}
		}

		if (mSpinnerRange != null) {
			if (candigram.range != null) {
				int i = 0;
				for (String range : mSpinnerRangeData.getEntryValues()) {
					if (Integer.parseInt(range) == candigram.range.intValue()) {
						mSpinnerRange.setSelection(i);
					}
					i++;
				}
			}
			else {
				mSpinnerRange.setSelection(Candigrams.RANGE_DEFAULT_POSITION);
			}
		}
		if (mSpinnerDuration != null) {
			if (candigram.duration != null) {
				int i = 0;
				for (String duration : mSpinnerDurationData.getEntryValues()) {
					if (Integer.parseInt(duration) == candigram.duration.intValue()) {
						mSpinnerDuration.setSelection(i);
					}
					i++;
				}
			}
			else {
				mSpinnerDuration.setSelection(Candigrams.DURATION_DEFAULT_POSITION);
			}
		}
		if (mSpinnerHops != null) {
			if (candigram.hopsMax != null) {
				int i = 0;
				for (String hops : mSpinnerHopsData.getEntryValues()) {
					if (Integer.parseInt(hops) == candigram.hopsMax.intValue()) {
						mSpinnerHops.setSelection(i);
					}
					i++;
				}
			}
			else {
				mSpinnerHops.setSelection(Candigrams.HOPS_DEFAULT_POSITION);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onNextButtonClick(View view) {
		if (mViewFlipper.getDisplayedChild() == 1) {
			/*
			 * Make sure there is some content
			 */
			if (mEntity.photo == null) {
				Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
						, null
						, getResources().getString(R.string.error_missing_candigram_photo)
						, null
						, this
						, android.R.string.ok
						, null, null, null, null);
				return;
			}
		}

		if (mViewFlipper.getDisplayedChild() < 2) {
			mViewFlipper.setInAnimation(this, R.anim.slide_in_right);
			mViewFlipper.setOutAnimation(this, R.anim.slide_out_left);
			mViewFlipper.setDisplayedChild(mViewFlipper.getDisplayedChild() + 1);
		}
	}

	@SuppressWarnings("ucd")
	public void onPreviousButtonClick(View view) {
		if (mViewFlipper.getDisplayedChild() > 0) {
			mViewFlipper.setInAnimation(this, R.anim.slide_in_left);
			mViewFlipper.setOutAnimation(this, R.anim.slide_out_right);
			mViewFlipper.setDisplayedChild(mViewFlipper.getDisplayedChild() - 1);
		}
	}

	@SuppressWarnings("ucd")
	public void onFinishButtonClick(View view) {
		if (isDirty()) {
			gather();
			if (validate()) {

				if (mSkipSave) {
					final IntentBuilder intentBuilder = new IntentBuilder().setEntity(mEntity);
					setResult(Constants.RESULT_ENTITY_EDITED, intentBuilder.create());
					finish();
					Animate.doOverridePendingTransition(this, TransitionType.CANDIGRAM_OUT);
				}
				else {
					if (mEditing) {
						update();
					}
					else {
						insert();
					}
				}
			}
		}
		else {
			onCancel(false);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	protected boolean validate() {
		if (!super.validate()) {
			return false;
		}
		Candigram candigram = (Candigram) mEntity;
		if (candigram.type == null) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_candigram_type)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		if (candigram.range == null) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_candigram_range)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		if (candigram.hopsMax == null) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_candigram_hops)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		if (candigram.type != null && candigram.type.equals(Constants.TYPE_APP_TOUR)) {
			if (candigram.duration == null) {
				Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
						, null
						, getResources().getString(R.string.error_missing_candigram_duration)
						, null
						, this
						, android.R.string.ok
						, null, null, null, null);
				return false;
			}
		}

		return true;
	}

	@Override
	protected void gather() {
		super.gather();

		Candigram candigram = (Candigram) mEntity;

		if (mSpinnerType != null && mSpinnerType.getSelectedItemPosition() < mSpinnerTypeData.getEntryValues().size()) {
			candigram.type = (String) mSpinnerTypeData.getEntryValues().get(mSpinnerType.getSelectedItemPosition());
		}

		if (mSpinnerRange != null && mSpinnerRange.getSelectedItemPosition() < mSpinnerRangeData.getEntryValues().size()) {
			candigram.range = Integer.parseInt(mSpinnerRangeData.getEntryValues().get(mSpinnerRange.getSelectedItemPosition()));
		}

		if (mSpinnerHops != null && mSpinnerHops.getSelectedItemPosition() < mSpinnerHopsData.getEntryValues().size()) {
			candigram.hopsMax = Integer.parseInt(mSpinnerHopsData.getEntryValues().get(mSpinnerHops.getSelectedItemPosition()));
		}

		if (mSpinnerDuration != null && mSpinnerDuration.getSelectedItemPosition() < mSpinnerDurationData.getEntryValues().size()) {
			candigram.duration = null;
			if (candigram.type.equals("tour")) {
				candigram.duration = Integer.parseInt(mSpinnerDurationData.getEntryValues().get(mSpinnerDuration.getSelectedItemPosition()));
			}
		}

		/*
		 * Set origin location so SERVICE can determine eligible area for HOPS
		 */
		if (mParentId != null) {
			Entity place = EntityManager.getEntity(mParentId);
			if (place != null && place.location != null) {
				candigram.location = new AirLocation();
				candigram.location.lat = place.location.lat;
				candigram.location.lng = place.location.lng;
			}
		}

		if (candigram.location == null) {
			AirLocation location = LocationManager.getInstance().getAirLocationLocked();
			if (location != null) {
				candigram.location = new AirLocation();
				candigram.location.lat = location.lat;
				candigram.location.lng = location.lng;
			}
		}
	}

	@Override
	protected String getLinkType() {
		return Constants.TYPE_LINK_CANDIGRAM;
	};

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.candigram_edit;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	private class SpinnerAdapter extends ArrayAdapter {

		private final List<String>	mItems;

		private SpinnerAdapter(Context context, int mSpinnerItemResId, List items, Integer spinnerHint) {
			super(context, mSpinnerItemResId, items);

			items.add(getString(spinnerHint));
			mItems = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			final View view = super.getView(position, convertView, parent);
			FontManager.getInstance().setTypefaceDefault((TextView) view);

			final TextView text = (TextView) view.findViewById(R.id.spinner_name);
			if (getThemeTone().equals("dark")) {
				if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
					text.setTextColor(Aircandi.getInstance().getResources().getColor(R.color.text_dark));
				}
			}

			if (position == getCount()) {
				text.setText("");
				text.setHint(mItems.get(getCount())); //"Hint to be displayed"
			}

			return view;
		}

		@Override
		public int getCount() {
			return super.getCount() - 1; // you dont display last item. It is used as hint.
		}
	}

}