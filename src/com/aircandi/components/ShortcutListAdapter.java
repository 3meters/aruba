package com.aircandi.components;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.TextView;

import com.aircandi.beta.R;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.utilities.UI;

public class ShortcutListAdapter extends ArrayAdapter<Shortcut> implements Filterable {

	private final LayoutInflater	mInflater;
	private Integer					mItemLayoutId	= R.layout.temp_listitem_applink_edit;
	private final List<Shortcut>	mListItems;

	public ShortcutListAdapter(Context context, List<Shortcut> shortcuts, Integer itemLayoutId) {
		super(context, 0, shortcuts);

		mListItems = shortcuts;
		mInflater = LayoutInflater.from(context);

		if (itemLayoutId != null) {
			mItemLayoutId = itemLayoutId;
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		final ViewHolder holder;
		final Shortcut itemData = mListItems.get(position);

		if (view == null) {
			view = mInflater.inflate(mItemLayoutId, null);
			holder = new ViewHolder();
			holder.photoView = (AirImageView) view.findViewById(R.id.photo);
			holder.name = (TextView) view.findViewById(R.id.name);
			view.setTag(holder);
		}
		else {
			holder = (ViewHolder) view.getTag();
		}

		if (itemData != null) {
			final Shortcut shortcut = itemData;

			setVisibility(holder.name, View.GONE);
			if (holder.name != null && shortcut.name != null && shortcut.name.length() > 0) {
				holder.name.setText(shortcut.name);
				setVisibility(holder.name, View.VISIBLE);
			}

			if (holder.photoView != null) {
				holder.photoView.setTag(shortcut);
				UI.drawPhoto(holder.photoView, shortcut.getPhoto());
			}
		}
		return view;
	}

	@Override
	public Shortcut getItem(int position) {
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
		private AirImageView	photoView;
		private TextView		name;
	}
}
