package com.aircandi.components;

import java.util.Locale;

public class Stopwatch {

	protected long		totalTime;
	protected long		lastThreshold;
	protected String	name;

	public Stopwatch(String name) {
		this.name = name;
	}

	/**
	 * Human readable time in seconds
	 * 
	 * @param nanoTime
	 * @return time in seconds
	 */
	public static final String toSeconds(long nanoTime) {
		return String.format(Locale.US, "%.9f", nanoTime / 1000000000.0);
	}

	public long getTotalTime() {
		return totalTime;
	}

	/**
	 * Returns last lap time, process statistic.
	 */
	public long segmentTime(String message) {
		return processSegmentTime(message);
	}

	private long processSegmentTime(String message) {
		if (lastThreshold == 0) {
			return 0;
		}
		final long now = System.nanoTime();
		final long lapTime = now - lastThreshold;
		totalTime += lapTime;
		lastThreshold = System.nanoTime();
		String stats = "segment time: " + String.valueOf(lapTime / 1000000) + "ms, total time: " + String.valueOf(totalTime / 1000000) + "ms";
		if (message != null) {
			stats = name + ": " + message + ": " + stats;
		}
		Logger.v(this, stats);
		return lapTime;
	}

	public Boolean isStarted() {
		return (lastThreshold != 0);
	}

	/**
	 * Starts time watching.
	 */
	public void start(String message) {
		Logger.v(this, name + ": *** Started ***: " + message);
		totalTime = 0;
		lastThreshold = System.nanoTime();
	}

	/**
	 * Suspends time watching, returns last lap time.
	 */
	public long stop(String message) {
		final long lapTime = processSegmentTime("*** Stopped ***: " + message);
		lastThreshold = 0;
		return lapTime;
	}
}