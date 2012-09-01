/*
 * Copyright (C) 2008 The Android Open Source Project Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.aircandi.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aircandi.R;

/**
 * Custom layout for an item representing a bookmark in the browser.
 */
class BookmarkItem extends RelativeLayout {

	private TextView		mTextTitle;
	private TextView		mTextUri;
	private ImageView		mImageFavIcon;
	private LayoutInflater	mFactory;

	BookmarkItem(Context context) {
		super(context);

		mFactory = LayoutInflater.from(context);
		mFactory.inflate(R.layout.temp_listitem_bookmark, this);
		mTextTitle = (TextView) findViewById(R.id.item_title);
		mTextUri = (TextView) findViewById(R.id.item_uri);
		mImageFavIcon = (ImageView) findViewById(R.id.item_favicon);
		mTextTitle.setMaxLines(1);
	}

	void copyTo(BookmarkItem item) {
		item.mTextTitle.setText(mTextTitle.getText());
		item.mTextUri.setText(mTextUri.getText());
		item.mImageFavIcon.setImageDrawable(mImageFavIcon.getDrawable());
	}

	CharSequence getName() {
		return mTextTitle.getText();
	}

	TextView getNameTextView() {
		return mTextTitle;
	}

	void setFavicon(Bitmap bitmap) {
		if (bitmap != null) {
			mImageFavIcon.setImageBitmap(bitmap);
		}
		else {
			mImageFavIcon.setImageResource(R.drawable.app_web_browser_sm);
		}
	}

	void setName(String name) {
		mTextTitle.setText(name);
	}

	void setUrl(String url) {
		mTextUri.setText(url);
	}
}