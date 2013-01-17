package com.aircandi.components;

import java.util.Locale;

public class StopWatch {
	protected long	totalTime;
	protected long	lastThreshold;

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
			throw new IllegalStateException("Stopwatch is stopped.");
		}
		final long now = System.nanoTime();
		final long lapTime = now - lastThreshold;
		totalTime += lapTime;
		lastThreshold = System.nanoTime();
		String stats = "segment time: " + String.valueOf(lapTime / 1000000) + "ms, total time: " + String.valueOf(totalTime / 1000000) + "ms";
		if (message != null) {
			stats = message + ": " + stats;
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
	public void start() {
		Logger.v(this, "Stopwatch started");
		totalTime = 0;
		lastThreshold = System.nanoTime();
	}

	/**
	 * Suspends time watching, returns last lap time.
	 */
	public long stop() {
		final long lapTime = processSegmentTime(null);
		lastThreshold = 0;
		//totalTime = 0;
		Logger.v(this, "Stopwatch stopped");
		return lapTime;
	}
}