package com.aircandi.ui.edit;

import java.util.HashMap;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.service.objects.Document;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.widgets.AirEditText;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.UI;

public class ReportEdit extends BaseEntityEdit {

	private Document	mReport;
	private String		mReportType;

	@Override
	public void initialize(Bundle savedInstanceState) {
		/*
		 * Feedback are not really an entity type so we handle
		 * all the expected initialization.
		 */
		mDescription = (AirEditText) findViewById(R.id.description);

		if (mDescription != null) {
			mDescription.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					mDirty = false;
					if (s.toString() != null || !s.toString().equals("")) {
						mDirty = true;
					}
				}
			});
		}

		((TextView) findViewById(R.id.content_message)).setText(getString(R.string.report_message) + " " + mEntitySchema + "?");
		mEditing = false;
	}

	@Override
	public void bind(BindingMode mode) {
		/*
		 * Not a real entity so we completely override databind.
		 */
		mReport = new Document();
		mReport.type = "report";
		mReport.name = "aircandi";
		mReport.data = new HashMap<String, Object>();
	}

	@Override
	public void draw() {
		((UserView) findViewById(R.id.created_by)).databind(Aircandi.getInstance().getCurrentUser(), null);
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onAccept() {
		if (validate()) {
			super.onAccept();
		}
	}

	public void onRadioButtonClicked(View view) {
		mReportType = (String) view.getTag();
		mDirty = true;
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	protected String getLinkType() {
		return null;
	}

	@Override
	protected void gather() {
		if (!TextUtils.isEmpty(mDescription.getText().toString())) {
			mReport.data.put("message", (Object) mDescription.getText().toString().trim());
		}
		mReport.data.put("type", (Object) mReportType);
		mReport.data.put("target", (Object) mEntity.id);
	}

	// --------------------------------------------------------------------------------------------
	// Services
	// --------------------------------------------------------------------------------------------

	@Override
	protected boolean validate() {
		if (!super.validate()) {
			return false;
		}
		if (TextUtils.isEmpty(mReportType)) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_report_option)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		return true;
	}

	@Override
	protected void insert() {
		Logger.i(this, "Insert report");

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy(R.string.progress_sending);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("InsertReport");
				mReport.createdDate = DateTime.nowDate().getTime();
				final ModelResult result = EntityManager.getInstance().insertReport(mReport);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.showToastNotification(getString(R.string.alert_report_sent), Toast.LENGTH_SHORT);
					finish();
				}
				else {
					Errors.handleError(ReportEdit.this, result.serviceResponse);
				}
				hideBusy();
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.report_edit;
	}
}