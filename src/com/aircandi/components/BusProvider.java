package com.aircandi.components;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

public class BusProvider {

	private static Bus	singletonObject;

	public static synchronized Bus getInstance() {
		if (singletonObject == null) {
			singletonObject = new Bus(ThreadEnforcer.ANY);
		}
		return singletonObject;
	}

	private BusProvider() {}
}
