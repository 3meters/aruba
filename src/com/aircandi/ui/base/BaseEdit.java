package com.aircandi.ui.base;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.BusyManager;
import com.aircandi.service.objects.Entity;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;

public abstract class BaseEdit extends BaseActivity {

	protected Boolean	mEditing	= false;
	protected Boolean	mDirty		= false;

	/* Inputs */
	protected Boolean	mSkipSave	= false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			unpackIntent();
			initialize(savedInstanceState);
			bind();
			draw();
		}
	}

	@Override
	protected void unpackIntent() {
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mSkipSave = extras.getBoolean(Constants.EXTRA_SKIP_SAVE, false);
		}
	}

	protected void initialize(Bundle savedInstanceState) {
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mBusyManager = new BusyManager(this);
	}

	protected abstract void bind();

	protected abstract void draw();

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {
		if (isDirty()) {
			confirmDirtyExit();
		}
		else {
			Routing.route(this, Route.Back, TransitionType.FormToPage);
		}
	}

	public abstract void onAccept();

	public void onCancel() {
		onBackPressed();
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	public Boolean isDirty() {
		return mDirty;
	}

	// --------------------------------------------------------------------------------------------
	// Services
	// --------------------------------------------------------------------------------------------

	protected abstract void insert();
	
	protected abstract void update();

	protected abstract void delete();

	protected void confirmDirtyExit() {
		if (!mSkipSave) {
			final AlertDialog dialog = Dialogs.alertDialog(null
					, getResources().getString(R.string.alert_dirty_exit_title)
					, getResources().getString(R.string.alert_dirty_exit_message)
					, null
					, BaseEdit.this
					, R.string.alert_dirty_save
					, android.R.string.cancel
					, R.string.alert_dirty_discard
					, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (which == Dialog.BUTTON_POSITIVE) {
								if (mEditing) {
									update();
								}
								else {
									update();
								}
							}
							else if (which == Dialog.BUTTON_NEUTRAL) {
								Routing.route(BaseEdit.this, Route.Cancel);
							}
						}
					}
					, null);
			dialog.setCanceledOnTouchOutside(false);
		}
		else {
			final AlertDialog dialog = Dialogs.alertDialog(null
					, getResources().getString(R.string.alert_dirty_exit_title)
					, getResources().getString(R.string.alert_dirty_exit_message)
					, null
					, BaseEdit.this
					, R.string.alert_dirty_discard
					, android.R.string.cancel
					, null
					, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (which == Dialog.BUTTON_POSITIVE) {
								Routing.route(BaseEdit.this, Route.Cancel);
							}
						}
					}
					, null);
			dialog.setCanceledOnTouchOutside(false);
		}
	}

	public void confirmDelete() {
		final AlertDialog dialog = Dialogs.alertDialog(null
				, getResources().getString(R.string.alert_delete_title)
				, getResources().getString(R.string.alert_delete_message_single)
				, null
				, this
				, android.R.string.ok
				, android.R.string.cancel
				, null
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							delete();
						}
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	protected boolean validate() {
		return true;
	}

	protected void beforeInsert(Entity entity) {}

	protected void afterInsert(Entity entity) {}

	protected void beforeUpdate(Entity entity) {}

	protected void afterUpdate(Entity entity) {}

}