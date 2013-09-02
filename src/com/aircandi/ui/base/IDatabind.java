package com.aircandi.ui.base;

public interface IDatabind {

	public void onError();

	public void onRefresh();

	public void onAdd();

	public void onHelp();

	public void databind(Boolean refresh);

	public void draw();

	public void showBusy();

	public void showBusy(Object message);
	
	public void hideBusy();
}
