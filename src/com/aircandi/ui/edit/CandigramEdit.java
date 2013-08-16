package com.aircandi.ui.edit;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.FontManager;
import com.aircandi.components.TabManager;
import com.aircandi.ui.base.BaseEntityEdit;

public class CandigramEdit extends BaseEntityEdit {

	private TabManager	mTabManager;
	private Spinner		mSpinnerType;
	private Spinner		mSpinnerRange;
	private Spinner		mSpinnerDuration;
	private Integer			mSpinnerItem;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		if (mEntity != null) {
			if (mEntity.ownerId != null && (mEntity.ownerId.equals(Aircandi.getInstance().getUser().id))) {
				mTabManager = new TabManager(Constants.TABS_ENTITY_FORM_ID, mActionBar, (ViewFlipper) findViewById(R.id.flipper_form));
				mTabManager.initialize();
				mTabManager.doRestoreInstanceState(savedInstanceState);
			}
		}

		mSpinnerType = (Spinner) findViewById(R.id.spinner_type);
		mSpinnerRange = (Spinner) findViewById(R.id.spinner_range);
		mSpinnerDuration = (Spinner) findViewById(R.id.spinner_duration);
		mSpinnerItem = getThemeTone().equals("dark") ? R.layout.spinner_item_dark : R.layout.spinner_item_light;

		mSpinnerType.setAdapter(new ArrayAdapter(this, mSpinnerItem, getResources().getStringArray(R.array.candigram_type_entries)) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = super.getView(position, convertView, parent);
				FontManager.getInstance().setTypefaceDefault((TextView) view);
				return view;
			}

			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent) {
				View view = super.getDropDownView(position, convertView, parent);
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
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

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