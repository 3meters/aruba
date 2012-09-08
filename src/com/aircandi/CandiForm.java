package com.aircandi;

import android.os.Bundle;

public class CandiForm extends CandiFormBase {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			bind(true);
		}
	}
}