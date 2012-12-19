package com.aircandi.ui.builders;

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

import com.aircandi.CandiConstants;
import com.aircandi.R;
import com.aircandi.components.Template;
import com.aircandi.ui.base.FormActivity;

public class TemplatePicker extends FormActivity implements OnItemClickListener {

	private ListView	mListView;
	private ListAdapter	mListAdapter;
	private Boolean		mIsRoot;
	private TextView	mTextViewMessage;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mIsRoot = extras.getBoolean(CandiConstants.EXTRA_ENTITY_IS_ROOT);
		}

		initialize();
	}

	private void initialize() {
		
		/* Shown as a dialog so doesn't have an action bar */
		List<Object> listData = new ArrayList<Object>();
		if (mCommon.mThemeTone.equals("dark")) {
			listData.add(new Template(R.drawable.ic_action_edit_dark, getString(R.string.name_entity_type_post), null, CandiConstants.TYPE_CANDI_POST));
			listData.add(new Template(R.drawable.ic_action_picture_dark, getString(R.string.name_entity_type_picture), null, CandiConstants.TYPE_CANDI_PICTURE));
			if (mIsRoot != null && mIsRoot) {
				listData.add(new Template(R.drawable.ic_action_folder_dark, getString(R.string.name_entity_type_folder), null,
						CandiConstants.TYPE_CANDI_FOLDER));
			}
		}
		else {
			listData.add(new Template(R.drawable.ic_action_edit_light, getString(R.string.name_entity_type_post), null, CandiConstants.TYPE_CANDI_POST));
			listData.add(new Template(R.drawable.ic_action_picture_light, getString(R.string.name_entity_type_picture), null, CandiConstants.TYPE_CANDI_PICTURE));
			if (mIsRoot != null && mIsRoot) {
				listData.add(new Template(R.drawable.ic_action_folder_light, getString(R.string.name_entity_type_folder), null,
						CandiConstants.TYPE_CANDI_FOLDER));
			}
		}

		mListAdapter = new ListAdapter(this, listData);
		mListView = (ListView) findViewById(R.id.form_list);
		mTextViewMessage = (TextView) findViewById(R.id.text_message);
		if (!mIsRoot) {
			mTextViewMessage.setText(R.string.dialog_template_picker_message_folder);
		}
		mListView.setAdapter(mListAdapter);
		mListView.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Template template = (Template) view.getTag();
		Intent intent = new Intent();
		intent.putExtra(CandiConstants.EXTRA_ENTITY_TYPE, template.type);
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	protected Boolean isDialog() {
		return true;
	}
	
	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected int getLayoutID() {
		return R.layout.template_picker;
	}

	private class ListAdapter extends ArrayAdapter<Object>
	{
		private List<Object>	items;

		public ListAdapter(Context context, List<Object> items) {
			super(context, 0, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			Template itemData = (Template) items.get(position);

			if (view == null)
			{
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.temp_listitem_templates, null);
			}

			if (itemData != null)
			{
				((ImageView) view.findViewById(R.id.item_image)).setImageResource(itemData.iconResId);
				((TextView) view.findViewById(R.id.item_title)).setText(itemData.title);
				view.setTag(itemData);
			}
			return view;
		}
	}
}