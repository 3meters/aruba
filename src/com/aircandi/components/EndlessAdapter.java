package com.aircandi.components;

/***
 * Copyright (c) 2008-2009 CommonsWare, LLC
 * Portions (c) 2009 Google, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import com.commonsware.cwac.adapter.AdapterWrapper;

/**
 * Adapter that assists another adapter in appearing endless.
 * For example, this could be used for an adapter being
 * filled by a set of Web service calls, where each call returns
 * a "page" of data.
 * Subclasses need to be able to return, via getPendingView()
 * a row that can serve as both a placeholder while more data
 * is being appended.
 * The actual logic for loading new data should be done in
 * appendInBackground(). This method, as the name suggests,
 * is run in a background thread. It should return true if
 * there might be more data, false otherwise.
 * If your situation is such that you will not know if there
 * is more data until you do some work (e.g., make another
 * Web service call), it is up to you to do something useful
 * with that row returned by getPendingView() to let the user
 * know you are out of data, plus return false from that final
 * call to appendInBackground().
 */
public abstract class EndlessAdapter extends AdapterWrapper {

	protected abstract boolean cacheInBackground() throws Exception;

	protected abstract void appendCachedData();

	private View			pendingView		= null;
	private final AtomicBoolean	keepOnAppending	= new AtomicBoolean(true);
	private Context			context;
	private final int				pendingResource	= -1;

	/**
	 * Constructor wrapping a supplied ListAdapter
	 */
	protected EndlessAdapter(ListAdapter wrapped) {
		super(wrapped);
	}

	/**
	 * How many items are in the data set represented by this
	 * Adapter.
	 */
	@Override
	public int getCount() {
		if (keepOnAppending.get()) {
			return super.getCount() + 1; // one more for "pending"
		}
		return super.getCount();
	}

	/**
	 * Masks ViewType so the AdapterView replaces the "Pending" row when new
	 * data is loaded.
	 */
	@Override
	public int getItemViewType(int position) {
		if (position == getWrappedAdapter().getCount()) {
			return IGNORE_ITEM_VIEW_TYPE;
		}
		return super.getItemViewType(position);
	}

	/**
	 * Masks ViewType so the AdapterView replaces the "Pending" row when new
	 * data is loaded.
	 * 
	 * @see #getItemViewType(int)
	 */
	@Override
	public int getViewTypeCount() {
		return super.getViewTypeCount() + 1;
	}

	/**
	 * Get a View that displays the data at the specified
	 * position in the data set. In this case, if we are at
	 * the end of the list and we are still in append mode,
	 * we ask for a pending view and return it, plus kick
	 * off the background task to append more data to the
	 * wrapped adapter.
	 * 
	 * @param position
	 *            Position of the item whose data we want
	 * @param convertView
	 *            View to recycle, if not null
	 * @param parent
	 *            ViewGroup containing the returned View
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (position == super.getCount() && keepOnAppending.get()) {
			if (pendingView == null) {
				pendingView = getPendingView(parent);
				final AppendTask task = new AppendTask();
				task.execute();
			}

			return pendingView;
		}

		return super.getView(position, convertView, parent);
	}

	/**
	 * Called if cacheInBackground() raises a runtime exception,
	 * to allow the UI to deal with the exception on the
	 * main application thread.
	 * 
	 * @param pendingView
	 *            View representing the pending row
	 * @param e
	 *            Exception that was raised by cacheInBackground()
	 * @return true if should allow retrying appending new data, false otherwise
	 */
	private boolean onException(View pendingView, Exception e) {
		Log.e("EndlessAdapter", "Exception in cacheInBackground()", e);

		return false;
	}

	/**
	 * A background task that will be run when there is a need
	 * to append more data. Mostly, this code delegates to the
	 * subclass, to append the data in the background thread and
	 * rebind the pending view once that is done.
	 */
	private class AppendTask extends AsyncTask<Void, Void, Exception> {

		@Override
		protected Exception doInBackground(Void... params) {
			Thread.currentThread().setName("EndlessAdapter");
			Exception result = null;

			try {
				keepOnAppending.set(cacheInBackground());
			}
			catch (Exception e) {
				result = e;
			}

			return result;
		}

		@Override
		protected void onPostExecute(Exception e) {
			if (e == null) {
				appendCachedData();
			}
			else {
				keepOnAppending.set(onException(pendingView, e));
			}

			pendingView = null;
			// notifyDataSetChanged();
		}
	}

	/**
	 * Inflates pending view using the pendingResource ID passed into the constructor
	 * 
	 * @param parent
	 * @return inflated pending view, or null if the context passed into the pending view constructor was null.
	 */
	protected View getPendingView(ViewGroup parent) {
		if (context != null) {
			return LayoutInflater.from(context).inflate(pendingResource, parent, false);
		}

		throw new RuntimeException("You must either override getPendingView() or supply a pending View resource via the constructor");
	}

	
}