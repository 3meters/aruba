package com.proxibase.aircandi.components;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.graphics.Bitmap;
import android.os.CountDownTimer;
import android.os.Debug;

public class Utilities {

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

	public static class Stopwatch {

		{
			Debug.startAllocCounting();
		}
		long	start	= System.nanoTime();

		void stop() {
			long elapsed = (System.nanoTime() - start) / 1000;
			Debug.stopAllocCounting();
			Logger.i(this, "CandiRadarActivity: " + elapsed + "us, "
																+ Debug.getThreadAllocCount() + " allocations, "
																+ Debug.getThreadAllocSize() + " bytes");
		}
	}

	public static final String md5(final String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {
				String h = Integer.toHexString(0xFF & messageDigest[i]);
				while (h.length() < 2) {
					h = "0" + h;
				}
				hexString.append(h);
			}
			return hexString.toString();

		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static final String md5HashForBitmap(Bitmap bitmap) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			byte[] bytes = ImageManager.byteArrayForBitmap(bitmap);
			digest.update(bytes);
			byte messageDigest[] = digest.digest();
			return messageDigest.toString();

			//			// Create Hex String
			//			StringBuffer hexString = new StringBuffer();
			//			for (int i = 0; i < messageDigest.length; i++) {
			//				String h = Integer.toHexString(0xFF & messageDigest[i]);
			//				while (h.length() < 2) {
			//					h = "0" + h;
			//				}
			//				hexString.append(h);
			//			}
			//			return hexString.toString();

		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}
}