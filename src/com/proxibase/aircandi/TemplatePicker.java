package com.proxibase.aircandi;

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

import com.proxibase.aircandi.components.Template;
import com.proxibase.aircandi.core.CandiConstants;

public class TemplatePicker extends FormActivity implements OnItemClickListener {

	private ListView	mListView;
	private ListAdapter	mListAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
	}

	private void initialize() {
		mCommon.track();

		/* Shown as a dialog so doesn't have an action bar */
		
		List<Object> listData = new ArrayList<Object>();
		listData.add(new Template(R.drawable.icon_post, "Post", null, CandiConstants.TYPE_CANDI_POST));
		listData.add(new Template(R.drawable.icon_picture, "Picture", null, CandiConstants.TYPE_CANDI_PICTURE));
		listData.add(new Template(R.drawable.icon_link, "Link", null, CandiConstants.TYPE_CANDI_LINK));

		mListAdapter = new ListAdapter(this, listData);
		mListView = (ListView) findViewById(R.id.form_list);
		mListView.setAdapter(mListAdapter);
		mListView.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Template template = (Template) view.getTag();
		Intent intent = new Intent();
		intent.putExtra(getString(R.string.EXTRA_ENTITY_TYPE), template.type);
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
	protected Integer getCustomTheme() {
		return mCommon.mThemeId;
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
				view = inflater.inflate(R.layout.temp_listitem_template, null);
			}

			if (itemData != null)
			{
				((ImageView) view.findViewById(R.id.item_image)).setImageResource(itemData.iconResId);
				((TextView) view.findViewById(R.id.item_title)).setText(itemData.title);
				// ((TextView) view.findViewById(R.id.item_description)).setText(itemData.description);
				view.setTag(itemData);
			}
			return view;
		}
	}
}