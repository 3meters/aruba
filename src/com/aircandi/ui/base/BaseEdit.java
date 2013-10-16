package com.aircandi.ui.base;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.WindowManager;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.service.objects.Entity;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;

public abstract class BaseEdit extends BaseActivity implements IEdit {

	protected Boolean	mEditing	= false;
	protected Boolean	mDirty		= false;

	/* Inputs */
	protected Boolean	mSkipSave	= false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (!isFinishing()) {
			unpackIntent();
			configureActionBar();
			initialize(savedInstanceState);
			bind(BindingMode.AUTO);
			draw();
		}
	}

	@Override
	public void unpackIntent() {
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mSkipSave = extras.getBoolean(Constants.EXTRA_SKIP_SAVE, false);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {}

	@Override
	public void bind(BindingMode mode) {}

	@Override
	public void draw() {}

	@Override
	protected void configureActionBar() {
		super.configureActionBar();
		if (mActionBar != null) {
			mActionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {
		Routing.route(this, Route.CANCEL);
	}

	@Override
	public void onCancel(Boolean force) {
		if (!force && mDirty) {
			confirmDirtyExit();
		}
		else {
			setResult(Activity.RESULT_CANCELED);
			finish();
			Animate.doOverridePendingTransition(this, TransitionType.PAGE_BACK);
		}
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

	protected void insert() {}

	protected void update() {}

	protected void delete() {}

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
								onAccept();
							}
							else if (which == Dialog.BUTTON_NEUTRAL) {
								Routing.route(BaseEdit.this, Route.CANCEL_FORCE);
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
								Routing.route(BaseEdit.this, Route.CANCEL_FORCE);
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

}