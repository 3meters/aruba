package com.aircandi.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.aircandi.R;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.EndlessAdapter;
import com.aircandi.components.FontManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.images.ImageRequest;
import com.aircandi.components.images.ImageRequestBuilder;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.Tip;
import com.aircandi.ui.base.CandiActivity;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.ui.widgets.WebImageView;

public class TipList extends CandiActivity {

	private ListView		mListView;
	private List<Tip>		mTips			= new ArrayList<Tip>();
	private String			mSource;
	private String			mSourceId;
	private long			mOffset			= 0;

	private LayoutInflater	mInflater;
	private static long		PAGE_SIZE		= 30L;
	private static long		LIST_MAX		= 300L;
	protected int			mLastResultCode	= Activity.RESULT_OK;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			initialize();
			configureActionBar();
			bind(true);
		}
	}

	protected void initialize() {
		Entity entity = ProxiExplorer.getInstance().getEntityModel().getCacheEntity(mCommon.mEntityId);
		mSource = entity.place.source;
		mSourceId = entity.place.sourceId;
		mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mListView = (ListView) findViewById(R.id.list);
	}

	private void configureActionBar() {
		/*
		 * Navigation setup for action bar icon and title
		 */
		Entity collection = ProxiExplorer.getInstance().getEntityModel().getCacheEntity(mCommon.mCollectionId);
		mCommon.mActionBar.setTitle(collection.name);
		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);

	}

	protected void bind(final Boolean refresh) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				ServiceResponse serviceResponse = loadTips(PAGE_SIZE, 0);
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					mTips = (ArrayList<Tip>) serviceResponse.data;
					mOffset += PAGE_SIZE;
					mListView.setAdapter(new EndlessTipAdapter(mTips));
				}
				else {
					mCommon.handleServiceError(serviceResponse, ServiceOperation.TipBrowse);
				}
				mCommon.hideBusy();
			}

		}.execute();
	}

	private ServiceResponse loadTips(long count, long offset) {

		ServiceResponse serviceResponse = new ServiceResponse();

		Bundle parameters = new Bundle();
		parameters.putString("source", mSource);
		parameters.putString("sourceId", mSourceId);
		parameters.putLong("limit", count);
		parameters.putLong("skip", offset);

		ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getPlaceTips")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.Success) {
			String jsonResponse = (String) serviceResponse.data;
			ServiceData serviceData = (ServiceData) ProxibaseService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Tip);
			List<Tip> tips = (List<Tip>) serviceData.data;
			serviceResponse.data = tips;
		}
		return serviceResponse;
	}

	public void doRefresh() {
		/* Called from AircandiCommon */
		mTips.clear();
		bind(true);
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes/enums
	// --------------------------------------------------------------------------------------------

	class EndlessTipAdapter extends EndlessAdapter {

		List<Tip>	moreTips	= new ArrayList<Tip>();

		EndlessTipAdapter(List<Tip> list) {
			super(new ListAdapter(list));
		}

		@Override
		protected View getPendingView(ViewGroup parent) {
			View view = mInflater.inflate(R.layout.temp_candi_list_item_placeholder, null);
			return (view);
		}

		@Override
		protected boolean cacheInBackground() {

			/* What happens if there is a connectivity error? */
			moreTips.clear();

			ServiceResponse serviceResponse = loadTips(PAGE_SIZE, mOffset);
			if (serviceResponse.responseCode == ResponseCode.Success) {
				moreTips = (ArrayList<Tip>) serviceResponse.data;
				Logger.d(this, "Request more tips: start = " + String.valueOf(mOffset)
						+ " new total = "
						+ String.valueOf(getWrappedAdapter().getCount() + moreTips.size()));
				mOffset += PAGE_SIZE;
				return ((getWrappedAdapter().getCount() + moreTips.size()) < LIST_MAX);
			}
			else {
				mCommon.handleServiceError(serviceResponse, ServiceOperation.PictureBrowse);
				return false;
			}
		}

		@Override
		protected void appendCachedData() {
			ArrayAdapter<Tip> list = (ArrayAdapter<Tip>) getWrappedAdapter();
			for (Tip tip : moreTips) {
				list.add(tip);
			}
		}
	}

	public class ListAdapter extends ArrayAdapter<Tip> {

		public ListAdapter(List<Tip> items) {
			super(TipList.this, 0, items);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			final ViewHolder holder;
			Tip itemData = (Tip) mTips.get(position);

			if (view == null) {
				view = getLayoutInflater().inflate(R.layout.temp_listitem_tip, null);
				holder = new ViewHolder();
				holder.image = (WebImageView) view.findViewById(R.id.image);
				holder.user = (UserView) view.findViewById(R.id.author);
				holder.description = (TextView) view.findViewById(R.id.description);
				
				FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.description));
				
				view.setTag(holder);
			}
			else {
				holder = (ViewHolder) view.getTag();
			}

			if (itemData != null) {
				Tip tip = itemData;

				/* Author block */
				if (holder.user != null) {
					setVisibility(holder.user, View.GONE);
					if (tip.user != null) {
						holder.user.bindToAuthor(tip.user, tip.createdAt.longValue(), false);
						setVisibility(holder.user, View.VISIBLE);
					}
				}

				if (holder.description != null) {
					setVisibility(holder.description, View.GONE);
					if (tip.text != null && tip.text.length() > 0) {
						holder.description.setText(tip.text);
						setVisibility(holder.description, View.VISIBLE);
					}
				}

				if (holder.image != null) {

					String imageUri = tip.user.photo.getImageSizedUri(100, 100);
					if (holder.image.getImageUri() == null || !imageUri.equals((String) holder.image.getImageUri())) {
						/*
						 * We are aggresive about recycling bitmaps when we can.
						 */
						holder.image.recycleBitmap();
						if (imageUri != null && imageUri.length() != 0) {
							ImageRequestBuilder builder = new ImageRequestBuilder(holder.image)
									.setFromUris(imageUri, null);

							ImageRequest imageRequest = builder.create();
							holder.image.setImageRequest(imageRequest);
						}
					}
				}
			}
			return view;
		}

		@Override
		public Tip getItem(int position) {
			return mTips.get(position);
		}

		public boolean areAllItemsEnabled() {
			return false;
		}

		public boolean isEnabled(int position) {
			return true;
		}

	}

	private class ViewHolder {

		public WebImageView	image;
		public UserView		user;
		public TextView		description;
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------
	protected static void setVisibility(View view, Integer visibility) {
		if (view != null) {
			view.setVisibility(visibility);
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.tip_list;
	}
}