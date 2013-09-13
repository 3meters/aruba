package com.aircandi.ui.base;

import android.os.Bundle;

public interface IBase {

	public void onAdd();

	public void onRefresh();

	public void onError();

	public void onHelp();

	public void unpackIntent();

	public void initialize(Bundle savedInstanceState);

	public void showBusy();

	public void showBusy(Object message, Boolean actionBarOnly);

	public void showBusyTimed(Integer duration, Boolean actionBarOnly);

	public void hideBusy();
	
	public enum BindingMode {
		AUTO,
		SERVICE,
	}	
}
