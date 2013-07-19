package com.aircandi.ui.helpers;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import com.aircandi.beta.R;
import com.aircandi.components.Template;
import com.aircandi.ui.base.BaseBrowse;

public class TemplatePicker extends BaseBrowse implements OnItemClickListener {

	private TextView	mName;
	private ListView	mListView;
	private ListAdapter	mListAdapter;

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mName = (TextView) findViewById(R.id.name);
		mListView = (ListView) findViewById(R.id.form_list);
	}

	@Override
	protected void databind(Boolean refresh) {
		mName.setText(R.string.dialog_template_picker_title);

		/* Shown as a dialog so doesn't have an action bar */
		final List<Object> listData = new ArrayList<Object>();
		if (mThemeTone.equals("dark")) {
			listData.add(new Template(R.drawable.ic_action_picture_dark, getString(R.string.name_entity_type_picture), null, Constants.SCHEMA_ENTITY_POST));
		}
		else {
			listData.add(new Template(R.drawable.ic_action_picture_light, getString(R.string.name_entity_type_picture), null, Constants.SCHEMA_ENTITY_POST));
		}

		mListAdapter = new ListAdapter(this, listData);
		mListView.setAdapter(mListAdapter);
		mListView.setOnItemClickListener(this);
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final Template template = (Template) view.getTag();
		final Intent intent = new Intent();
		intent.putExtra(Constants.EXTRA_ENTITY_SCHEMA, template.type);
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
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
			final Template itemData = (Template) items.get(position);

			if (view == null) {
				view = LayoutInflater.from(TemplatePicker.this).inflate(R.layout.temp_listitem_templates, null);
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
		return R.layout.picker_template;
	}
}