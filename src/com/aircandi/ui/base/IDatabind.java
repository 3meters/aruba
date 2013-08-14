package com.aircandi.ui.base;

public interface IDatabind {

	public void onError();

	public void onRefresh();

	public void onAdd();

	public void onHelp();

	public void onDatabind(Boolean refresh);
	
	public void showBusy();
	
	public void hideBusy();
}
