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
import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.ApplinkListAdapter;
import com.aircandi.components.FontManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiManager;
import com.aircandi.components.ProxiManager.ModelResult;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ServiceDataType;
import com.aircandi.service.objects.Applink;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.ui.widgets.BounceListView;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.ImageUtils;

public class SourcesBuilder extends FormActivity {

	private BounceListView		mList;
	private TextView			mMessage;
	private final List<Entity>	mSystemApplinks	= new ArrayList<Entity>();
	private final List<Entity>	mActiveApplinks	= new ArrayList<Entity>();
	private String				mEntityId;
	private Entity				mEntity;
	private Applink				mApplinkEditing;
	private List<String>		mJsonApplinksOriginal;

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
			final List<String> jsonApplinks = extras.getStringArrayList(Constants.EXTRA_APPLINKS);
			if (jsonApplinks != null) {
				mJsonApplinksOriginal = jsonApplinks;
				List<Applink> applinks = new ArrayList<Applink>();
				for (String jsonApplink : jsonApplinks) {
					Applink source = (Applink) HttpService.convertJsonToObjectInternalSmart(jsonApplink, ServiceDataType.Applink);
					applinks.add(source);
				}
				splitApplinks(applinks);
			}
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			if (mEntityId != null) {
				mEntity = ProxiManager.getInstance().getEntityModel().getCacheEntity(mEntityId);
			}
		}

		mMessage = (TextView) findViewById(R.id.message);
		mList = (BounceListView) findViewById(R.id.list);

		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.message));
	}

	private void bind() {

		if (mActiveApplinks.size() == 0) {
			mCommon.hideBusy(true); // visible by default
			mMessage.setText(R.string.sources_builder_empty);
			mMessage.setVisibility(View.VISIBLE);
		}
		else {
			mMessage.setVisibility(View.GONE);
			Integer position = 0;
			for (Entity source : mActiveApplinks) {
				source.checked = false;
				source.position = position;
				position++;
			}
		}
		final ApplinkListAdapter adapter = new ApplinkListAdapter(this, mActiveApplinks, R.layout.temp_listitem_sources_builder);
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
		final Applink source = (Applink) check.getTag();
		source.checked = check.isChecked();
	}

	@SuppressWarnings("ucd")
	public void onSuggestLinksButtonClick(View view) {
		/* Go get source suggestions again */
		List<Entity> applinks = mergeApplinks();
		loadApplinkSuggestions(applinks, true, (Place) mEntity);
	}

	@SuppressWarnings("ucd")
	public void onDeleteButtonClick(View view) {
		for (int i = mActiveApplinks.size() - 1; i >= 0; i--) {
			if (mActiveApplinks.get(i).checked) {
				confirmSourceDelete();
				return;
			}
		}
	}

	@SuppressWarnings("ucd")
	public void onAddButtonClick(View view) {
		final Intent intent = new Intent(this, SourceBuilder.class);
		startActivityForResult(intent, Constants.ACTIVITY_SOURCE_NEW);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@SuppressWarnings("ucd")
	public void onMoveUpButtonClick(View view) {
		for (int i = mActiveApplinks.size() - 1; i >= 0; i--) {
			if (mActiveApplinks.get(i).checked) {
				mActiveApplinks.get(i).position -= 2;
			}
		}
		Collections.sort(mActiveApplinks, new Entity.SortEntitiesByPosition());
		Integer position = 0;
		for (Entity source : mActiveApplinks) {
			source.position = position;
			position++;
		}
		mList.invalidateViews();
	}

	@SuppressWarnings("ucd")
	public void onMoveDownButtonClick(View view) {
		for (int i = mActiveApplinks.size() - 1; i >= 0; i--) {
			if (mActiveApplinks.get(i).checked) {
				mActiveApplinks.get(i).position += 2;
			}
		}
		Collections.sort(mActiveApplinks, new Entity.SortEntitiesByPosition());
		Integer position = 0;
		for (Entity source : mActiveApplinks) {
			source.position = position;
			position++;
		}
		mList.invalidateViews();
	}

	@SuppressWarnings("ucd")
	public void onListItemClick(View view) {
		final Intent intent = new Intent(this, SourceBuilder.class);
		final CheckBox check = (CheckBox) ((View) view.getParent()).findViewById(R.id.check);
		mApplinkEditing = (Applink) check.getTag();
		final String jsonSource = HttpService.convertObjectToJsonSmart(mApplinkEditing, false, true);
		intent.putExtra(Constants.EXTRA_SOURCE, jsonSource);
		startActivityForResult(intent, Constants.ACTIVITY_SOURCE_EDIT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == Constants.ACTIVITY_SOURCE_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String jsonSource = extras.getString(Constants.EXTRA_SOURCE);
					if (jsonSource != null) {
						final Applink sourceUpdated = (Applink) HttpService.convertJsonToObjectInternalSmart(jsonSource, ServiceDataType.Applink);
						if (sourceUpdated != null) {
							/* Copy changes */
							mApplinkEditing.name = sourceUpdated.name;
							mApplinkEditing.id = sourceUpdated.id;
							mApplinkEditing.url = sourceUpdated.url;
							mApplinkEditing.photo = sourceUpdated.photo;
							mList.invalidateViews();
						}
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_SOURCE_NEW) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String jsonSource = extras.getString(Constants.EXTRA_SOURCE);
					if (jsonSource != null) {
						final Applink sourceNew = (Applink) HttpService.convertJsonToObjectInternalSmart(jsonSource, ServiceDataType.Applink);
						if (sourceNew != null) {
							sourceNew.checked = false;
							mActiveApplinks.add(sourceNew);

							/* Rebuild the position numbering */
							Integer position = 0;
							for (Entity source : mActiveApplinks) {
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

	private void splitApplinks(List<Applink> applinks) {
		mActiveApplinks.clear();
		mSystemApplinks.clear();

		for (Applink applink : applinks) {
			if (applink.system != null && applink.system) {
				mSystemApplinks.add(applink);
			}
			else {
				mActiveApplinks.add(applink);
			}
		}
	}

	private List<Entity> mergeApplinks() {
		List<Entity> applinks = new ArrayList<Entity>();
		applinks.addAll(mSystemApplinks);
		applinks.addAll(mActiveApplinks);
		return applinks;
	}

	private void confirmSourceDelete() {

		/* How many are we deleting? */
		Integer deleteCount = 0;
		for (int i = mActiveApplinks.size() - 1; i >= 0; i--) {
			if (mActiveApplinks.get(i).checked) {
				deleteCount++;
			}
		}

		final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final ViewGroup customView = (ViewGroup) inflater.inflate(R.layout.temp_delete_sources, null);
		final TextView message = (TextView) customView.findViewById(R.id.message);
		final LinearLayout list = (LinearLayout) customView.findViewById(R.id.list);
		for (Entity applink : mActiveApplinks) {
			if (applink.checked) {
				View sourceView = inflater.inflate(R.layout.temp_listitem_delete_sources, null);
				WebImageView image = (WebImageView) sourceView.findViewById(R.id.image);
				TextView title = (TextView) sourceView.findViewById(R.id.title);
				if (applink.name != null) {
					title.setText(applink.name);
				}
				image.setTag(applink);
				final String imageUri = applink.getPhotoUri();
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
							for (int i = mActiveApplinks.size() - 1; i >= 0; i--) {
								if (mActiveApplinks.get(i).checked) {
									mActiveApplinks.remove(i);
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

	private void loadApplinkSuggestions(final List<Entity> applinks, final Boolean autoInsert, final Place entity) {
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy(true);
				mCommon.startBodyBusyIndicator();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("LoadSourceSuggestions");
				ModelResult result = ProxiManager.getInstance().getEntityModel().getApplinkSuggestions(applinks, entity);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					final List<Applink> sourcesProcessed = (List<Applink>) result.serviceResponse.data;
					if (autoInsert) {
						if (sourcesProcessed.size() > 0) {

							/* First make sure they have default captions */
							for (Applink source : sourcesProcessed) {
								if (source.name == null) {
									source.name = source.type;
								}
							}
							int activeCountOld = mActiveApplinks.size();
							splitApplinks(sourcesProcessed);
							int activeCountNew = mActiveApplinks.size();
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
		List<Entity> applinks = mergeApplinks();

		for (Entity source : applinks) {
			sourceStrings.add(HttpService.convertObjectToJsonSmart(source, true, true));
		}

		intent.putStringArrayListExtra(Constants.EXTRA_APPLINKS, (ArrayList<String>) sourceStrings);
		setResult(Activity.RESULT_OK, intent);
		finish();
		AnimUtils.doOverridePendingTransition(this, TransitionType.FormToPage);
	}

	private Boolean isDirty() {

		/* Gather */
		List<Entity> applinks = mergeApplinks();

		final List<String> jsonApplinks = new ArrayList<String>();
		for (Entity applink : applinks) {
			jsonApplinks.add(HttpService.convertObjectToJsonSmart(applink, true, true));
		}

		if (mJsonApplinksOriginal == null) {
			if (jsonApplinks.size() > 0) {
				return true;
			}
		}
		else {

			if (jsonApplinks.size() != mJsonApplinksOriginal.size()) {
				return true;
			}

			int position = 0;
			for (String jsonApplink : jsonApplinks) {
				if (!jsonApplink.equals(mJsonApplinksOriginal.get(position))) {
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