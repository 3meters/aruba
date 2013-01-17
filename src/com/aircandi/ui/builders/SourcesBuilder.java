package com.aircandi.ui.builders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;

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
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Source;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.ui.widgets.BounceListView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

public class SourcesBuilder extends FormActivity {

	private BounceListView	mList;
	private List<Source>	mSources	= new ArrayList<Source>();
	private Entity			mEntity;
	private Source			mSourceEditing;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		initialize();
		if (mEntity != null && mEntity.sourceSuggestions == null) {
			loadSourceSuggestions(mEntity);
		}
		else {
			bind();
			mCommon.hideBusy();
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
				for (String jsonSource : jsonSources) {
					Source source = (Source) ProxibaseService.convertJsonToObjectInternalSmart(jsonSource, ServiceDataType.Source);
					mSources.add(source);
				}
			}
		}

		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);

		mList = (BounceListView) findViewById(R.id.list);
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_cancel));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_save));
	}

	public void bind() {

		Integer position = 0;
		for (Source source : mSources) {
			source.checked = false;
			source.position = position;
			position++;
		}

		SourceListAdapter adapter = new SourceListAdapter(this, mSources, R.layout.temp_listitem_sources_builder);
		mList.setAdapter(adapter);
	}

	@Override
	public void onBackPressed() {
		gatherAndExit();
	}

	public void gatherAndExit() {
		Intent intent = new Intent();
		List<String> sourceStrings = new ArrayList<String>();
		for (Source source : mSources) {
			sourceStrings.add(ProxibaseService.convertObjectToJsonSmart(source, true, true));
		}
		intent.putStringArrayListExtra(CandiConstants.EXTRA_SOURCES, (ArrayList<String>) sourceStrings);
		setResult(Activity.RESULT_OK, intent);
		finish();
		AnimUtils.doOverridePendingTransition(this, TransitionType.FormToCandiPage);
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
							if (mEntity != null) {
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

	public void loadSourceSuggestions(final Entity entity) {
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("LoadSourceSuggestions");
				ModelResult result = ProxiExplorer.getInstance().getEntityModel().getSourceSuggestions(entity.sources);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					entity.sourceSuggestions = (List<Source>) result.serviceResponse.data;
					if (entity.sourceSuggestions == null) {
						entity.sourceSuggestions = new ArrayList<Source>();
					}
				}
				bind();
				mCommon.hideBusy();
			}
		}.execute();
	}

	private void scrollToBottom() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mList.setSelection(mList.getAdapter().getCount() - 1);
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onDeleteButtonClick(View view) {

		/* How many are we deleting? */
		String deleteNames = "";
		Integer deleteCount = 0;
		for (int i = mSources.size() - 1; i >= 0; i--) {
			if (mSources.get(i).checked) {
				deleteCount++;
				deleteNames += "    " + mSources.get(i).name + "\n";
			}
		}

		String message = getResources().getString(
				deleteCount == 1 ? R.string.alert_source_delete_message_single : R.string.alert_source_delete_message_multiple);
		message += "\n\n" + deleteNames;

		AlertDialog dialog = AircandiCommon.showAlertDialog(null
				, getResources().getString(R.string.alert_source_delete_title)
				, message
				, null
				, this
				, android.R.string.ok
				, android.R.string.cancel
				, new DialogInterface.OnClickListener() {

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

	public void onAddButtonClick(View view) {
		Intent intent = new Intent(this, SourceBuilder.class);
		if (mEntity != null) {
			intent.putExtra(CandiConstants.EXTRA_ENTITY_ID, mEntity.id);
		}
		startActivityForResult(intent, CandiConstants.ACTIVITY_SOURCE_NEW);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

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

	public void onListItemClick(View view) {
		Intent intent = new Intent(this, SourceBuilder.class);
		CheckBox check = (CheckBox) view.findViewById(R.id.check);
		mSourceEditing = (Source) check.getTag();
		String jsonSource = ProxibaseService.convertObjectToJsonSmart(mSourceEditing, false, true);
		intent.putExtra(CandiConstants.EXTRA_SOURCE, jsonSource);
		startActivityForResult(intent, CandiConstants.ACTIVITY_SOURCE_EDIT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	@Override
	protected Boolean isDialog() {
		return false;
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	public boolean onCreateOptionsMenu(Menu menu) {
		mCommon.doCreateOptionsMenu(menu);
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		mCommon.doPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
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