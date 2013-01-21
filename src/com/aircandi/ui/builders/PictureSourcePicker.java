package com.aircandi.ui.builders;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Window;
import com.aircandi.CandiConstants;
import com.aircandi.R;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.FontManager;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.Template;
import com.aircandi.service.objects.Entity;
import com.aircandi.ui.base.FormActivity;

public class PictureSourcePicker extends FormActivity implements OnItemClickListener {

	private ListView	mListView;
	private ListAdapter	mListAdapter;
	private TextView	mTitle;
	private String		mEntityId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		super.onCreate(savedInstanceState);

		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
		initialize();
	}

	private void initialize() {
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mEntityId = extras.getString(CandiConstants.EXTRA_ENTITY_ID);
		}

		/* Shown as a dialog so doesn't have an action bar */
		List<Object> listData = new ArrayList<Object>();
		Integer iconResId = R.drawable.ic_action_picture_dark;
		if (mCommon.mThemeTone.equals("light")) {
			iconResId = R.drawable.ic_action_picture_light;
		}
		
		listData.add(new Template(iconResId, getString(R.string.dialog_picture_source_search), null, "search"));
		listData.add(new Template(iconResId, getString(R.string.dialog_picture_source_gallery), null, "gallery"));

		/* Only show the camera choice if there is one and there is a place to store the image */
		if (AndroidManager.isIntentAvailable(this, MediaStore.ACTION_IMAGE_CAPTURE)) {
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				listData.add(new Template(iconResId, getString(R.string.dialog_picture_source_camera), null, "camera"));
			}
		}

		/* Add place photo option if this is a place entity */
		if (mEntityId != null) {
			Entity entity = ProxiExplorer.getInstance().getEntityModel().getCacheEntity(mEntityId);
			if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE) && entity.place.source != null && entity.place.source.equals("foursquare")) {
				listData.add(new Template(iconResId, getString(R.string.dialog_picture_source_place), null, "place"));
			}
		}
		
		listData.add(new Template(iconResId, getString(R.string.dialog_picture_source_default), null, "default"));

		mTitle = (TextView) findViewById(R.id.custom_title);
		mTitle.setText(R.string.dialog_picture_source_title);

		mListAdapter = new ListAdapter(this, listData);
		mListView = (ListView) findViewById(R.id.form_list);
		mListView.setAdapter(mListAdapter);
		mListView.setOnItemClickListener(this);

		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.custom_title));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_cancel));
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Template template = (Template) view.getTag();
		Intent intent = new Intent();
		intent.putExtra(CandiConstants.EXTRA_PICTURE_SOURCE, template.type);
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

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

			if (view == null) {
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.temp_listitem_picture_source, null);
			}

			if (itemData != null) {
				((ImageView) view.findViewById(R.id.item_image)).setImageResource(itemData.iconResId);
				((TextView) view.findViewById(R.id.item_title)).setText(itemData.title);
				FontManager.getInstance().setTypefaceDefault((TextView) view.findViewById(R.id.item_title));
				view.setTag(itemData);
			}
			return view;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected Boolean isDialog() {
		return true;
	}

	@Override
	protected int getLayoutID() {
		return R.layout.picker_picture_source;
	}
}