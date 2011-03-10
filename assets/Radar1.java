package com.georain.ripple.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.apache.http.client.ClientProtocolException;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import com.facebook.android.BaseRequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.FacebookError;
import com.facebook.android.Facebook.DialogListener;
import com.georain.ripple.controller.EggsMine.GetImageTask;
import com.georain.ripple.controller.RippleActivity.ImageHolder;
import com.georain.ripple.controller.RippleActivity.ViewHolder;
import com.georain.ripple.model.BaseModifyListener;
import com.georain.ripple.model.BaseQueryListener;
import com.georain.ripple.model.Entity;
import com.georain.ripple.model.Query;
import com.georain.ripple.model.RippleRunner;
import com.georain.ripple.model.RippleService;
import com.georain.ripple.model.Stream;
import com.georain.ripple.model.UserFb;
import com.georain.ripple.model.RippleService.GsonType;
import com.georain.ripple.model.RippleService.QueryFormat;
import com.georain.ripple.utilities.DateUtils;

public class Radar1 extends RippleActivity
{
	public static Entity getPointByBssidClient(List<Entity> pointList, String bssid)
	{
		int pointListCount = pointList.size();
		for (int i = 0; i < pointListCount; i++)
			if (pointList.get(i).tagId.equals(bssid.toString()))
				return pointList.get(i);
		return null;
	}

	public static List<Entity> getPointsSelected(List<Entity> pointList)
	{
		List<Entity> pointListSelected = new ArrayList<Entity>();
		for (int i = 0; i < pointList.size(); i++)
			if (pointList.get(i).isSelected)
				pointListSelected.add(pointList.get(i));
		return pointListSelected;
	}

	private int					mPrefLevelCutoff		= -100;
	private Boolean				mPrefAutoscan			= true;
	private int					mPrefAutoscanInterval	= 5000;
	private Boolean				mPrefRippleSpotsOnly	= true;

	private boolean				mFirstRun				= true;
	private List<ScanResult>	mWifiList;
	private ArrayList<Entity>	mPointList				= new ArrayList<Entity>();
	private WifiManager			mWifiManager;
	private boolean				mSpotCountChanged;
	private WifiLock			mWifiLock;
	private Context				mContext;
	private WifiReceiver		mWifiReceiver			= new WifiReceiver();
	private Boolean				mScanRequestActive		= false;
	private Boolean				mScanRequestProcessing	= false;

	protected Button			mUserPicture;
	protected LinearLayout		mUserInfo;
	protected Bitmap			mUserBitmap				= null;
	protected ImageCache		mImageCache;
	protected HashMap			mHookupList				= new HashMap();

	public BaseModifyListener	mListener;
	private TableLayout			mSpotLayout1;
	private TableLayout			mSpotLayout2;
	private TableLayout			mSpotLayout;
	private Boolean				mLayoutLocked			= false;

	private TableLayout			mMenuGroup;
	private boolean				mUserRefusedWifiEnable	= false;
	private Handler				mHandler				= new Handler();
	public Boolean				suspended				= false;
	private Animation			mAnimSpotLayoutIn;
	private Animation			mAnimSpotLayoutOut;
	private AnimationDrawable	mAnimSpotHookedUp;
	private Boolean				mReadyToRun				= false;

	@SuppressWarnings("unused")
	private Runnable			mStartAnimationTask		= new Runnable() {
															public void run()
															{
																mAnimSpotHookedUp.start();
															}
														};

	private Runnable			mWifiScanTask			= new Runnable() {
															public void run()
															{
																wifiScan();
															}
														};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// Start normal processing
		mReadyToRun = false;
		setContentView(R.layout.radar);
		super.onCreate(savedInstanceState);

		// We stash some preference settings
		loadPreferences();

		// We'll use these later when we get called back
		mUserPicture = (Button) findViewById(R.id.User_Picture);
		mUserInfo = (LinearLayout) findViewById(R.id.User_Info);
		mUserPicture.getBackground().mutate().setColorFilter(getResources().getColor(R.color.button_color_filter), PorterDuff.Mode.MULTIPLY);
		Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.icon_placeholder_generic);
		BitmapDrawable bmd = new BitmapDrawable(bm);
		bmd.setBounds(0, 10, 100, 110);
		mUserPicture.setCompoundDrawablePadding(0);
		mUserPicture.setCompoundDrawables(null, bmd, null, null);

		// Image cache
		mImageCache = new ImageCache(getApplicationContext(), 100, 16);

		// If we don't have a current user object, we create one.
		if (getCurrentUser() == null)
		{
			if (FacebookService.facebookRunner == null)
			{
				Intent intent = new Intent(getApplicationContext(), Dashboard.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
				startActivity(intent);
				return;
			}

			Utilities.Log("Ripple", "Radar: starting facebook graph request for user");
			FacebookService.facebookRunner.request("me", new UserRequestListener());
		}
		else
			showUserInfo();

		// Stashing
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		// Ui Hookup
		mSpotLayout = mSpotLayout1;
		mSpotLayout1 = (TableLayout) findViewById(R.id.SpotLayout1);
		mSpotLayout2 = (TableLayout) findViewById(R.id.SpotLayout2);

		mMenuGroup = (TableLayout) findViewById(R.id.MenuGroup);

		mAnimSpotLayoutIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_normal);
		mAnimSpotLayoutOut = AnimationUtils.loadAnimation(this, R.anim.fade_out_normal);
		mAnimSpotLayoutIn.setFillAfter(true);
		mAnimSpotLayoutOut.setFillAfter(true);
		mAnimSpotLayoutIn.setFillBefore(false);
		mAnimSpotLayoutOut.setFillBefore(false);
		mAnimSpotLayoutIn.setFillEnabled(true);
		mAnimSpotLayoutOut.setFillEnabled(true);

		mReadyToRun = true;
	}

	/* 
	 * ==========================================================================================
	 * USER routines
	 * ==========================================================================================
	 */
	
	public class UserRequestListener extends BaseRequestListener
	{
		public void onComplete(final String response)
		{
			// Process the response here: executed in background thread
			Utilities.Log("Ripple", "Radar: returning facebook graph request for user");
			setCurrentUser(RippleService.getGson(GsonType.Internal).fromJson(response, UserFb.class));

			// Once we have a current user, we launch our first wifi scan (which leads to data requests)
			// Other calls to start wifi scans are in Resume, manual refresh, and autoscan (setup at
			// the end of processing a previous wifi scan.

			// Turn on our receiver that listens for wifi scan results
			registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			mHandler.removeCallbacks(mWifiScanTask);
			mHandler.post(mWifiScanTask);

			// Update the user with the most current facebook token
			getCurrentUser().token = FacebookService.facebookRunner.facebook.getAccessToken();
			getCurrentUser().tokenDate = DateUtils.nowString();

			// Make sure they are in the Ripple service
			RippleRunner rippleRunner = new RippleRunner();
			Query query = new Query("Users").filter("Id eq '" + getCurrentUser().id + "'");
			Utilities.Log("Ripple", "Radar: starting ripple query to see if a facebook user '" + getCurrentUser().id + "' already exists in ripple");
			rippleRunner.select(query, UserFb.class, new UserQueryListener());
		}
	}

	public class UserQueryListener extends BaseQueryListener
	{
		public void onComplete(String response)
		{
			ArrayList<Object> users = RippleService.convertJsonToObjects(response, UserFb.class);

			// We need to insert if we don't have them yet
			RippleRunner rippleRunner = new RippleRunner();
			if (users == null || users.size() == 0)
			{
				Utilities.Log("Ripple", "Radar: starting ripple insert for '" + getCurrentUser().id + "'");
				rippleRunner.insert(getCurrentUser(), "Users", new UserReadyListener());
			}
			else
			{
				Utilities.Log("Ripple", "Radar: starting ripple update for '" + getCurrentUser().id + "'");
				rippleRunner.update(getCurrentUser(), getCurrentUser().getUriOdata(), new UserReadyListener());
			}
		}
	}

	public class UserReadyListener extends BaseModifyListener
	{
		public void onComplete()
		{
			// Post the processed result back to the UI thread
			Utilities.Log("Ripple", "Radar: user '" + getCurrentUser().id + "' has been inserted or updated");
			Radar1.this.runOnUiThread(new Runnable() {
				public void run()
				{
					showUserInfo();
				}
			});
		}
	}

	public void showUserInfo()
	{
		// Get their picture
		String userId = getCurrentUser().id;
		String imageFormat = "large";

		mUserPicture.setText(getCurrentUser().name);
		Bitmap bitmap = mImageCache.get(getCurrentUser().id);
		if (bitmap != null)
		{
			Utilities.Log("Ripple", "Radar: cache hit for image '" + userId + "'");
			getCurrentUser().picture_bitmap = bitmap;
			showUserPicture();
		}
		else
		{
			new GetFacebookImageTask().execute(userId, imageFormat); // Will set the picture when finished
		}
	}

	public void showUserPicture()
	{
		BitmapDrawable bm = new BitmapDrawable(getCurrentUser().picture_bitmap);
		bm.setBounds(0, 10, 100, 110);
		mUserPicture.setCompoundDrawablePadding(0);
		mUserPicture.setCompoundDrawables(null, bm, null, null);
		Animation animation = AnimationUtils.loadAnimation(Radar1.this, R.anim.fade_in_normal);
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		mUserPicture.startAnimation(animation);
	}

	class GetFacebookImageTask extends AsyncTask<String, Void, Bitmap>
	{
		String	userId;

		@Override
		protected Bitmap doInBackground(String... params)
		{
			// We are on the background thread
			userId = params[0];
			Utilities.Log("Ripple", "Radar: starting AsyncTask to get image (from cache or service) for " + userId);
			Bitmap bitmap = null;
			bitmap = FacebookService.getFacebookPicture(params[0], params[1]);
			bitmap = RippleUI.cropToSquare(bitmap);
			mImageCache.put(params[0], bitmap);
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap)
		{
			// We are on the UI thread
			super.onPostExecute(bitmap);
			Utilities.Log("Ripple", "Radar: returning AsyncTask to get image (from cache or service) for " + userId);
			getCurrentUser().picture_bitmap = bitmap;
			showUserPicture();
		}
	}

	/* 
	 * ==========================================================================================
	 * EVENT routines
	 * ==========================================================================================
	 */
	
	public void onUserClick(View view)
	{
		Intent intent = new Intent(this, Dashboard.class);
		startActivity(intent);
	}

	public void onStreamButtonClick(View view)
	{
		Stream stream = (Stream) view.getTag();
		String fullyQualifiedClass = "com.georain.ripple.controller." + stream.streamClass;
		String streamName = stream.streamName.toLowerCase();

		if (stream.streamClass.toLowerCase().equals("list"))
		{
			RippleUI.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
			return;
		}

		if (streamName.equals("share"))
		{
			postToWall(Radar1.this, "Hooked up at " + getCurrentEntity().label + " using Ripple");
			return;
		}

		try
		{
			Class clazz = Class.forName(fullyQualifiedClass, false, this.getClass().getClassLoader());

			Intent intent = new Intent(this, clazz);
			String jsonStream = RippleService.getGson(GsonType.Internal).toJson(stream);
			intent.putExtra("stream", jsonStream);

			if (streamName.toLowerCase().equals("friends"))
				intent.putExtra("FriendsFilter", "FriendsByPoint");
			else if (streamName.toLowerCase().equals("eggs"))
			{
				if (stream.itemCount == 0)
					intent.putExtra("TabId", "eggsdrop");
				else
					intent.putExtra("TabId", "egghunt");
			}

			startActivity(intent);
		}
		catch (ClassNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void onRefreshClick(View view)
	{
		// For this activity, refresh means rescan and reload point data from the service
		wifiScan();
	}

	public void onRippleSpotClick(View view)
	{
		mLayoutLocked = true;
		TextView pointView = (TextView) view;
		boolean isPrevSelection = false;
		Entity point = (Entity) view.getTag();

		if (pointView.getLineCount() > 3)
			pointView.setEllipsize(TruncateAt.END);

		// Unselect the point if it is seleced. We will be left without a selection
		if (point.isSelected)
		{
			// Switch back to the base image resource
			Drawable bg = getResources().getDrawable(R.drawable.icon_point_bg_shadow).mutate();
			pointView.setBackgroundDrawable(bg);

			// If it is a ripple point, we restore the appropriate color filter
			if (point.isTagged)
			{
				pointView.getBackground().mutate().setColorFilter(null);
				pointView.getBackground().mutate().setColorFilter(getResources().getColor(R.color.point_ripple_filter), PorterDuff.Mode.MULTIPLY);
				pointView.invalidate();
				pointView.invalidateDrawable(pointView.getBackground());
			}
			pointView.setSelected(false);
			point.isSelected = false;
			setCurrentEntity(null);
			hideMenuGroup();
		}
		else
		{
			// Clear all current selections in our internal point list
			for (int i = 0; i < mPointList.size(); i++)
				if (mPointList.get(i).isSelected)
				{
					mPointList.get(i).isSelected = false;
					isPrevSelection = true;
				}

			// Clear selection from point views. When a point is not selected, we have to change the background
			// resource. mSpotLayout could change while we are processing because a wifi scan could have returned.
			int countRows = mSpotLayout.getChildCount();
			for (int iRow = 0; iRow < countRows; iRow++)
			{
				TableRow tr = (TableRow) mSpotLayout.getChildAt(iRow);
				int countPointsRow = tr.getChildCount();
				for (int iPoint = 0; iPoint < countPointsRow; iPoint++)
				{
					FrameLayout viewPointContainer = (FrameLayout) tr.getChildAt(iPoint);
					TextView viewPointRow = (TextView) viewPointContainer.getChildAt(0);
					Entity pointRow = (Entity) viewPointRow.getTag();
					viewPointRow.setSelected(false);
					Drawable bg = getResources().getDrawable(R.drawable.icon_point_bg_shadow).mutate();
					viewPointRow.setBackgroundDrawable(bg);

					if (pointRow.isTagged)
					{
						Utilities.Log("Ripple", "PointClick: " + pointRow.label + "is a ripple point");
						viewPointRow.getBackground().mutate().setColorFilter(null);
						viewPointRow.getBackground().mutate().setColorFilter(getResources().getColor(R.color.point_ripple_filter),
								PorterDuff.Mode.MULTIPLY);
						viewPointRow.invalidate();
						viewPointRow.invalidateDrawable(viewPointRow.getBackground());
					}
				}
			}

			pointView.setSelected(true);
			point.isSelected = true;

			// The selected point uses a special color filter
			// Drawable bg = getResources().getDrawable(R.drawable.icon_point_bg_selected2).mutate();
			// pointView.setBackgroundDrawable(bg);

			Drawable bgDrawable = pointView.getBackground().mutate();
			if (bgDrawable != null)
			{
				bgDrawable.setColorFilter(null);
				int colorFilter = getResources().getColor(R.color.point_ripple_filter_selected);
				bgDrawable.setColorFilter(colorFilter, PorterDuff.Mode.MULTIPLY);
			}

			// Set the title
			setCurrentEntity(point);
			showMenuGroup(point, isPrevSelection);

			// Last thing to do (uses a different thread)
			if (getCurrentEntity().isTagged)
			{
				// Check to see if the user has already hooked up in the last minute
				Date lastDate = (Date) mHookupList.get(getCurrentEntity().entityId);
				int secondsAgo = 0;
				if (lastDate != null)
					secondsAgo = DateUtils.intervalInSeconds(lastDate, DateUtils.nowDate());

				mHookupList.put(getCurrentEntity().entityId, DateUtils.nowDate());

				if (lastDate == null || secondsAgo > 60)
				{
					RippleRunner ripple = new RippleRunner();
					Bundle parameters = new Bundle();

					parameters.putString("userFacebookId", getCurrentUser().id);
					String method = "InsertHookup";
					parameters.putString("entityId", getCurrentEntity().entityId);
					parameters.putString("userId", getCurrentUser().id);
					parameters.putString("hookupDate", DateUtils.nowString());

					ripple.post(method, parameters, QueryFormat.Json, null);
				}
			}
		}
		mLayoutLocked = false;
	}

	public void postToWall(Context context, String message)
	{
		Bundle parameters = new Bundle();
		parameters.putString("message", message);
		FacebookService.facebookRunner.facebook.dialog(context, "stream.publish", parameters, new WallPostDialogListener());
	}

	/* 
	 * ==========================================================================================
	 * POINT routines
	 * ==========================================================================================
	 */

	// 1: A wifi scan has been completed (Main). Process trigger

	public class WifiReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(final Context context, Intent intent)
		{
			// We might be getting called back but the user has signed out so there is no
			// current user. In that case, we should just exit.
			if (getCurrentUser() == null)
				return;

			// We only process scan results we have requested
			if (mScanRequestActive && !mScanRequestProcessing)
			{
				mScanRequestActive = false;
				mScanRequestProcessing = true;

				// Get the latest scan results
				mWifiList = mWifiManager.getScanResults();
				Utilities.Log("Ripple", "Radar: received: " + intent.getAction().toString());
				Utilities.Log("Ripple", "Radar: starting AsyncTask to rebuild points (includes calls to ripple service)");
				RebuildPointsTask task = new RebuildPointsTask();
				task.execute(mPointList, mWifiList);
			}
		}
	}

	// 2: Update the points we are tracking based on the new scan results (AsyncTask)

	class RebuildPointsTask extends AsyncTask<Object, Void, ArrayList<Entity>>
	{
		IOException	exception	= null;

		@Override
		protected ArrayList<Entity> doInBackground(Object... params)
		{
			// We are on the background thread
			ArrayList<Entity> pointListNew = null;
			try
			{
				pointListNew = lookupPoints((List<Entity>) params[0], (List<ScanResult>) params[1]);
			}
			catch (IOException e)
			{
				exception = e;
				e.printStackTrace();
			}
			return pointListNew;
		}

		@Override
		protected void onPostExecute(ArrayList<Entity> pointListNew)
		{
			// We are on the UI thread
			super.onPostExecute(pointListNew);
			if (exception != null)
			{
				RippleUI.showToastNotification(Radar1.this, "Network error", Toast.LENGTH_SHORT);
				stopProgress();
				return;
			}
			Utilities.Log("Ripple", "Radar: returning AsyncTask to rebuild points");
			updatePointCollection(pointListNew);

			// Let the radar ui know that there are fresh results to process
			try
			{
				// Rebuild our UI as needed to reflect the new state
				constructUi();

				// If the current point is flagged as dirty then rebuild the stream menus
				if (getCurrentEntity() != null && getCurrentEntity().isDirty && getCurrentEntity().isSelected)
				{
					showMenuGroup(getCurrentEntity(), true);
					getCurrentEntity().isDirty = false;
				}

				// Scanning is complete so change the heading back to normal
				stopProgress();

				// Schedule the next wifi scan run
				mScanRequestProcessing = false;
				if (mPrefAutoscan)
				{
					mHandler.removeCallbacks(mWifiScanTask);
					mHandler.postDelayed(mWifiScanTask, mPrefAutoscanInterval);
				}
			}
			catch (Exception e)
			{
				RippleUI.showToastNotification(mContext, "Radar callback failed.", Toast.LENGTH_SHORT);
			}
		}
	}

	// 2A: Lookup points in service (Async)

	public ArrayList<Entity> lookupPoints(List<Entity> pointList, List<ScanResult> scanList) throws IOException
	{
		ArrayList<Entity> pointListNew = new ArrayList<Entity>();

		// Filter out the ripple spots
		for (int i = 0; i < scanList.size(); i++)
		{
			ScanResult scanResult = scanList.get(i);
			String bssid = scanResult.BSSID;
			String label = scanResult.SSID;
			String ssid = scanResult.SSID;
			int levelDb = scanResult.level;

			// Check to see if we already have this point
			Entity pointExisting = null;
			if (pointList != null)
				pointExisting = getPointByBssidClient(pointList, bssid);

			if (pointExisting != null && !pointExisting.isDirty)
			{
				pointExisting.addScanPass(levelDb);
				pointListNew.add(pointExisting);
			}
			else
			{
				// Point might already exist but have been updated in the service.
				// Check with service to see if this point has been ripplized.
				// If call comes back null then there was a network or service problem.
				// The user got a toast notification from the service and we will still display
				// the point as raw wifi only.
				@SuppressWarnings("unused")
				Query query = new Query("Points").filter("Bssid eq '" + bssid + "'");

				// We are making synchronous calls inside an asynchronous thread
				RippleService ripple = new RippleService();

				String response;
				try
				{
					// New way to get point data
					Bundle parameters = new Bundle();
					parameters.putString("userId", getCurrentUser().id);
					parameters.putString("bssid", bssid);
					response = ripple.post("GetPoint", parameters, QueryFormat.Json);

					// Old way: response = ripple.select(query, Point.class);
					ArrayList<Object> points = RippleService.convertJsonToObjects(response, Entity.class);

					Entity point = null;
					if ((points != null) && (points.size() != 0))
						point = (Entity) points.get(0);

					if (point != null)
					{
						point.isTagged = true;
						point.isServiceVerified = true;
						point.levelDb = levelDb;
					}
					else
					{
						// Not a ripple point
						point = new Entity(label, bssid, ssid, levelDb);
						if (points != null)
							point.isServiceVerified = true;
						else
							point.isServiceVerified = false;

						// Add a stream to expose the ripple it command
						Stream stream = new Stream();
						stream.streamName = "PointEdit";
						stream.streamLabel = "Ripple It";
						stream.streamClass = "PointEditor";
						stream.layoutTemplate = "point_editor";
						stream.showHeader = true;
						stream.showFooter = false;
						stream.headerTitle = "Edit Point";
						stream.headerIconResource = "spot_orange";
						stream.itemCount = 0;
						stream.includeByDefault = true;
						point.streams = new ArrayList<Stream>();
						point.streams.add(stream);
					}

					point.addScanPass(levelDb);

					if (pointExisting != null && pointExisting.isDirty)
						if (pointExisting.isSelected)
						{
							point.isSelected = true;
							point.isDirty = true;
						}

					pointListNew.add(point);
				}
				catch (ClientProtocolException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
					throw e;
				}
				catch (URISyntaxException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		// We need to move over any points in the previous pointList that were not in the current scanList
		for (Entity point : pointList)
			if (getPointByBssidClient(pointListNew, point.tagId) == null)
			{
				point.scanMisses++;
				if (point.scanMisses < 3)
					pointListNew.add(point);
			}

		// Replace the core point list
		return pointListNew;
	}

	// 2B: Update to radar point collection (Main)

	public void updatePointCollection(ArrayList<Entity> pointListNew)
	{
		/**
		 * This is called whenever we get the results of a new wifi scan. We need to merge any changes together with our
		 * existing collection of points to reflect the current state of affairs.
		 */

		// Do the work to update the collection of points. A network issue will be flagged in the service and we
		// still get back a raw collection of wifi points.
		int pointListPrevCount = mPointList.size();
		if (mPrefRippleSpotsOnly)
		{
			int pointCount = 0;
			for (Entity point : mPointList)
				if (point.isTagged)
					pointCount++;
			pointListPrevCount = pointCount;
		}
		if (pointListNew == null)
			return;

		mPointList.clear();

		// Always add any point that is currently selected
		if (getCurrentEntity() != null)
			mPointList.add(getCurrentEntity());

		for (Entity point : pointListNew)
		{
			if (getCurrentEntity() != null && getCurrentEntity().tagId.equals(point.tagId))
				continue;
			mPointList.add(point);
		}

		// Do we have a point that is currently selected? It's only one now but might be multiple in the future
		// List<Point> pointSelectedList = RadarController.getPointsSelected(mPointList);
		// if (pointSelectedList.size() != 0)
		// setCurrentPoint(pointSelectedList.get(0));
		// else
		// setCurrentPoint(null);

		// For UI routines, we flag whether the number of points has changed
		int pointListCurrentCount = mPointList.size();
		if (mPrefRippleSpotsOnly)
		{
			int pointCount = 0;
			for (Entity point : mPointList)
				if (point.isTagged && point.getAvgPointLevel() < point.signalThreshold)
					pointCount++;
			pointListCurrentCount = pointCount;
		}
		if (pointListCurrentCount != pointListPrevCount)
			mSpotCountChanged = true;
		else
			mSpotCountChanged = false;

		// Sort the point collection by point type and strength of signal
		Collections.sort(mPointList, new SortBySpotLevelDb());
	}

	/* 
	 * ==========================================================================================
	 * RADAR Routines
	 * ==========================================================================================
	 */

	// Construct point layout and draw (Main)

	private void constructUi()
	{
		// First check to see if we have any wifi spots
		// TODO: Display something to the user that no spots (wifi or ripple)
		// were found.
		if (mPointList.size() == 0)
			return;

		// Populate the layout with latest points
		BuildPointLayoutTask task = new BuildPointLayoutTask();
		task.execute(mPointList, PreferenceManager.getDefaultSharedPreferences(getApplicationContext()), getApplicationContext());
	}
	
	// Construct point layout and draw (AsyncTask)

	class BuildPointLayoutTask extends AsyncTask<Object, Void, ArrayList<TableRow>>
	{
		@Override
		protected ArrayList<TableRow> doInBackground(Object... params)
		{
			// We are on the background thread
			ArrayList<TableRow> tableRows = constructPointLayout((ArrayList<Entity>) params[0], (SharedPreferences) params[1], (Context) params[2]);
			return tableRows;
		}

		@Override
		protected void onPostExecute(ArrayList<TableRow> tableRows)
		{
			// We are on the UI thread
			super.onPostExecute(tableRows);
			Utilities.Log("Ripple", "Radar: returning AsyncTask to build point table rows");
			drawUi(tableRows);
		}
	}

	// 1: Build an array of table rows used by the main draw routine to display points (Async)

	public ArrayList<TableRow> constructPointLayout(ArrayList<Entity> pointList, SharedPreferences prefs, Context context)
	{
		Boolean prefRippleSpotsOnly = prefs.getBoolean(Preferences.PREF_RIPPLE_SPOTS_ONLY, false);
		String prefDisplayExtras = prefs.getString(Preferences.PREF_DISPLAY_EXTRAS, "none");
		Integer prefLevelCutoff = Integer.parseInt(prefs.getString(Preferences.PREF_LEVEL_CUTOFF, "-100"));
		ArrayList<TableRow> tableRows = new ArrayList<TableRow>();

		// Non UI: Count the number of points to display
		int pointCount = 0;
		for (int i = 0; i < pointList.size(); i++)
		{
			Entity point = pointList.get(i);

			if (prefRippleSpotsOnly && !point.isTagged)
				continue;

			if (point.isTagged && point.getAvgPointLevel() < point.signalThreshold)
				continue;

			int avgPointLevel = point.getAvgPointLevel();
			if (avgPointLevel < mPrefLevelCutoff)
				continue;

			pointCount++;
		}

		if (pointCount > 9)
			pointCount = 9;

		// UI: Get our first row to hold points
		int resId = R.layout.temp_tablerowspots;
		if (pointCount == 3)
			resId = R.layout.temp_tablerowspotstight;
		else if (pointCount == 5)
			resId = R.layout.temp_tablerowspotstight;
		else if (pointCount == 7)
			resId = R.layout.temp_tablerowspotstight;
		else if (pointCount == 8)
			resId = R.layout.temp_tablerowspotstight;

		LayoutInflater inflater = getLayoutInflater();

		TableRow tableRow = (TableRow) inflater.inflate(resId, null);
		TableRow.LayoutParams rowLp = new TableRow.LayoutParams();
		rowLp.setMargins(2, 0, 2, 0);

		if (pointCount == 0)
		{
			TextView message = (TextView) inflater.inflate(R.layout.temp_radar_message, null);
			message.setText("No spots\nclose by");
			tableRow.addView(message, rowLp);
			tableRows.add(tableRow);
			return tableRows;
		}

		// UI: Add points to rows
		for (int i = 0; i < pointCount; i++)
		{
			Entity point = pointList.get(i);

			if (prefRippleSpotsOnly && !point.isTagged)
				continue;

			if (point.isTagged && point.getAvgPointLevel() < point.signalThreshold)
				continue;

			int avgZoneLevel = point.getAvgPointLevel();
			if (avgZoneLevel < prefLevelCutoff)
				continue;

			// UI: Inflate a point

			FrameLayout pointContainer = (FrameLayout) inflater.inflate(R.layout.temp_point_image, null);
			ImageView pointView = (ImageView) pointContainer.findViewById(R.id.Spot);

//			if (prefDisplayExtras.equals("none"))
//				pointView.setText(point.label);
//			else if (prefDisplayExtras.equals("level"))
//				pointView.setText(point.label + " " + String.valueOf(point.getAvgPointLevel()));
//			else if (prefDisplayExtras.equals("bssid"))
//				pointView.setText(point.label + " " + String.valueOf(point.bssid));

			pointView.setTag(point);
			tableRow.addView(pointContainer, rowLp);

			if (point.isSelected)
			{
				pointView.setSelected(true);

				// The selected point uses a special color filter
				// pointView.getBackground().mutate().setColorFilter(null);
				// pointView.setBackgroundDrawable(null);
				// Drawable bg = getResources().getDrawable(R.drawable.icon_point_bg_selected2).mutate();
				// pointView.setBackgroundDrawable(bg);

				Drawable bgDrawable = pointView.getBackground().mutate();
				if (bgDrawable != null)
				{
					bgDrawable.setColorFilter(null);
					int colorFilter = getResources().getColor(R.color.point_ripple_filter_selected);
					bgDrawable.setColorFilter(colorFilter, PorterDuff.Mode.MULTIPLY);
				}
			}
			else
			{
				if (point.isTagged)
				{
					Drawable bgDrawable = pointView.getBackground().mutate();
					if (bgDrawable != null)
					{
						bgDrawable.setColorFilter(null);
						int colorFilter = getResources().getColor(R.color.point_ripple_filter);
						bgDrawable.setColorFilter(colorFilter, PorterDuff.Mode.MULTIPLY);
					}
				}
			}
			
			// We have an image resource
			
			if (point.pointResourceId != null && !point.pointResourceId.equals(""))
			{
				String url = "http://dev.georain.com/images/" + point.pointResourceId;
				ImageHolder imageHolder = new ImageHolder();
				imageHolder.imageId = point.pointResourceId;
				imageHolder.imageView = pointView;
				imageHolder.imageUrl = url;
				new GetImageTask().execute(imageHolder);
			}

			Drawable bgDrawable = pointView.getBackground().mutate();
			if (bgDrawable != null)
				bgDrawable.setAlpha((int) (127 + (128 * point.getLevelPcnt())));

			// Show stream info
			TextView badgeView = (TextView) pointContainer.findViewById(R.id.Badge);
			TextView eggView = (TextView) pointContainer.findViewById(R.id.Eggs);
			if (point.isTagged)
			{
				int totalItemCount = 0;
				Boolean hasEggs = false;
				for (Stream stream : point.streams)
				{
					if (stream.streamName.toLowerCase().equals("eggs"))
					{
						if (stream.itemCount != 0)
							hasEggs = true;
					}
					else
						totalItemCount += stream.itemCount;
				}
				if (totalItemCount > 0)
					badgeView.setText(String.valueOf(totalItemCount));
				else
					badgeView.setVisibility(View.INVISIBLE);

				if (!hasEggs)
					eggView.setVisibility(View.INVISIBLE);
			}
			else
			{
				badgeView.setVisibility(View.INVISIBLE);
				eggView.setVisibility(View.INVISIBLE);
			}

			Boolean newTableRow = false;

			if ((pointCount == 3) && (i == 0))
				newTableRow = true;
			else if ((pointCount == 4) && (i == 1))
				newTableRow = true;
			else if ((pointCount == 5) && (i == 1))
				newTableRow = true;
			else if ((pointCount == 6) && (i == 2))
				newTableRow = true;
			else if ((pointCount == 7) && ((i == 1) || (i == 4)))
				newTableRow = true;
			else if ((pointCount == 8) && ((i == 1) || (i == 4)))
				newTableRow = true;
			else if ((pointCount >= 9) && ((i == 2) || (i == 5)))
				newTableRow = true;

			if (newTableRow)
			{
				if ((pointCount == 7) && (i == 4))
					resId = R.layout.temp_tablerowspotstight;

				tableRows.add(tableRow);
				tableRow = (TableRow) inflater.inflate(resId, null);
				resId = R.layout.temp_tablerowspots;
			}
			if (i >= 9)
				break;
		}
		tableRows.add(tableRow);
		return tableRows;
	}

	// 2: Draw the points (Main)

	private void drawUi(ArrayList<TableRow> tableRows)
	{
		if (mLayoutLocked && !mFirstRun)
		{
			Utilities.Log("Ripple", "Radar: layout locked to skipping drawUi()");
			return;
		}

		if (mFirstRun)
		{
			mSpotLayout = mSpotLayout1;
			mSpotLayout.removeAllViews();
		}
		else if (mSpotCountChanged)
		{
			if (mSpotLayout == mSpotLayout1)
			{
				mSpotLayout = mSpotLayout2;
				mSpotLayout.removeAllViews();
			}
			else if (mSpotLayout == mSpotLayout2)
			{
				mSpotLayout = mSpotLayout1;
				mSpotLayout.removeAllViews();
			}
		}
		else
		{
			mSpotLayout.removeAllViews();
		}

		for (int i = 0; i < tableRows.size(); i++)
		{
			TableLayout.LayoutParams tableLp = new TableLayout.LayoutParams();
			tableLp.setMargins(0, 3, 0, 3);
			if (i + 1 < tableRows.size())
			{
				if (tableRows.get(i).getChildCount() != tableRows.get(i + 1).getChildCount()) // tight cases
					tableLp.setMargins(0, 0, 0, -12);
			}
			mSpotLayout.addView(tableRows.get(i), tableLp);
		}
		mSpotLayout.requestLayout();

		// Fade out the old points and fade in the new
		mSpotLayout.bringToFront();
		if (mFirstRun)
		{
			mSpotLayout2.startAnimation(mAnimSpotLayoutOut);
			mSpotLayout1.startAnimation(mAnimSpotLayoutIn);
			mFirstRun = false;
		}
		else if (mSpotCountChanged)
		{
			if (mSpotLayout == mSpotLayout1)
			{
				mSpotLayout2.startAnimation(mAnimSpotLayoutOut);
				mSpotLayout1.startAnimation(mAnimSpotLayoutIn);
			}
			else if (mSpotLayout == mSpotLayout2)
			{
				mSpotLayout1.startAnimation(mAnimSpotLayoutOut);
				mSpotLayout2.startAnimation(mAnimSpotLayoutIn);
			}
		}
	}

	/* 
	 * ==========================================================================================
	 * MENU routines
	 * ==========================================================================================
	 */

	private void hideMenuGroup()
	{
		// Fade out current menu group
		Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
		mMenuGroup.startAnimation(animation);
		mMenuGroup.setVisibility(View.INVISIBLE);

		// Bring back the user ui
		mUserInfo.setClickable(true);
		Animation animationIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
		mUserInfo.startAnimation(animationIn);
	}

	private void showMenuGroup(Entity rippleZone, boolean hideFirst)
	{
		if (mMenuGroup.getVisibility() != View.VISIBLE)
			mMenuGroup.setVisibility(View.VISIBLE);

		if (!hideFirst)
		{
			// Dim the user ui
			mUserInfo.setClickable(false);
			Animation animationOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
			animationOut.setFillAfter(true);
			mUserInfo.startAnimation(animationOut);

			// Construct the menu UI
			configureMenus(getCurrentEntity());

			TextView menuTitle = (TextView) findViewById(R.id.Menu_Title);
			if (menuTitle != null)
				menuTitle.setText(getCurrentEntity().label);
			Animation animationIn = AnimationUtils.loadAnimation(Radar1.this, R.anim.fade_in);
			mMenuGroup.startAnimation(animationIn);
		}
		else
		{
			// Fade out current menu group
			Animation animationOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
			animationOut.setAnimationListener(new AnimationListener() {
				public void onAnimationEnd(Animation animation)
				{
					// Construct the menu UI
					configureMenus(getCurrentEntity());

					TextView menuTitle = (TextView) findViewById(R.id.Menu_Title);
					if (menuTitle != null)
						menuTitle.setText(getCurrentEntity().label);

					Animation animationIn = AnimationUtils.loadAnimation(Radar1.this, R.anim.fade_in);
					mMenuGroup.startAnimation(animationIn);
				}

				public void onAnimationRepeat(Animation animation)
				{
					return;
				}

				public void onAnimationStart(Animation animation)
				{
					return;
				}
			});
			mMenuGroup.startAnimation(animationOut);
		}
	}

	private void configureMenus(Entity point)
	{
		Boolean needMoreButton = false;
		if (point.streams.size() > 6)
			needMoreButton = true;

		// Get the table we use for grouping and clear it
		TableLayout table = (TableLayout) findViewById(R.id.MenuGroup);
		if (table.getChildCount() > 1)
			table.removeViews(1, table.getChildCount() - 1);

		// Make the first row
		TableRow tableRow = (TableRow) this.getLayoutInflater().inflate(R.layout.temp_tablerow_streams, null);
		TableRow.LayoutParams rowLp = new TableRow.LayoutParams();
		rowLp.setMargins(0, 0, 0, 0);
		TableLayout.LayoutParams tableLp;

		// Loop the streams
		Integer streamCount = 0;
		for (Stream stream : point.streams)
		{
			// Make a button and configure it
			RelativeLayout streamButtonContainer = null;
			if (!stream.streamName.toLowerCase().equals("facebook"))
				streamButtonContainer = (RelativeLayout) this.getLayoutInflater().inflate(R.layout.temp_button_stream, null);
			else
				streamButtonContainer = (RelativeLayout) this.getLayoutInflater().inflate(R.layout.temp_button_stream_facebook, null);

			TextView streamButton = (TextView) streamButtonContainer.findViewById(R.id.StreamButton);
			TextView streamBadge = (TextView) streamButtonContainer.findViewById(R.id.StreamBadge);
			streamButtonContainer.setTag(stream);
			if (needMoreButton && streamCount == 5)
			{
				streamButton.setText("More...");
				streamButton.setTag(stream);
			}
			else
			{
				streamButton.setText(stream.streamLabel);
				streamButton.setTag(stream);
				if (stream.streamName.toLowerCase().equals("facebook"))
				{
					// BitmapDrawable bm = new BitmapDrawable(getCurrentUser().picture_bitmap);
					// bm.setBounds(0, 10, 100, 110);
					// mUserPicture.setCompoundDrawablePadding(0);
					// mUserPicture.setCompoundDrawables(null, bm, null, null);
					// Animation animation = AnimationUtils.loadAnimation(Radar.this, R.anim.fade_in_normal);
					// animation.setFillEnabled(true);
					// animation.setFillAfter(true);
					// mUserPicture.startAnimation(animation);

					streamButton.setText("acebook");
					// streamButton.setBackgroundResource(R.drawable.button_facebook);
				}
				if (stream.itemCount > 0)
				{
					if (stream.streamName.toLowerCase().equals("eggs"))
						streamBadge.setVisibility(View.INVISIBLE);
					else
					{
						streamBadge.setText(String.valueOf(stream.itemCount));
						streamBadge.setTag(stream);
					}
				}
				else
					streamBadge.setVisibility(View.INVISIBLE);
			}
			// if (!stream.streamName.toLowerCase().equals("facebook"))
			// {
			streamButton.getBackground().mutate().setColorFilter(getResources().getColor(R.color.button_color_filter), PorterDuff.Mode.MULTIPLY);
			// }

			// Add button to row
			tableRow.addView(streamButtonContainer, rowLp);
			streamCount++;

			// If we have three in a row then commit it and make a new row
			if (streamCount == 3)
			{
				tableLp = new TableLayout.LayoutParams();
				tableLp.setMargins(0, 0, 0, 0);
				table.addView(tableRow, tableLp);
				tableRow = (TableRow) this.getLayoutInflater().inflate(R.layout.temp_tablerow_streams, null);
			}
			else if (streamCount == 6)
				break;
		}

		// We might have an uncommited row with stream buttons in it
		if (streamCount != 3)
		{
			tableLp = new TableLayout.LayoutParams();
			tableLp.setMargins(0, 3, 0, 3);
			table.addView(tableRow, tableLp);
		}
	}

	/* 
	 * ==========================================================================================
	 * WIFI routines
	 * ==========================================================================================
	 */
	
	private void wifiAskToEnable()
	{
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						mUserRefusedWifiEnable = false;
						wifiEnable();
						break;
					case DialogInterface.BUTTON_NEGATIVE:
						mUserRefusedWifiEnable = true;
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setIcon(R.drawable.icon_alert_dialog).setMessage(R.string.alert_dialog_wifidisabled).setPositiveButton(R.string.alert_dialog_yes,
				dialogClickListener).setNegativeButton(R.string.alert_dialog_no, dialogClickListener).show();
	}

	private void wifiEnable()
	{
		mWifiManager.setWifiEnabled(true);
		RippleUI.showToastNotification(this, "Wifi enabling...", Toast.LENGTH_LONG);
	}

	private void wifiLockAcquire(int lockType)
	{
		if (mWifiLock == null)
		{
			mWifiLock = mWifiManager.createWifiLock(lockType, "Ripple");
			mWifiLock.setReferenceCounted(false);
		}
		if (!mWifiLock.isHeld())
			mWifiLock.acquire();
	}

	private void wifiReleaseLock()
	{
		if (mWifiLock.isHeld())
			mWifiLock.release();
	}

	private void wifiScan()
	{
		if (mWifiManager.isWifiEnabled())
		{
			mScanRequestActive = true;
			startProgress();

			// Show search message if there aren't any current points
			TextView message = (TextView) findViewById(R.id.Radar_Message);
			if (message != null)
				message.setText("Searching for\nspots...");

			wifiLockAcquire(WifiManager.WIFI_MODE_FULL);
			mWifiList = mWifiManager.getScanResults();
			Utilities.Log("Ripple", "Radar: requesting wifi scan");
			mWifiManager.startScan();
		}
		else if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING)
			RippleUI.showToastNotification(this, "Wifi still enabling...", Toast.LENGTH_SHORT);
		else if (!mUserRefusedWifiEnable)
			wifiAskToEnable();
		else
			RippleUI.showToastNotification(this, "Wifi is disabled.", Toast.LENGTH_SHORT);
	}

	/* 
	 * ==========================================================================================
	 * MISC routines
	 * ==========================================================================================
	 */
	
	private void loadPreferences()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (prefs != null)
		{
			mPrefRippleSpotsOnly = prefs.getBoolean(Preferences.PREF_RIPPLE_SPOTS_ONLY, true);
			mPrefLevelCutoff = Integer.parseInt(prefs.getString(Preferences.PREF_LEVEL_CUTOFF, "-100"));
			this.setPrefAutoscan(prefs.getBoolean(Preferences.PREF_AUTOSCAN, true));
			mPrefAutoscanInterval = Integer.parseInt(prefs.getString(Preferences.PREF_AUTOSCAN_INTERVAL, "5000"));
		}
	}

	public void setPrefAutoscan(Boolean prefAutoScan)
	{
		this.mPrefAutoscan = prefAutoScan;
	}

	@Override
	// Don't count on this always getting called when this activity is killed
	protected void onDestroy()
	{
		try
		{
			super.onDestroy();
			// We are aggressive about hold our wifi lock so we need to be sure
			// it gets released when we are destroyed.
			wifiReleaseLock();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void onPause()
	{
		try
		{
			super.onPause();

			// Start blocking the processing of any scan messages even
			// if we have an active request
			mScanRequestActive = false;
			if (mWifiReceiver != null)
				getApplicationContext().unregisterReceiver(mWifiReceiver);

			// Make sure we don't get called back by a running scan task
			mHandler.removeCallbacks(mWifiScanTask);

			// So we don't leave the UI in the middle of processing
			stopProgress();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		// OnResume gets called even if we bailed early to get signed in so we
		// use a flag to track whether we are completely ready for full operations.
		// We also don't want to launch a wifi scan (which leads to user specific data operations) until
		// we have a current user. The very first wifi scan request gets kicked off
		// at the end of the code that establishes a current user.
		if (mReadyToRun && getCurrentUser() != null)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			if (prefs != null)
			{
				mPrefRippleSpotsOnly = prefs.getBoolean(Preferences.PREF_RIPPLE_SPOTS_ONLY, true);
				mPrefLevelCutoff = Integer.parseInt(prefs.getString(Preferences.PREF_LEVEL_CUTOFF, "-100"));
				this.setPrefAutoscan(prefs.getBoolean(Preferences.PREF_AUTOSCAN, true));
				mPrefAutoscanInterval = Integer.parseInt(prefs.getString(Preferences.PREF_AUTOSCAN_INTERVAL, "5000"));
			}

			// We always try to kick off a scan when radar is started or resumed
			registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			mHandler.removeCallbacks(mWifiScanTask);
			mHandler.post(mWifiScanTask);
		}
	}

	public class SortBySpotLevelDb implements Comparator<Entity>
	{
		@Override
		public int compare(Entity object1, Entity object2)
		{
			if (object1.isTagged && !object2.isTagged)
				return -1;
			else if (object2.isTagged && !object1.isTagged)
				return 1;
			else
				return object2.getAvgPointLevel() - object1.getAvgPointLevel();
		}
	}

	public enum WifiAction
	{
		Enabled, Enabling, Disabled, EnableAndRetry
	}

	class WallPostDialogListener implements DialogListener
	{
		public void onCancel()
		{
			RippleUI.showToastNotification(Radar1.this, "Wall post cancelled!", Toast.LENGTH_SHORT);
		}

		public void onComplete(Bundle values)
		{
			final String postId = values.getString("post_id");
			if (postId != null)
				RippleUI.showToastNotification(Radar1.this, "Message posted to your facebook wall!", Toast.LENGTH_SHORT);
			else
				RippleUI.showToastNotification(Radar1.this, "Wall post cancelled!", Toast.LENGTH_SHORT);
		}

		public void onError(DialogError e)
		{
			RippleUI.showToastNotification(Radar1.this, "Failed to post to wall!", Toast.LENGTH_SHORT);
			e.printStackTrace();
		}

		public void onFacebookError(FacebookError e)
		{
			RippleUI.showToastNotification(Radar1.this, "Failed to post to wall!", Toast.LENGTH_SHORT);
			e.printStackTrace();
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
	
}