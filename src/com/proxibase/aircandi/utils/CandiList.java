package com.proxibase.aircandi.utils;

import java.util.ArrayList;

import com.proxibase.aircandi.candi.models.CandiModel;

public class CandiList<T> extends ArrayList<T> {

	private static final long	serialVersionUID	= -2567383399125318333L;

	public CandiList() {}

	public CandiList(final int capacity) {

		super(capacity);
	}

	public boolean containsKey(String key) {

		for (int i = 0; i < this.size(); i++) {
			CandiModel candiModel = (CandiModel) this.get(i);
			if (candiModel.getModelId().equals(key))
				return true;
		}
		return false;
	}

	public CandiModel getByKey(String key) {

		for (int i = 0; i < this.size(); i++) {
			CandiModel candiModel = (CandiModel) this.get(i);
			if (candiModel.getModelId().equals(key))
				return candiModel;
		}
		return null;
	}
}
