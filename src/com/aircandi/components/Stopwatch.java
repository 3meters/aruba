package com.aircandi.components;


public class Stopwatch {

	private long	totalTime;
	private long	lastThreshold;
	private final String	name;

	public Stopwatch(String name) {
		this.name = name;
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
	
	public boolean isStarted() {
		return (lastThreshold > 0);
	}
}