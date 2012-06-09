package com.proxibase.aircandi.components;

/*
 * Copyright (C) 2010 Tani Group
 * http://android-demo.blogspot.com/
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.proxibase.aircandi.R;

public class IconContextMenu implements DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {

	private static final int				LIST_PREFERED_HEIGHT	= 70;

	private IconMenuAdapter					mMenuAdapter			= null;
	private Activity						mParentActivity			= null;
	private int								mDialogId				= 0;
	private IconContextMenuOnClickListener	mClickHandler			= null;

	public IconContextMenu(Activity parent, int id) {
		mParentActivity = parent;
		mDialogId = id;
		mMenuAdapter = new IconMenuAdapter(mParentActivity);
	}

	public void addItem(Resources res, CharSequence title, int imageResourceId, int actionTag) {
		mMenuAdapter.addItem(new IconContextMenuItem(res, title, imageResourceId, actionTag));
	}

	public void addItem(Resources res, int textResourceId, int imageResourceId, int actionTag) {
		mMenuAdapter.addItem(new IconContextMenuItem(res, textResourceId, imageResourceId, actionTag));
	}

	public void setOnClickListener(IconContextMenuOnClickListener listener) {
		mClickHandler = listener;
	}

	public Dialog createMenu(String titleText, Context context) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(mParentActivity);
		View titleView = ((Activity) context).getLayoutInflater().inflate(R.layout.temp_dialog_title, null);
		((TextView) titleView.findViewById(R.id.dialog_title_text)).setText(titleText);
		builder.setCustomTitle(titleView);

		builder.setAdapter(mMenuAdapter, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialoginterface, int i) {
				IconContextMenuItem item = (IconContextMenuItem) mMenuAdapter.getItem(i);

				if (mClickHandler != null) {
					mClickHandler.onClick(item.actionTag);
				}
			}
		});

		builder.setInverseBackgroundForced(false);

		AlertDialog dialog = builder.create();
		dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

		dialog.setOnCancelListener(this);
		dialog.setOnDismissListener(this);
		return dialog;
	}

	public void onCancel(DialogInterface dialog) {
		cleanup();
	}

	public void onDismiss(DialogInterface dialog) {}

	private void cleanup() {
		mParentActivity.dismissDialog(mDialogId);
	}

	public interface IconContextMenuOnClickListener {

		public abstract void onClick(int menuId);
	}

	protected class IconMenuAdapter extends BaseAdapter {

		private Context							context	= null;

		private ArrayList<IconContextMenuItem>	mItems	= new ArrayList<IconContextMenuItem>();

		public IconMenuAdapter(Context context) {
			this.context = context;
		}

		public void addItem(IconContextMenuItem menuItem) {
			mItems.add(menuItem);
		}

		@Override
		public int getCount() {
			return mItems.size();
		}

		@Override
		public Object getItem(int position) {
			return mItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			IconContextMenuItem item = (IconContextMenuItem) getItem(position);
			return item.actionTag;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			IconContextMenuItem item = (IconContextMenuItem) getItem(position);

			Resources res = mParentActivity.getResources();

			if (convertView == null) {

				TextView textView = new TextView(context);
				AbsListView.LayoutParams param = new AbsListView.LayoutParams(
						AbsListView.LayoutParams.FILL_PARENT,
						AbsListView.LayoutParams.WRAP_CONTENT);
				textView.setLayoutParams(param);
				textView.setPadding((int) toPixel(res, 15), (int) toPixel(res, 5), (int) toPixel(res, 15), (int) toPixel(res, 5));
				textView.setGravity(android.view.Gravity.CENTER_VERTICAL);
				textView.setTextColor(Color.BLACK);

				// Theme th = context.getTheme();
				// TypedValue tv = new TypedValue();
				// if (th.resolveAttribute(android.R.attr.te.textAppearanceLargeInverse, tv, true)) {
				// textView.setTextAppearance(context, tv.resourceId);
				// }

				textView.setTextSize(16);
				textView.setMinHeight(LIST_PREFERED_HEIGHT);
				textView.setMaxHeight(LIST_PREFERED_HEIGHT);
				textView.setCompoundDrawablePadding((int) toPixel(res, 14));

				BitmapDrawable bitmapDrawable = (BitmapDrawable) item.image;
				Bitmap bitmap = Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), (int) toPixel(res, 60), (int) toPixel(res, 55), false);
				item.image = new BitmapDrawable(bitmap);

				convertView = textView;
			}

			TextView textView = (TextView) convertView;
			textView.setTag(item);
			textView.setText(item.text);
			textView.setCompoundDrawablesWithIntrinsicBounds(item.image, null, null, null);

			return textView;
		}

		private float toPixel(Resources res, int dip) {
			float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, res.getDisplayMetrics());
			return px;
		}
	}

	protected class IconContextMenuItem {

		public final CharSequence	text;
		public Drawable				image;
		public final int			actionTag;

		public IconContextMenuItem(Resources res, int textResourceId, int imageResourceId, int actionTag) {
			text = res.getString(textResourceId);
			if (imageResourceId != -1) {
				image = res.getDrawable(imageResourceId);
			}
			else {
				image = null;
			}
			this.actionTag = actionTag;
		}

		public IconContextMenuItem(Resources res, CharSequence title, int imageResourceId, int actionTag) {
			text = title;
			if (imageResourceId != -1) {
				image = res.getDrawable(imageResourceId);
			}
			else {
				image = null;
			}
			this.actionTag = actionTag;
		}
	}
}