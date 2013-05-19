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

import com.aircandi.beta.R;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.objects.Source;
import com.aircandi.ui.widgets.WebImageView;

public class SourceListAdapter extends ArrayAdapter<Source> implements Filterable {

	private final LayoutInflater	mInflater;
	private Integer					mItemLayoutId	= R.layout.temp_listitem_sources_builder;
	private final List<Source>		mListItems;

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
		final Source itemData = mListItems.get(position);

		if (view == null) {
			view = mInflater.inflate(mItemLayoutId, null);
			holder = new ViewHolder();
			holder.image = (WebImageView) view.findViewById(R.id.image);
			holder.label = (TextView) view.findViewById(R.id.source_label);
			holder.id = (TextView) view.findViewById(R.id.source_id);
			holder.url = (TextView) view.findViewById(R.id.source_url);
			holder.check = (CheckBox) view.findViewById(R.id.check);
			if (holder.check != null) {
				holder.check.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View view) {
						final CheckBox checkBox = (CheckBox) view;
						final Source source = (Source) checkBox.getTag();
						source.checked = checkBox.isChecked();
					}
				});
			}
			view.setTag(holder);

			FontManager.getInstance().setTypefaceDefault(holder.label);
			FontManager.getInstance().setTypefaceDefault(holder.id);
			FontManager.getInstance().setTypefaceDefault(holder.url);
		}
		else {
			holder = (ViewHolder) view.getTag();
		}

		if (itemData != null) {
			final Source source = itemData;

			setVisibility(holder.check, View.GONE);
			if (holder.check != null && source.checked != null) {
				holder.check.setChecked(source.checked);
				holder.check.setTag(source);
				setVisibility(holder.check, View.VISIBLE);
			}

			setVisibility(holder.label, View.GONE);
			if (holder.label != null && source.label != null && source.label.length() > 0) {
				holder.label.setText(source.label);
				setVisibility(holder.label, View.VISIBLE);
			}

			setVisibility(holder.id, View.GONE);
			if (holder.id != null) {
				if (source.id != null && source.id.length() > 0) {
					holder.id.setText(source.id);
					setVisibility(holder.id, View.VISIBLE);
				}
			}

			setVisibility(holder.url, View.GONE);
			if (holder.url != null) {
				if (source.url != null && source.url.length() > 0) {
					holder.url.setText(source.url);
					setVisibility(holder.url, View.VISIBLE);
				}
			}
			
			if (holder.image != null) {
				holder.image.setTag(source);
				/*
				 * The WebImageView sets the current bitmap ref being held
				 * by the internal image view to null before doing the work
				 * to satisfy the new request.
				 */
				final String imageUri = source.getPhoto().getUri();

				/* Don't do anything if the image is already set to the one we want */
				if (holder.image.getImageUri() == null || !holder.image.getImageUri().equals(imageUri)) {

					final BitmapRequestBuilder builder = new BitmapRequestBuilder(holder.image).setImageUri(imageUri);
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
		private TextView		label;
		private TextView		id;
		private TextView		url;
		private CheckBox		check;
	}
}
