package com.aircandi.ui.helpers;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.AirApplication;
import com.aircandi.ui.base.BasePicker;

public class ApplicationPicker extends BasePicker implements OnItemClickListener {

	private TextView	mName;
	private ListView	mListView;
	private ListAdapter	mListAdapter;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mName = (TextView) findViewById(R.id.name);
		mListView = (ListView) findViewById(R.id.form_list);
		mListView.setOnItemClickListener(this);
	}

	@Override
	public void bind(BindingMode mode) {
		String schema = mEntity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE) ? Constants.SCHEMA_REMAP_PICTURE : mEntity.schema;
		String title = !TextUtils.isEmpty(mEntity.name) ? mEntity.name : schema;
		mName.setText(title);

		/* Shown as a dialog so doesn't have an action bar */
		final List<Object> listData = new ArrayList<Object>();

		if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			listData.add(new AirApplication(getThemeTone().equals("light") ? R.drawable.ic_logo_holo_light : R.drawable.ic_logo_holo_dark
					, getString(R.string.dialog_application_candigram_new), null, Constants.SCHEMA_ENTITY_CANDIGRAM));

			listData.add(new AirApplication(getThemeTone().equals("light") ? R.drawable.ic_action_picture_light : R.drawable.ic_action_picture_dark
					, getString(R.string.dialog_application_picture_new), null, Constants.SCHEMA_ENTITY_PICTURE));

			listData.add(new AirApplication(getThemeTone().equals("light") ? R.drawable.ic_action_monolog_light : R.drawable.ic_action_monolog_dark
					, getString(R.string.dialog_application_comment_new), null, Constants.SCHEMA_ENTITY_COMMENT));
		}
		else if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
			listData.add(new AirApplication(getThemeTone().equals("light") ? R.drawable.ic_action_picture_light : R.drawable.ic_action_picture_dark
					, getString(R.string.dialog_application_picture_new), null, Constants.SCHEMA_ENTITY_PICTURE));

			listData.add(new AirApplication(getThemeTone().equals("light") ? R.drawable.ic_action_monolog_light : R.drawable.ic_action_monolog_dark
					, getString(R.string.dialog_application_comment_new), null, Constants.SCHEMA_ENTITY_COMMENT));
		}
		else if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
			listData.add(new AirApplication(getThemeTone().equals("light") ? R.drawable.ic_action_monolog_light : R.drawable.ic_action_monolog_dark
					, getString(R.string.dialog_application_comment_new), null, Constants.SCHEMA_ENTITY_COMMENT));
		}

		mListAdapter = new ListAdapter(this, listData);
		mListView.setAdapter(mListAdapter);
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final AirApplication choice = (AirApplication) view.getTag();
		final Intent intent = new Intent();
		intent.putExtra(Constants.EXTRA_ENTITY_SCHEMA, choice.schema);
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	private class ListAdapter extends ArrayAdapter<Object>
	{
		private final List<Object>	items;

		private ListAdapter(Context context, List<Object> items) {
			super(context, 0, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			final AirApplication itemData = (AirApplication) items.get(position);

			if (view == null) {
				view = LayoutInflater.from(ApplicationPicker.this).inflate(R.layout.temp_listitem_applications, null);
			}

			if (itemData != null) {
				((ImageView) view.findViewById(R.id.photo)).setImageResource(itemData.iconResId);
				((TextView) view.findViewById(R.id.name)).setText(itemData.title);
				view.setTag(itemData);
			}
			return view;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected Boolean isDialog() {
		return true;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.application_picker;
	}
}