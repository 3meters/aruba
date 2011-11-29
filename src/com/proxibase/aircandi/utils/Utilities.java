package com.proxibase.aircandi.utils;

import android.os.CountDownTimer;
import android.util.Log;

import com.proxibase.aircandi.core.CandiConstants;

public class Utilities {

	public static void Log(String tag, String task, String message) {
		if (CandiConstants.MODE_DEBUG)
			Log.d(tag, task + ": " + message);
	}
	
	public static class SimpleCountDownTimer extends CountDownTimer {

		private long	mMillisUntilFinished;

		public SimpleCountDownTimer(long millisInFuture) {
			this(millisInFuture, millisInFuture);
		}

		public SimpleCountDownTimer(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
			mMillisUntilFinished = millisInFuture;
		}

		@Override
		public void onFinish() {
			mMillisUntilFinished = 0;
		}

		@Override
		public void onTick(long millisUntilFinished) {
			mMillisUntilFinished = millisUntilFinished;
		}

		public long getMillisUntilFinished() {
			return mMillisUntilFinished;
		}
	}

	
}