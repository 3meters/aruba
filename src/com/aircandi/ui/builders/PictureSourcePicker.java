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

import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.EntityManager;
import com.aircandi.components.FontManager;
import com.aircandi.components.Template;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.base.FormActivity;

public class PictureSourcePicker extends FormActivity implements OnItemClickListener {

	private ListView	mListView;
	private ListAdapter	mListAdapter;
	private TextView	mTitle;
	private String		mEntityId;
	private String		mEntityType;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			initialize();
		}
	}

	private void initialize() {

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mEntityType = extras.getString(Constants.EXTRA_ENTITY_TYPE);
		}

		/* Shown as a dialog so doesn't have an action bar */
		final List<Object> listData = new ArrayList<Object>();

		if (mEntityType != null) {
			if (mEntityType.equals(Constants.TYPE_APPLINK_FACEBOOK)) {
				listData.add(new Template(mCommon.mThemeTone.equals("light") ? R.drawable.ic_action_facebook_light : R.drawable.ic_action_facebook_dark
						, getString(R.string.dialog_picture_source_facebook), null, "facebook"));
			}
			else if (mEntityType.equals(Constants.TYPE_APPLINK_TWITTER)) {
				listData.add(new Template(mCommon.mThemeTone.equals("light") ? R.drawable.ic_action_twitter_light : R.drawable.ic_action_twitter_dark
						, getString(R.string.dialog_picture_source_twitter), null, "twitter"));
			}
			listData.add(new Template(mCommon.mThemeTone.equals("light") ? R.drawable.ic_action_picture_light : R.drawable.ic_action_picture_dark
					, getString(R.string.dialog_picture_source_default), null, "default"));
		}
		else {

			listData.add(new Template(mCommon.mThemeTone.equals("light") ? R.drawable.ic_action_search_light : R.drawable.ic_action_search_dark
					, getString(R.string.dialog_picture_source_search), null, "search"));

			listData.add(new Template(mCommon.mThemeTone.equals("light") ? R.drawable.ic_action_tiles_large_light : R.drawable.ic_action_tiles_large_dark
					, getString(R.string.dialog_picture_source_gallery), null, "gallery"));

			/* Only show the camera choice if there is one and there is a place to store the image */
			if (AndroidManager.isIntentAvailable(this, MediaStore.ACTION_IMAGE_CAPTURE)) {
				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					listData.add(new Template(mCommon.mThemeTone.equals("light") ? R.drawable.ic_action_camera_light : R.drawable.ic_action_camera_dark
							, getString(R.string.dialog_picture_source_camera), null, "camera"));
				}
			}

			/* Add place photo option if this is a place entity */
			if (mEntityId != null) {
				final Entity entity = EntityManager.getInstance().getEntity(mEntityId);
				if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					Place place = (Place) entity;
					if (place.getProvider().type != null && place.getProvider().type.equals("foursquare")) {
						listData.add(new Template(mCommon.mThemeTone.equals("light") ? R.drawable.ic_action_location_light : R.drawable.ic_action_location_dark
								, getString(R.string.dialog_picture_source_place), null, "place"));
					}
					else {
						List<Entity> entities = (List<Entity>) entity.getLinkedEntitiesByLinkType(Constants.TYPE_LINK_POST, null, Direction.in, false);
						for (Entity post : entities) {
							if (post.photo != null) {
								listData.add(new Template(mCommon.mThemeTone.equals("light") ? R.drawable.ic_action_location_light
										: R.drawable.ic_action_location_dark
										, getString(R.string.dialog_picture_source_place), null, "place"));
								break;
							}
						}
					}
				}
			}

			listData.add(new Template(mCommon.mThemeTone.equals("light") ? R.drawable.ic_action_picture_light : R.drawable.ic_action_picture_dark
					, getString(R.string.dialog_picture_source_default), null, "default"));
		}

		mTitle = (TextView) findViewById(R.id.title);
		mTitle.setText(R.string.dialog_picture_source_title);

		mListAdapter = new ListAdapter(this, listData);
		mListView = (ListView) findViewById(R.id.form_list);
		mListView.setAdapter(mListAdapter);
		mListView.setOnItemClickListener(this);

		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.title));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_cancel));
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final Template template = (Template) view.getTag();
		final Intent intent = new Intent();
		intent.putExtra(Constants.EXTRA_PICTURE_SOURCE, template.type);
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
				final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
	protected int getLayoutId() {
		return R.layout.picker_picture_source;
	}
}