package com.aircandi.ui.edit;

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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.applications.Candigrams;
import com.aircandi.applications.Candigrams.PropertyType;
import com.aircandi.components.FontManager;
import com.aircandi.components.SpinnerData;
import com.aircandi.components.TabManager;
import com.aircandi.service.objects.Candigram;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.utilities.Dialogs;

public class CandigramEdit extends BaseEntityEdit {

	private TabManager	mTabManager;

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
		if (!mEditing || (mEntity.ownerId != null && (mEntity.ownerId.equals(Aircandi.getInstance().getUser().id)))) {
			mTabManager = new TabManager(Constants.TABS_ENTITY_FORM_ID, mActionBar, (ViewFlipper) findViewById(R.id.flipper_form));
			mTabManager.initialize();
			mTabManager.doRestoreInstanceState(savedInstanceState);
		}

		mSpinnerTypeData = Candigrams.getSpinnerData(this, PropertyType.type);
		mSpinnerRangeData = Candigrams.getSpinnerData(this, PropertyType.range);
		mSpinnerDurationData = Candigrams.getSpinnerData(this, PropertyType.duration);

		mHintType = (TextView) findViewById(R.id.hint_type);
		mHintRange = (TextView) findViewById(R.id.hint_range);
		mHintDuration = (TextView) findViewById(R.id.hint_duration);
		mHintCapture = (TextView) findViewById(R.id.hint_capture);

		mSpinnerType = (Spinner) findViewById(R.id.spinner_type);
		mSpinnerRange = (Spinner) findViewById(R.id.spinner_range);
		mSpinnerDuration = (Spinner) findViewById(R.id.spinner_duration);

		mCheckBoxCapture = (CheckBox) findViewById(R.id.chk_capture);

		mSpinnerItem = getThemeTone().equals("dark") ? R.layout.spinner_item_dark : R.layout.spinner_item_light;

		mSpinnerType.setAdapter(new ArrayAdapter(this, mSpinnerItem, getResources().getStringArray(R.array.candigram_type_entries)) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = super.getView(position, convertView, parent);
				FontManager.getInstance().setTypefaceDefault((TextView) view);
				return view;
			}

		});

		mSpinnerRange.setAdapter(new ArrayAdapter(this, mSpinnerItem, getResources().getStringArray(R.array.candigram_range_entries)) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = super.getView(position, convertView, parent);
				FontManager.getInstance().setTypefaceDefault((TextView) view);
				return view;
			}
		});

		mSpinnerDuration.setAdapter(new ArrayAdapter(this, mSpinnerItem, getResources().getStringArray(
				R.array.candigram_duration_entries)) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = super.getView(position, convertView, parent);
				FontManager.getInstance().setTypefaceDefault((TextView) view);
				return view;
			}
		});

		mSpinnerType.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				Candigram candigram = (Candigram) mEntity;
				mHintType.setText(mSpinnerTypeData.getDescriptions().get(position));
				if (candigram.type == null || !candigram.type.equals(mSpinnerTypeData.getEntryValues().get(position))) {
					mDirty = true;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		mSpinnerRange.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				Candigram candigram = (Candigram) mEntity;
				mHintRange.setText(mSpinnerRangeData.getDescriptions().get(position));
				if (candigram.range == null
						|| candigram.range.intValue() != Integer.parseInt(mSpinnerRangeData.getEntryValues().get(position))) {
					mDirty = true;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		mSpinnerDuration.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				Candigram candigram = (Candigram) mEntity;
				mHintDuration.setText(mSpinnerDurationData.getDescriptions().get(position));
				if (candigram.duration == null
						|| candigram.duration.intValue() != Integer.parseInt(mSpinnerDurationData.getEntryValues().get(position))) {
					mDirty = true;
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
					mDirty = true;
				}
			}
		});

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
				mSpinnerType.setSelection(Candigrams.TYPE_DEFAULT_POSITION);
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
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	protected boolean validate() {
		if (!super.validate()) {
			return false;
		}
		if (mEntity.type == null) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_candigram_type)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		return true;
	}

	@Override
	protected void gather() {
		super.gather();

		Candigram candigram = (Candigram) mEntity;

		if (mSpinnerType != null) {
			candigram.type = (String) mSpinnerTypeData.getEntryValues().get(mSpinnerType.getSelectedItemPosition());
		}

		if (mSpinnerRange != null) {
			candigram.range = Integer.parseInt(mSpinnerRangeData.getEntryValues().get(mSpinnerRange.getSelectedItemPosition()));
		}

		if (mSpinnerDuration != null) {
			candigram.duration = Integer.parseInt(mSpinnerDurationData.getEntryValues().get(mSpinnerDuration.getSelectedItemPosition()));
		}

		if (findViewById(R.id.chk_capture) != null) {
			candigram.capture = ((CheckBox) findViewById(R.id.chk_capture)).isChecked();
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
}