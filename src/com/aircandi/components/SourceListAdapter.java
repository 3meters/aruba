package com.aircandi.components;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Filterable;
import android.widget.TextView;

import com.aircandi.R;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.objects.Source;
import com.aircandi.ui.widgets.WebImageView;

public class SourceListAdapter extends ArrayAdapter<Source> implements Filterable {

	private LayoutInflater	mInflater;
	private Integer			mItemLayoutId	= R.layout.temp_listitem_sources_builder;
	private List<Source>	mListItems;

	public SourceListAdapter(Context context, List<Source> sources, Integer itemLayoutId) {
		super(context, 0, sources);
		
		mListItems = sources;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		if (itemLayoutId != null) {
			mItemLayoutId = itemLayoutId;
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		final ViewHolder holder;
		Source itemData = (Source) mListItems.get(position);

		if (view == null) {
			view = mInflater.inflate(mItemLayoutId, null);
			holder = new ViewHolder();
			holder.image = (WebImageView) view.findViewById(R.id.image);
			holder.title = (TextView) view.findViewById(R.id.title);
			holder.subtitle = (TextView) view.findViewById(R.id.subtitle);
			holder.check = (CheckBox) view.findViewById(R.id.check);
			if (holder.check != null) {
				holder.check.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View view) {
						CheckBox checkBox = (CheckBox) view;
						Source source = (Source) checkBox.getTag();
						source.checked = checkBox.isChecked();
					}
				});
			}
			view.setTag(holder);

			FontManager.getInstance().setTypefaceDefault(holder.title);
			FontManager.getInstance().setTypefaceDefault(holder.subtitle);
		}
		else {
			holder = (ViewHolder) view.getTag();
		}

		if (itemData != null) {
			Source source = itemData;

			setVisibility(holder.check, View.GONE);
			if (holder.check != null && source.checked != null) {
				holder.check.setChecked(source.checked);
				holder.check.setTag(source);
				setVisibility(holder.check, View.VISIBLE);
			}

			setVisibility(holder.title, View.GONE);
			if (holder.title != null && source.name != null && source.name.length() > 0) {
				holder.title.setText(source.name);
				setVisibility(holder.title, View.VISIBLE);
			}

			setVisibility(holder.subtitle, View.GONE);
			if (holder.subtitle != null && source.id != null && source.id.length() > 0) {
				holder.subtitle.setText(source.id);
				setVisibility(holder.subtitle, View.VISIBLE);
			}
			
			if (holder.image != null) {
				holder.image.setTag(source);
				holder.image.getImageBadge().setVisibility(View.GONE);
				/*
				 * The WebImageView sets the current bitmap ref being held
				 * by the internal image view to null before doing the work
				 * to satisfy the new request.
				 */
				final String imageUri = source.getImageUri();

				/* Don't do anything if the image is already set to the one we want */
				if (holder.image.getImageUri() == null || !holder.image.getImageUri().equals(imageUri)) {

					BitmapRequestBuilder builder = new BitmapRequestBuilder(holder.image).setImageUri(imageUri);
					final BitmapRequest imageRequest = builder.create();
					holder.image.setBitmapRequest(imageRequest);
				}
			}
		}
		return view;
	}

	@Override
	public Source getItem(int position) {
		return mListItems.get(position);
	}

	@Override
	public int getCount() {
		return mListItems.size();
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		return true;
	}

	private static void setVisibility(View view, Integer visibility) {
		if (view != null) {
			view.setVisibility(visibility);
		}
	}

	private static class ViewHolder {
		private WebImageView	image;
		private TextView		title;
		private TextView		subtitle;
		private CheckBox		check;
	}
}
