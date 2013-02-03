package com.aircandi.ui.builders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.CandiConstants;
import com.aircandi.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.FontManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.components.SourceListAdapter;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Source;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.ui.widgets.BounceListView;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.ImageUtils;

public class SourcesBuilder extends FormActivity {

	private BounceListView		mList;
	private TextView			mMessage;
	private List<Source>		mSources	= new ArrayList<Source>();
	private Entity				mEntity;
	private Source				mSourceEditing;
	private ArrayList<String>	mJsonSourcesOriginal;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		if (!isFinishing()) {
			initialize();
			if (mEntity != null
					&& mEntity.sourceSuggestions == null
					&& mEntity.sources != null
					&& mEntity.sources.size() > 0) {
				loadSourceSuggestions(mEntity, mEntity.sources, false);
			}
			else {
				bind();
				mCommon.hideBusy(true);
			}
		}
	}

	private void initialize() {
		/* We use this to access the source suggestions */
		if (mCommon.mEntityId != null) {
			mEntity = ProxiExplorer.getInstance().getEntityModel().getCacheEntity(mCommon.mEntityId);
		}

		Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			ArrayList<String> jsonSources = extras.getStringArrayList(CandiConstants.EXTRA_SOURCES);
			if (jsonSources != null) {
				mJsonSourcesOriginal = jsonSources;
				for (String jsonSource : jsonSources) {
					Source source = (Source) ProxibaseService.convertJsonToObjectInternalSmart(jsonSource, ServiceDataType.Source);
					mSources.add(source);
				}
			}
		}

		mMessage = (TextView) findViewById(R.id.message);
		mList = (BounceListView) findViewById(R.id.list);
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_cancel));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_save));
	}

	private void bind() {

		if (mSources.size() == 0) {
			mCommon.hideBusy(true);
			mMessage.setText(R.string.sources_builder_empty);
			mMessage.setVisibility(View.VISIBLE);
		}
		else {
			mMessage.setVisibility(View.GONE);
			Integer position = 0;
			for (Source source : mSources) {
				source.checked = false;
				source.position = position;
				position++;
			}
		}
		SourceListAdapter adapter = new SourceListAdapter(this, mSources, R.layout.temp_listitem_sources_builder);
		mList.setAdapter(adapter);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {
		if (isDirty()) {
			confirmDirtyExit();
		}
		else {
			setResult(Activity.RESULT_CANCELED);
			finish();
			AnimUtils.doOverridePendingTransition(SourcesBuilder.this, TransitionType.FormToPage);
		}
	}

	@SuppressWarnings("ucd")
	public void onSaveButtonClick(View view) {
		gatherAndExit();
	}

	@Override
	public void onCancelButtonClick(View view) {
		if (isDirty()) {
			confirmDirtyExit();
		}
		else {
			setResult(Activity.RESULT_CANCELED);
			finish();
			AnimUtils.doOverridePendingTransition(SourcesBuilder.this, TransitionType.FormToPage);
		}
	}

	@SuppressWarnings("ucd")
	public void onDeleteButtonClick(View view) {
		for (int i = mSources.size() - 1; i >= 0; i--) {
			if (mSources.get(i).checked) {
				confirmSourceDelete();
				return;
			}
		}
	}

	@SuppressWarnings("ucd")
	public void onAddButtonClick(View view) {
		Intent intent = new Intent(this, SourceBuilder.class);
		if (mEntity != null) {
			intent.putExtra(CandiConstants.EXTRA_ENTITY_ID, mEntity.id);
		}
		startActivityForResult(intent, CandiConstants.ACTIVITY_SOURCE_NEW);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@SuppressWarnings("ucd")
	public void onMoveUpButtonClick(View view) {
		for (int i = mSources.size() - 1; i >= 0; i--) {
			if (mSources.get(i).checked) {
				mSources.get(i).position -= 2;
			}
		}
		Collections.sort(mSources, new Source.SortSourcesBySourcePosition());
		Integer position = 0;
		for (Source source : mSources) {
			source.position = position;
			position++;
		}
		mList.invalidateViews();
	}

	@SuppressWarnings("ucd")
	public void onMoveDownButtonClick(View view) {
		for (int i = mSources.size() - 1; i >= 0; i--) {
			if (mSources.get(i).checked) {
				mSources.get(i).position += 2;
			}
		}
		Collections.sort(mSources, new Source.SortSourcesBySourcePosition());
		Integer position = 0;
		for (Source source : mSources) {
			source.position = position;
			position++;
		}
		mList.invalidateViews();
	}

	@SuppressWarnings("ucd")
	public void onListItemClick(View view) {
		Intent intent = new Intent(this, SourceBuilder.class);
		CheckBox check = (CheckBox) view.findViewById(R.id.check);
		mSourceEditing = (Source) check.getTag();
		String jsonSource = ProxibaseService.convertObjectToJsonSmart(mSourceEditing, false, true);
		intent.putExtra(CandiConstants.EXTRA_SOURCE, jsonSource);
		startActivityForResult(intent, CandiConstants.ACTIVITY_SOURCE_EDIT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == CandiConstants.ACTIVITY_SOURCE_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					Bundle extras = intent.getExtras();
					String jsonSource = extras.getString(CandiConstants.EXTRA_SOURCE);
					if (jsonSource != null) {
						Source sourceUpdated = (Source) ProxibaseService.convertJsonToObjectInternalSmart(jsonSource, ServiceDataType.Source);
						if (sourceUpdated != null) {
							/* Copy changes */
							mSourceEditing.name = sourceUpdated.name;
							mSourceEditing.id = sourceUpdated.id;
							mList.invalidateViews();
						}
					}
				}
			}
			else if (requestCode == CandiConstants.ACTIVITY_SOURCE_NEW) {
				if (intent != null && intent.getExtras() != null) {
					Bundle extras = intent.getExtras();
					String jsonSource = extras.getString(CandiConstants.EXTRA_SOURCE);
					if (jsonSource != null) {
						Source sourceNew = (Source) ProxibaseService.convertJsonToObjectInternalSmart(jsonSource, ServiceDataType.Source);
						if (sourceNew != null) {
							sourceNew.checked = false;
							mSources.add(sourceNew);

							/* Remove as a suggestion candidate */
							if (mEntity != null && mEntity.sourceSuggestions != null) {
								for (int i = mEntity.sourceSuggestions.size() - 1; i >= 0; i--) {
									if (mEntity.sourceSuggestions.get(i).source.equals(sourceNew.source)) {
										mEntity.sourceSuggestions.remove(i);
									}
								}
							}

							/* Rebuild the position numbering */
							Integer position = 0;
							for (Source source : mSources) {
								source.position = position;
								position++;
							}

							mList.invalidateViews();
							scrollToBottom();
						}
					}
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	private void confirmSourceDelete() {

		/* How many are we deleting? */
		Integer deleteCount = 0;
		for (int i = mSources.size() - 1; i >= 0; i--) {
			if (mSources.get(i).checked) {
				deleteCount++;
			}
		}

		final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ViewGroup customView = (ViewGroup) inflater.inflate(R.layout.temp_delete_sources, null);
		TextView message = (TextView) customView.findViewById(R.id.message);
		LinearLayout list = (LinearLayout) customView.findViewById(R.id.list);
		for (Source source : mSources) {
			if (source.checked) {
				View sourceView = inflater.inflate(R.layout.temp_listitem_delete_sources, null);
				WebImageView image = (WebImageView) sourceView.findViewById(R.id.image);
				TextView title = (TextView) sourceView.findViewById(R.id.title);
				if (source.name != null) {
					title.setText(source.name);
				}
				image.setTag(source);
				final String imageUri = source.getImageUri();
				BitmapRequest bitmapRequest = new BitmapRequest(imageUri, image.getImageView());
				bitmapRequest.setImageSize(image.getSizeHint());
				bitmapRequest.setImageRequestor(image.getImageView());

				BitmapManager.getInstance().masterFetch(bitmapRequest);
				list.addView(sourceView);
			}
		}

		message.setText(deleteCount == 1
				? R.string.alert_source_delete_message_single
				: R.string.alert_source_delete_message_multiple);

		AlertDialog dialog = AircandiCommon.showAlertDialog(null
				, getResources().getString(R.string.alert_source_delete_title)
				, null
				, customView
				, this
				, android.R.string.ok
				, android.R.string.cancel
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							for (int i = mSources.size() - 1; i >= 0; i--) {
								if (mSources.get(i).checked) {
									Source source = mSources.remove(i);
									if (source.custom == null || !source.custom) {
										mEntity.sourceSuggestions.add(0, source);
									}
								}
							}
							mList.invalidateViews();
						}

					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	private void confirmDirtyExit() {
		AlertDialog dialog = AircandiCommon.showAlertDialog(null
				, getResources().getString(R.string.alert_sources_dirty_exit_title)
				, getResources().getString(R.string.alert_sources_dirty_exit_message)
				, null
				, this
				, android.R.string.ok
				, android.R.string.cancel
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							setResult(Activity.RESULT_CANCELED);
							finish();
							AnimUtils.doOverridePendingTransition(SourcesBuilder.this, TransitionType.FormToPage);
						}
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	private void loadSourceSuggestions(final Entity entity, final List<Source> sources, final Boolean autoInsert) {
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("LoadSourceSuggestions");
				ModelResult result = ProxiExplorer.getInstance().getEntityModel().getSourceSuggestions(sources);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					if (entity != null) {
						List<Source> sourceSuggestions = (List<Source>) result.serviceResponse.data;
						entity.sourceSuggestions = sourceSuggestions;
						if (entity.sourceSuggestions == null) {
							entity.sourceSuggestions = new ArrayList<Source>();
						}
						if (autoInsert) {
							if (sourceSuggestions.size() > 0) {
								mSources.addAll(sourceSuggestions);
								ImageUtils.showToastNotification(getResources().getString(sourceSuggestions.size() == 1
										? R.string.toast_source_linked
										: R.string.toast_sources_linked), Toast.LENGTH_SHORT);
							}

						}
					}
					else {
						List<Source> sourceSuggestions = (List<Source>) result.serviceResponse.data;
						mSources.addAll(sourceSuggestions);
					}
				}
				bind();
				mCommon.hideBusy(true);
			}
		}.execute();
	}

	private void gatherAndExit() {
		Intent intent = new Intent();
		List<String> sourceStrings = new ArrayList<String>();
		for (Source source : mSources) {
			sourceStrings.add(ProxibaseService.convertObjectToJsonSmart(source, true, true));
		}
		intent.putStringArrayListExtra(CandiConstants.EXTRA_SOURCES, (ArrayList<String>) sourceStrings);
		setResult(Activity.RESULT_OK, intent);
		finish();
		AnimUtils.doOverridePendingTransition(this, TransitionType.FormToPage);
	}

	@Override
	protected Boolean isDialog() {
		return false;
	}

	private Boolean isDirty() {

		/* Gather */
		List<String> jsonSources = new ArrayList<String>();
		for (Source source : mSources) {
			jsonSources.add(ProxibaseService.convertObjectToJsonSmart(source, true, true));
		}
		if (mJsonSourcesOriginal == null) {
			if (jsonSources.size() > 0) {
				return true;
			}
		}
		else {

			if (jsonSources.size() != mJsonSourcesOriginal.size()) {
				return true;
			}

			int position = 0;
			for (String jsonSource : jsonSources) {
				if (!jsonSource.toString().equals(mJsonSourcesOriginal.get(position).toString())) {
					return true;
				}
				position++;
			}
		}
		return false;
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	private void scrollToBottom() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mList.setSelection(mList.getAdapter().getCount() - 1);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mCommon.doCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		mCommon.doPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case R.id.suggest_links:
				/* Go get source suggestions again */
				if (mSources.size() > 0) {
					loadSourceSuggestions(mEntity, mSources, true);
				}
				return true;
		}
		/* In case we add general menu items later */
		mCommon.doOptionsItemSelected(item);
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.builder_sources;
	}
}