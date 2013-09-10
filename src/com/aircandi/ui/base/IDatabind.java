package com.aircandi.ui.base;

import android.os.Bundle;


public interface IDatabind {

	public void onError();

	public void onRefresh();

	public void onAdd();

	public void onHelp();

	public void unpackIntent();
	
	public void initialize(Bundle savedInstanceState);
	
	public void afterInitialize();
	
	public void beforeDatabind();
	
	public void databind(BindingMode mode);

	public void afterDatabind();

	public void draw();

	public void showBusy();

	public void showBusy(Object message, Boolean actionBarOnly);

	public void showBusyTimed(Integer duration, Boolean actionBarOnly);
	
	public void hideBusy();

	public enum BindingMode {
		auto,
		service,
	}
}
