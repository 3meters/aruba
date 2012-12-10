package com.aircandi;

import android.os.Bundle;

public class UserCandiForm extends CandiFormBase {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			initialize();
			bind(false);
		}
	}

	public void bind(Boolean refresh) {
		doBind(refresh, false);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.candi_form;
	}
}