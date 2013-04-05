package com.aircandi.components;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

public class BusProvider {

	private static class BusHolder {
		public static final Bus	instance	= new Bus(ThreadEnforcer.ANY);
	}

	public static Bus getInstance() {
		return BusHolder.instance;
	}

	private BusProvider() {}
}
