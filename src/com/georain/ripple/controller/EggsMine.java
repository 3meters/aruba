package com.georain.ripple.controller;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import com.georain.ripple.controller.EggsGrid.GetImageTask;
import com.georain.ripple.controller.RippleActivity.ImageHolder;
import com.georain.ripple.model.BaseQueryListener;
import com.georain.ripple.model.Egg;
import com.georain.ripple.model.RippleRunner;
import com.georain.ripple.model.RippleService;
import com.georain.ripple.model.RippleService.QueryFormat;
import com.georain.ripple.utilities.DateUtils;

public class EggsMine extends RippleActivity
{
	private ArrayList<Object>	mListItems	= null;
	private Class				mClass		= Egg.class;
	private ImageCache			mImageCache	= null;
	private GridView			mGridView	= null;
	private ImageView			mEggDetailImage;
	private TextView			mEggDetailTitle;
	private TextView			mEggDetailDesc;
	private TextView			mEggDetailMessage;
	private TextView			mEggDetailFinder;
	private Button				mEggDetailDeleteButton;
	private LinearLayout		mEggDetailRow;
	private View				mGridViewCurrentImage;
	public static EggsMine		mSelf		= null;
	private String				mEggSetName;
	private ImageView			mEggDetailDropperImage;
	private TextView			mEggDetailDropperText;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setContentView(R.layout.eggs_mine);
		super.onCreate(savedInstanceState);
		mSelf = this;

		mGridView = (GridView) findViewById(R.id.gridview);
		mEggDetailRow = (LinearLayout) findViewById(R.id.Egg_Detail_Row);
		mEggDetailImage = (ImageView) findViewById(R.id.Egg_Detail_Image);
		mEggDetailTitle = (TextView) findViewById(R.id.Egg_Detail_Title);
		mEggDetailDesc = (TextView) findViewById(R.id.Egg_Detail_Desc);
		mEggDetailMessage = (TextView) findViewById(R.id.Egg_Detail_Message);
		mEggDetailFinder = (TextView) findViewById(R.id.Egg_Detail_Finder);
		mEggDetailDropperImage = (ImageView) findViewById(R.id.EggDrop_Icon);
		mEggDetailDropperText = (TextView) findViewById(R.id.EggDrop_Text);
		mEggDetailDeleteButton = (Button) findViewById(R.id.Egg_Detail_Delete_Button);

		setLayoutAnim_slidedown(mEggDetailRow, EggsMine.this);

		if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("EggSetName"))
			mEggSetName = getIntent().getExtras().getString("EggSetName");

		mGridView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView parent, View view, int position, long id)
			{
				mGridViewCurrentImage = view;
				mEggDetailRow.setVisibility(View.VISIBLE);

				Egg egg = (Egg) view.getTag();
				loadEggDetailUnwrapped(egg, false);
			}
		});

		// Image cache
		mImageCache = new ImageCache(getApplicationContext(), "aircandy", 100, 16);

		// Get the data
		startProgress();

		// Get the data for the list
		loadData();
	}

	// For this activity, refresh means rescan and reload point data from the service
	@Override
	public void onRefreshClick(View view)
	{
		startProgress();
		loadData();
	}

	// ----------------------------------------------------------------------------------
	// Data query
	// ----------------------------------------------------------------------------------

	public void loadData()
	{
		RippleRunner ripple = new RippleRunner();
		Bundle parameters = new Bundle();
		String method = "GetEggsForUserBySet";
		parameters.putString("userId", getCurrentUser().id);
		parameters.putString("eggSetName", mEggSetName);
		ripple.post(method, parameters, QueryFormat.Json, new ListQueryListener());
	}

	public class ListQueryListener extends BaseQueryListener
	{
		public void onComplete(final String response)
		{
			mListItems = RippleService.convertJsonToObjects(response, mClass);

			// Post the processed result back to the UI thread
			EggsMine.this.runOnUiThread(new Runnable() {
				public void run()
				{
					if (mListItems == null || mListItems.size() == 0)
						return;

					loadGrid();
					stopProgress();
				}
			});
		}

		@Override
		public void onIOException(IOException e)
		{
			// TODO Auto-generated method stub
			super.onIOException(e);
			EggsMine.this.runOnUiThread(new Runnable() {
				public void run()
				{
					RippleUI.showToastNotification(EggsMine.this, "Network error", Toast.LENGTH_SHORT);
					stopProgress();
				}
			});

		}
	}

	private void loadEggDetailUnwrapped(Egg egg, Boolean localOnly)
	{
		mEggDetailImage.setVisibility(View.VISIBLE);
		mEggDetailImage.setTag(egg);
		mEggDetailDeleteButton.setTag(egg);

		String eggResourceId = egg.eggResourceId;
		Bitmap bitmap = mImageCache.get(eggResourceId);
		if (bitmap != null)
			mEggDetailImage.setImageBitmap(bitmap);
		else
		{
			String url = Ripple.URL_RIPPLEMEDIA + "images/eggs/" + eggResourceId;
			ImageHolder imageHolder = new ImageHolder();
			imageHolder.imageId = eggResourceId;
			imageHolder.imageView = mEggDetailImage;
			imageHolder.imageUrl = url;
			new GetImageTask().execute(imageHolder);
		}

		// Title
		String eggName = egg.eggName;
		if (egg.categoryId.toLowerCase().equals("rare"))
			eggName += " (Rare)";
		mEggDetailTitle.setText(eggName);

		// Description
		String desc = "";
		if (!egg.eggSetName.toLowerCase().equals("none"))
		{
			desc += egg.eggSetName + " Set";
			mEggDetailDesc.setText(desc);
			mEggDetailDesc.setVisibility(View.VISIBLE);
		}
		else
			mEggDetailDesc.setVisibility(View.GONE);

		// Message
		if (egg.basketMessage != null && !egg.basketMessage.equals(""))
		{
			String message = "\"" + egg.basketMessage + "\"";
			mEggDetailMessage.setText(message);
			mEggDetailMessage.setVisibility(View.VISIBLE);
		}
		else
			mEggDetailMessage.setVisibility(View.GONE);

		// Dropper
		Date dateDrop = DateUtils.wcfToDate(egg.basketDropDate);
		String description = "Dropped by\n" + egg.eggDropperName + "\n" + DateFormat.getInstance().format(dateDrop);
		mEggDetailDropperText.setText(description);

		// Get their picture
		String url = FacebookService.GRAPH_BASE_URL + egg.basketDropperId + "/picture?type=large";
		String userId = egg.basketDropperId;

		Bitmap bmDropper = mImageCache.get(egg.basketDropperId);
		if (bmDropper != null)
			mEggDetailDropperImage.setImageBitmap(bmDropper);
		else
		{
			ImageHolder imageHolder = new ImageHolder();
			imageHolder.imageId = userId;
			imageHolder.imageShape = "square";
			imageHolder.imageView = mEggDetailDropperImage;
			imageHolder.imageUrl = url;
			new GetImageTask().execute(imageHolder);
		}

		if (egg.eggFinderId != null)
		{
			if (egg.eggFoundDate != null)
			{
				Date date = DateUtils.wcfToDate(egg.eggFoundDate);
				String finder = "Found on " + DateFormat.getInstance().format(date);
				mEggDetailFinder.setText(finder);
				mEggDetailFinder.setVisibility(View.VISIBLE);
			}
		}
		else
		{
			mEggDetailFinder.setVisibility(View.GONE);
		}
	}

	private void loadGrid()
	{
		if (mListItems != null && mListItems.size() > 0)
		{
			ImageAdapter adapter = new ImageAdapter(EggsMine.this);
			mGridView.setAdapter(adapter);
		}
	}

	class GetImageTask extends AsyncTask<ImageHolder, Void, Bitmap>
	{
		ImageHolder	holder;

		@Override
		protected Bitmap doInBackground(ImageHolder... params)
		{
			// We are on the background thread
			holder = params[0];
			Bitmap bitmap = RippleUI.getImage(holder.imageUrl);
			if (holder.imageShape == "square")
				bitmap = RippleUI.cropToSquare(bitmap);
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap)
		{
			// We are on the UI thread
			super.onPostExecute(bitmap);
			if (bitmap != null)
			{
				holder.imageView.setImageBitmap(bitmap);
				holder.image = bitmap;
				mImageCache.put(holder.imageId, bitmap);
			}
		}
	}

	// ----------------------------------------------------------------------------------
	// Event handlers
	// ----------------------------------------------------------------------------------

	public void onEggMapClick(View view)
	{
		Intent intent = new Intent(this, EggsMap.class);
		startActivity(intent);
	}

	public void onDeleteEggClick(View view)
	{
		startProgress();
		RippleRunner ripple = new RippleRunner();
		Bundle parameters = new Bundle();
		Egg egg = (Egg) view.getTag();

		String method = "DeleteEgg";
		parameters.putString("eggId", egg.eggId);

		ripple.post(method, parameters, QueryFormat.Json, new DeleteEggListener());
	}

	public class DeleteEggListener extends BaseQueryListener
	{
		public void onComplete(final String response)
		{
			// Post the processed result back to the UI thread
			EggsMine.this.runOnUiThread(new Runnable() {
				public void run()
				{
					RippleUI.showToastNotification(EggsMine.this, "Removed from your collection", Toast.LENGTH_SHORT);
					startProgress();
					mEggDetailRow.setVisibility(View.GONE);
					loadData();
				}
			});
		}

		@Override
		public void onIOException(IOException e)
		{
			// TODO Auto-generated method stub
			super.onIOException(e);
			EggsMine.this.runOnUiThread(new Runnable() {
				public void run()
				{
					RippleUI.showToastNotification(EggsMine.this, "Network error", Toast.LENGTH_SHORT);
					stopProgress();
				}
			});

		}

	}

	// ----------------------------------------------------------------------------------
	// List binding
	// ----------------------------------------------------------------------------------

	public class ImageAdapter extends BaseAdapter
	{
		private Context	context;

		public ImageAdapter(Context c) {
			context = c;
		}

		// ---returns the number of images---
		public int getCount()
		{
			return mListItems.size();
		}

		// ---returns the ID of an item---
		public Object getItem(int position)
		{
			return position;
		}

		public long getItemId(int position)
		{
			return position;
		}

		// ---returns an ImageView view---
		public View getView(int position, View convertView, ViewGroup parent)
		{
			Egg egg = (Egg) mListItems.get(position);

			ImageView imageView;
			if (convertView == null)
			{
				imageView = new ImageView(context);
				imageView.setLayoutParams(new GridView.LayoutParams(100, 100));
				imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				imageView.setPadding(4, 4, 4, 4);
				imageView.setBackgroundResource(R.drawable.egg_frame_bg);
			}
			else
				imageView = (ImageView) convertView;
			imageView.setTag(egg);

			String eggResourceId = egg.eggResourceId;
			if (!egg.eggUnwrapped)
				eggResourceId = "egg_wrapped_bow.png";

			Bitmap bitmap = mImageCache.get(eggResourceId);
			if (bitmap != null)
				imageView.setImageBitmap(bitmap);
			else
			{
				String url = Ripple.URL_RIPPLEMEDIA + "images/eggs/" + eggResourceId;
				ImageHolder imageHolder = new ImageHolder();
				imageHolder.imageId = eggResourceId;
				imageHolder.imageView = imageView;
				imageHolder.imageUrl = url;
				new GetImageTask().execute(imageHolder);
			}
			return imageView;
		}
	}

	public void setLayoutAnim_slidedown(ViewGroup panel, Context ctx)
	{

		AnimationSet set = new AnimationSet(true);
		Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				-1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
		animation.setDuration(800);
		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation)
			{
			// TODO Auto-generated method stub
			// MapContacts.this.mapviewgroup.setVisibility(View.VISIBLE);

			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animation animation)
			{

			// TODO Auto-generated method stub

			}
		});
		set.addAnimation(animation);

		LayoutAnimationController controller = new LayoutAnimationController(set, 0.25f);
		panel.setLayoutAnimation(controller);

	}

	public void setLayoutAnim_slideup(ViewGroup panel, Context ctx)
	{

		AnimationSet set = new AnimationSet(true);

		/*
		 * Animation animation = new AlphaAnimation(1.0f, 0.0f); animation.setDuration(200);
		 * set.addAnimation(animation);
		 */

		Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				0.0f, Animation.RELATIVE_TO_SELF, -1.0f);
		animation.setDuration(800);
		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation)
			{
			// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animation animation)
			{
			// MapContacts.this.mapviewgroup.setVisibility(View.INVISIBLE);
			// TODO Auto-generated method stub

			}
		});
		set.addAnimation(animation);

		LayoutAnimationController controller = new LayoutAnimationController(set, 0.25f);
		panel.setLayoutAnimation(controller);

	}
}