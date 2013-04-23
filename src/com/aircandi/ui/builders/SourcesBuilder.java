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

import com.actionbarsherlock.view.MenuItem;
import com.aircandi.CandiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.FontManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiManager;
import com.aircandi.components.ProxiManager.ModelResult;
import com.aircandi.components.SourceListAdapter;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ServiceDataType;
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
	private final List<Source>	mSystemSources	= new ArrayList<Source>();
	private final List<Source>	mActiveSources	= new ArrayList<Source>();
	private String				mEntityId;
	private Entity				mEntity;
	private Source				mSourceEditing;
	private List<String>		mJsonSourcesOriginal;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (!isFinishing()) {
			initialize();
			bind();
			mCommon.hideBusy(true); // Visible by default
		}
	}

	private void initialize() {

		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
		mCommon.mActionBar.setTitle(R.string.form_title_links);

		/* We use this to access the source suggestions */
		final Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			final List<String> jsonSources = extras.getStringArrayList(CandiConstants.EXTRA_SOURCES);
			if (jsonSources != null) {
				mJsonSourcesOriginal = jsonSources;
				List<Source> sources = new ArrayList<Source>();
				for (String jsonSource : jsonSources) {
					Source source = (Source) HttpService.convertJsonToObjectInternalSmart(jsonSource, ServiceDataType.Source);
					sources.add(source);
				}
				splitSources(sources);
			}
			mEntityId = extras.getString(CandiConstants.EXTRA_ENTITY_ID);
			if (mEntityId != null) {
				mEntity = ProxiManager.getInstance().getEntityModel().getCacheEntity(mEntityId);
			}
		}

		mMessage = (TextView) findViewById(R.id.message);
		mList = (BounceListView) findViewById(R.id.list);

		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.message));
	}

	private void bind() {

		if (mActiveSources.size() == 0) {
			mCommon.hideBusy(true); // visible by default
			mMessage.setText(R.string.sources_builder_empty);
			mMessage.setVisibility(View.VISIBLE);
		}
		else {
			mMessage.setVisibility(View.GONE);
			Integer position = 0;
			for (Source source : mActiveSources) {
				source.checked = false;
				source.position = position;
				position++;
			}
		}
		final SourceListAdapter adapter = new SourceListAdapter(this, mActiveSources, R.layout.temp_listitem_sources_builder);
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

	public void onCheckedClick(View view) {
		CheckBox check = (CheckBox) view.findViewById(R.id.check);
		check.setChecked(!check.isChecked());
		final Source source = (Source) check.getTag();
		source.checked = check.isChecked();
	}

	@SuppressWarnings("ucd")
	public void onSuggestLinksButtonClick(View view) {
		/* Go get source suggestions again */
		List<Source> sources = mergeSources();
		loadSourceSuggestions(sources, true, mEntity);
	}

	@SuppressWarnings("ucd")
	public void onDeleteButtonClick(View view) {
		for (int i = mActiveSources.size() - 1; i >= 0; i--) {
			if (mActiveSources.get(i).checked) {
				confirmSourceDelete();
				return;
			}
		}
	}

	@SuppressWarnings("ucd")
	public void onAddButtonClick(View view) {
		final Intent intent = new Intent(this, SourceBuilder.class);
		startActivityForResult(intent, CandiConstants.ACTIVITY_SOURCE_NEW);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@SuppressWarnings("ucd")
	public void onMoveUpButtonClick(View view) {
		for (int i = mActiveSources.size() - 1; i >= 0; i--) {
			if (mActiveSources.get(i).checked) {
				mActiveSources.get(i).position -= 2;
			}
		}
		Collections.sort(mActiveSources, new Source.SortSourcesBySourcePosition());
		Integer position = 0;
		for (Source source : mActiveSources) {
			source.position = position;
			position++;
		}
		mList.invalidateViews();
	}

	@SuppressWarnings("ucd")
	public void onMoveDownButtonClick(View view) {
		for (int i = mActiveSources.size() - 1; i >= 0; i--) {
			if (mActiveSources.get(i).checked) {
				mActiveSources.get(i).position += 2;
			}
		}
		Collections.sort(mActiveSources, new Source.SortSourcesBySourcePosition());
		Integer position = 0;
		for (Source source : mActiveSources) {
			source.position = position;
			position++;
		}
		mList.invalidateViews();
	}

	@SuppressWarnings("ucd")
	public void onListItemClick(View view) {
		final Intent intent = new Intent(this, SourceBuilder.class);
		final CheckBox check = (CheckBox) ((View) view.getParent()).findViewById(R.id.check);
		mSourceEditing = (Source) check.getTag();
		final String jsonSource = HttpService.convertObjectToJsonSmart(mSourceEditing, false, true);
		intent.putExtra(CandiConstants.EXTRA_SOURCE, jsonSource);
		startActivityForResult(intent, CandiConstants.ACTIVITY_SOURCE_EDIT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == CandiConstants.ACTIVITY_SOURCE_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String jsonSource = extras.getString(CandiConstants.EXTRA_SOURCE);
					if (jsonSource != null) {
						final Source sourceUpdated = (Source) HttpService.convertJsonToObjectInternalSmart(jsonSource, ServiceDataType.Source);
						if (sourceUpdated != null) {
							/* Copy changes */
							mSourceEditing.label = sourceUpdated.label;
							mSourceEditing.id = sourceUpdated.id;
							mSourceEditing.url = sourceUpdated.url;
							mSourceEditing.photo = sourceUpdated.photo;
							mList.invalidateViews();
						}
					}
				}
			}
			else if (requestCode == CandiConstants.ACTIVITY_SOURCE_NEW) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String jsonSource = extras.getString(CandiConstants.EXTRA_SOURCE);
					if (jsonSource != null) {
						final Source sourceNew = (Source) HttpService.convertJsonToObjectInternalSmart(jsonSource, ServiceDataType.Source);
						if (sourceNew != null) {
							sourceNew.checked = false;
							mActiveSources.add(sourceNew);

							/* Rebuild the position numbering */
							Integer position = 0;
							for (Source source : mActiveSources) {
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

	private void splitSources(List<Source> sources) {
		mActiveSources.clear();
		mSystemSources.clear();

		for (Source source : sources) {
			if (source.system != null && source.system) {
				mSystemSources.add(source);
			}
			else {
				mActiveSources.add(source);
			}
		}
	}

	private List<Source> mergeSources() {
		List<Source> sources = new ArrayList<Source>();
		sources.addAll(mSystemSources);
		sources.addAll(mActiveSources);
		return sources;
	}

	private void confirmSourceDelete() {

		/* How many are we deleting? */
		Integer deleteCount = 0;
		for (int i = mActiveSources.size() - 1; i >= 0; i--) {
			if (mActiveSources.get(i).checked) {
				deleteCount++;
			}
		}

		final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final ViewGroup customView = (ViewGroup) inflater.inflate(R.layout.temp_delete_sources, null);
		final TextView message = (TextView) customView.findViewById(R.id.message);
		final LinearLayout list = (LinearLayout) customView.findViewById(R.id.list);
		for (Source source : mActiveSources) {
			if (source.checked) {
				View sourceView = inflater.inflate(R.layout.temp_listitem_delete_sources, null);
				WebImageView image = (WebImageView) sourceView.findViewById(R.id.image);
				TextView title = (TextView) sourceView.findViewById(R.id.title);
				if (source.label != null) {
					title.setText(source.label);
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

		message.setText((deleteCount == 1)
				? R.string.alert_source_delete_message_single
				: R.string.alert_source_delete_message_multiple);

		final AlertDialog dialog = AircandiCommon.showAlertDialog(null
				, getResources().getString(R.string.alert_source_delete_title)
				, null
				, customView
				, this
				, android.R.string.ok
				, android.R.string.cancel
				, null
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							for (int i = mActiveSources.size() - 1; i >= 0; i--) {
								if (mActiveSources.get(i).checked) {
									mActiveSources.remove(i);
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
		final AlertDialog dialog = AircandiCommon.showAlertDialog(null
				, getResources().getString(R.string.alert_sources_dirty_exit_title)
				, getResources().getString(R.string.alert_sources_dirty_exit_message)
				, null
				, SourcesBuilder.this
				, R.string.alert_dirty_save
				, android.R.string.cancel
				, R.string.alert_dirty_discard
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							gatherAndExit();
						}
						else if (which == Dialog.BUTTON_NEUTRAL) {
							setResult(Activity.RESULT_CANCELED);
							finish();
							AnimUtils.doOverridePendingTransition(SourcesBuilder.this, TransitionType.FormToPage);
						}
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	private void loadSourceSuggestions(final List<Source> sources, final Boolean autoInsert, final Entity entity) {
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy(true);
				mCommon.startBusyIndicator();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("LoadSourceSuggestions");
				ModelResult result = ProxiManager.getInstance().getEntityModel().getSourceSuggestions(sources, entity);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					final List<Source> sourcesProcessed = (List<Source>) result.serviceResponse.data;
					if (autoInsert) {
						if (sourcesProcessed.size() > 0) {

							/* First make sure they have default captions */
							for (Source source : sourcesProcessed) {
								if (source.label == null) {
									source.label = source.type;
								}
							}
							int activeCountOld = mActiveSources.size();
							splitSources(sourcesProcessed);
							int activeCountNew = mActiveSources.size();
							if (activeCountNew == activeCountOld) {
								ImageUtils.showToastNotification(getResources().getString(R.string.toast_source_no_links), Toast.LENGTH_SHORT);
							}
							else {
								ImageUtils.showToastNotification(getResources().getString((sourcesProcessed.size() == 1)
										? R.string.toast_source_linked
										: R.string.toast_sources_linked), Toast.LENGTH_SHORT);
							}
						}

					}
				}
				bind();
				mCommon.hideBusy(true);
			}
		}.execute();
	}

	private void gatherAndExit() {
		final Intent intent = new Intent();
		final List<String> sourceStrings = new ArrayList<String>();
		List<Source> sources = mergeSources();

		for (Source source : sources) {
			sourceStrings.add(HttpService.convertObjectToJsonSmart(source, true, true));
		}

		intent.putStringArrayListExtra(CandiConstants.EXTRA_SOURCES, (ArrayList<String>) sourceStrings);
		setResult(Activity.RESULT_OK, intent);
		finish();
		AnimUtils.doOverridePendingTransition(this, TransitionType.FormToPage);
	}

	private Boolean isDirty() {

		/* Gather */
		List<Source> sources = mergeSources();

		final List<String> jsonSources = new ArrayList<String>();
		for (Source source : sources) {
			jsonSources.add(HttpService.convertObjectToJsonSmart(source, true, true));
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
				if (!jsonSource.equals(mJsonSourcesOriginal.get(position))) {
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
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.accept) {
			gatherAndExit();
			return true;
		}
		else if (item.getItemId() == R.id.cancel) {
			if (isDirty()) {
				confirmDirtyExit();
			}
			else {
				setResult(Activity.RESULT_CANCELED);
				finish();
				AnimUtils.doOverridePendingTransition(SourcesBuilder.this, TransitionType.FormToPage);
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
	protected int getLayoutId() {
		return R.layout.builder_sources;
	}
}