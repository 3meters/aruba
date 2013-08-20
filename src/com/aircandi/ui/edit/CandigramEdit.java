package com.aircandi.ui.edit;

import java.util.List;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.applications.Candigrams;
import com.aircandi.applications.Candigrams.PropertyType;
import com.aircandi.components.FontManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.SpinnerData;
import com.aircandi.components.TabManager;
import com.aircandi.service.objects.Candigram;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Dialogs;

public class CandigramEdit extends BaseEntityEdit {

	private TabManager	mTabManager;
	private ViewFlipper	mViewFlipper;

	private Integer		mSpinnerItem;
	private Spinner		mSpinnerType;
	private Spinner		mSpinnerRange;
	private Spinner		mSpinnerDuration;

	private CheckBox	mCheckBoxCapture;

	private TextView	mHintType;
	private TextView	mHintRange;
	private TextView	mHintDuration;
	private TextView	mHintCapture;

	private SpinnerData	mSpinnerTypeData;
	private SpinnerData	mSpinnerRangeData;
	private SpinnerData	mSpinnerDurationData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mCheckBoxCapture = (CheckBox) findViewById(R.id.chk_capture);
		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper_form);
		mSpinnerItem = getThemeTone().equals("dark") ? R.layout.spinner_item_dark : R.layout.spinner_item_light;

		mSpinnerTypeData = Candigrams.getSpinnerData(this, PropertyType.type);
		mSpinnerRangeData = Candigrams.getSpinnerData(this, PropertyType.range);
		mSpinnerDurationData = Candigrams.getSpinnerData(this, PropertyType.duration);

		mHintType = (TextView) findViewById(R.id.hint_type);
		mHintRange = (TextView) findViewById(R.id.hint_range);
		mHintDuration = (TextView) findViewById(R.id.hint_duration);
		mHintCapture = (TextView) findViewById(R.id.hint_capture);

		mSpinnerType = (Spinner) findViewById(mEditing ? R.id.spinner_type : R.id.wizard_spinner_type);
		mSpinnerRange = (Spinner) findViewById(R.id.spinner_range);
		mSpinnerDuration = (Spinner) findViewById(R.id.spinner_duration);

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

		mCheckBoxCapture.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Candigram candigram = (Candigram) mEntity;
				mHintCapture.setText(getString(isChecked ? R.string.candigram_capture_true_hint : R.string.candigram_capture_false_hint));

				if (candigram.capture == null || ((Candigram) mEntity).capture != isChecked) {
					if (!mFirstDraw) {
						mDirty = true;
					}
				}
			}
		});

		if (mEditing) {
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
	protected void draw() {
		super.draw();

		/* Place content */
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

		if (mCheckBoxCapture != null) {
			mCheckBoxCapture.setChecked(candigram.capture != null ? candigram.capture : false);
			mHintCapture.setText(getString(mCheckBoxCapture.isChecked() ? R.string.candigram_capture_true_hint : R.string.candigram_capture_false_hint));
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
					Animate.doOverridePendingTransition(this, TransitionType.CandigramOut);
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

		if (mSpinnerDuration != null && mSpinnerDuration.getSelectedItemPosition() < mSpinnerDurationData.getEntryValues().size()) {
			candigram.duration = Integer.parseInt(mSpinnerDurationData.getEntryValues().get(mSpinnerDuration.getSelectedItemPosition()));
		}

		if (findViewById(R.id.chk_capture) != null) {
			candigram.capture = ((CheckBox) findViewById(R.id.chk_capture)).isChecked();
		}

		if (candigram.type.equals(Constants.TYPE_APP_TOUR)) {
			/* Updates do not change the current hop settings */
			if (!mEditing && candigram.duration != null) {
				candigram.hopLastDate = DateTime.nowDate().getTime();
				candigram.hopNextDate = candigram.hopLastDate.longValue() + candigram.duration.longValue();
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