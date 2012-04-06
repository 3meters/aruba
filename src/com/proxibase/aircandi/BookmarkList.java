package com.proxibase.aircandi;

import com.proxibase.aircandi.components.BookmarkAdapter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class BookmarkList extends FormActivity implements OnItemClickListener {

	private ListView		mListView;
	private BookmarkAdapter	mBookmarkAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
	}

	private void initialize() {
		mCommon.track();

		mBookmarkAdapter = new BookmarkAdapter(this, "");
		mListView = (ListView) findViewById(R.id.list_bookmarks);
		mListView.setAdapter(mBookmarkAdapter);
		mListView.setOnItemClickListener(this);
		mListView.setDivider(null);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		String bookmarkUri = mBookmarkAdapter.getUrl(position);
		String bookmarkTitle = mBookmarkAdapter.getTitle(position);
		Intent intent = new Intent();
		intent.putExtra(getString(R.string.EXTRA_URI), bookmarkUri);
		intent.putExtra(getString(R.string.EXTRA_URI_TITLE), bookmarkTitle);
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

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected int getLayoutID() {
		return R.layout.bookmarks;
	}

}