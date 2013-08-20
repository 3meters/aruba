package com.aircandi.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;

public class SpinnerData {

	private List<String>	mDescriptions;
	private List<String>	mEntries;
	private List<String>	mEntryValues;

	private Context			mContext;

	public SpinnerData(Context context) {
		mContext = context;
	}

	public List<String> getDescriptions() {
		return mDescriptions;
	}

	public void setDescriptions(int descriptionsResId) {
		mDescriptions = new ArrayList(Arrays.asList(mContext.getResources().getStringArray(descriptionsResId)));
	}

	public List<String> getEntries() {
		return mEntries;
	}

	public void setEntries(int entriesResId) {
		mEntries = new ArrayList(Arrays.asList(mContext.getResources().getStringArray(entriesResId)));
	}

	public List<String> getEntryValues() {
		return mEntryValues;
	}

	public void setEntryValues(int entryValuesResId) {
		mEntryValues = new ArrayList(Arrays.asList(mContext.getResources().getStringArray(entryValuesResId)));
	}
}
