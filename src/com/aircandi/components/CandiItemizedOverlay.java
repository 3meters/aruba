package com.aircandi.components;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.aircandi.Aircandi;
import com.aircandi.CandiList;
import com.aircandi.CandiMap;
import com.aircandi.CandiRadar;
import com.aircandi.CandiMap.MapBeacon;
import com.aircandi.MapCandiList;
import com.aircandi.candi.models.CandiModel;
import com.aircandi.components.AnimUtils.TransitionType;
import com.aircandi.components.ProxiExplorer.EntityTree;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Entity;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;
import com.aircandi.R;

@SuppressWarnings("unused")
public class CandiItemizedOverlay extends ItemizedOverlay {

	private ArrayList<OverlayItem>	mOverlays			= new ArrayList<OverlayItem>();
	private MapView					mMapView;
	private List<MapBeacon>			mMapBeacons;
	private List<GeoPoint>			mGeoPoints;
	private List<List<OverlayItem>>	mOverlaysClustered	= new ArrayList<List<OverlayItem>>();
	private Context					mContext;
	private Bitmap					mMarker;
	private Bitmap					mMarkerGrouped;
	private Integer					mZoomLevel;
	private String					mBeaconId;

	private static int				PIXEL_CLUSTER		= 25;
	private long					systemTime			= System.currentTimeMillis();
	private boolean					tapPrimed			= false;
	private float					mTouchStartY;
	private float					mTouchStartX;
	private int						mTouchSlopSquare;
	private long					lastTouchTime		= -1;

	public CandiItemizedOverlay(List<MapBeacon> mapBeacons, List<GeoPoint> geoPoints, Drawable defaultMarker, MapView mapView) {
		super(boundCenterBottom(defaultMarker));

		mMapView = mapView;
		mContext = mMapView.getContext();
		mMapBeacons = mapBeacons;

		final ViewConfiguration configuration = ViewConfiguration.get(mContext);
		int touchSlop = configuration.getScaledTouchSlop();
		int doubleTapSlop = configuration.getScaledDoubleTapSlop();
		mTouchSlopSquare = (touchSlop * touchSlop) / 4;

		mMarker = ((BitmapDrawable) mapView.getResources().getDrawable(R.drawable.ic_map_candi_iii)).getBitmap();
		mMarkerGrouped = ((BitmapDrawable) mapView.getResources().getDrawable(R.drawable.ic_map_candi_cluster)).getBitmap();

		populate();
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected boolean onTap(int index) {
		if (!tapPrimed) return false;

		OverlayItem overlayItem = getItem(index);
		List<OverlayItem> cell = getCluster(overlayItem);

		String title = "";
		mBeaconId = null;
		int folderCount = 0;
		int linkCount = 0;
		int pictureCount = 0;
		int postCount = 0;

		for (OverlayItem item : cell) {
			title += item.getTitle() + ", ";
			Beacon mapBeacon = ProxiExplorer.getInstance().getEntityModel().getBeacon(item.getSnippet());
			if (mapBeacon != null) {

				folderCount += mapBeacon.folderCount;
				linkCount += mapBeacon.linkCount;
				pictureCount += mapBeacon.pictureCount;
				postCount += mapBeacon.postCount;
				mBeaconId = mapBeacon.id;
			}
		}
		title = title.substring(0, title.length() - 2);
		String message = "";
		if (folderCount > 0) {
			message += Aircandi.applicationContext.getString(R.string.candi_map_label_folders) + " " + String.valueOf(folderCount) + "\n";
		}
		if (linkCount > 0) {
			message += Aircandi.applicationContext.getString(R.string.candi_map_label_links) + " " + String.valueOf(linkCount) + "\n";
		}
		if (pictureCount > 0) {
			message += Aircandi.applicationContext.getString(R.string.candi_map_label_pictures) + " " + String.valueOf(pictureCount) + "\n";
		}
		if (postCount > 0) {
			message += Aircandi.applicationContext.getString(R.string.candi_map_label_posts) + " " + String.valueOf(postCount) + "\n";
		}

		/* Do stuff here when you tap, i.e. */
		AircandiCommon.showAlertDialog(R.drawable.ic_app
				, title
				, message
				, null
				, mContext
				, cell.size() == 1 ? R.string.alert_candimap_ok : null
				, cell.size() == 1 ? R.string.alert_candimap_cancel : null
				, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							Logger.d(mContext, "View map candi");

							/*
							 * mCommon.mEntityId is the original entity the user navigated to but
							 * they could have swiped using the viewpager to a different entity so
							 * we need to use mEntity to get the right entity context.
							 */
							IntentBuilder intentBuilder = new IntentBuilder(mMapView.getContext(), MapCandiList.class)
									.setCommandType(CommandType.View)
									.setBeaconId(mBeaconId)
									.setCollectionId(ProxiConstants.ROOT_COLLECTION_ID)
									.setEntityTree(EntityTree.Map);
							
							Intent intent = intentBuilder.create();
							mMapView.getContext().startActivity(intent);
							AnimUtils.doOverridePendingTransition((Activity) mMapView.getContext(), TransitionType.CandiFormToCandiList);
						}
					}
				}
				, null);

		Tracker.trackEvent("DialogMapBeacon", "Open", null, 0);

		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, MapView mapView) {
		long thisTime = System.currentTimeMillis();

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if ((System.currentTimeMillis() - systemTime) < ViewConfiguration.getDoubleTapTimeout()) {
					mapView.getController().zoomInFixing((int) event.getX(), (int) event.getY());
					return true;
				}
				else {
					mTouchStartY = event.getY();
					mTouchStartX = event.getX();
					tapPrimed = false;
					lastTouchTime = thisTime;
				}

				break;
			case MotionEvent.ACTION_UP:
				systemTime = System.currentTimeMillis();
				if (Math.abs(mTouchStartY - event.getY()) <= mTouchSlopSquare
						&& Math.abs(mTouchStartX - event.getX()) <= mTouchSlopSquare) {
					tapPrimed = true;
				}
				break;
		}

		return false;
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void drawTitle(OverlayItem item, String title, Canvas canvas) {

		GeoPoint point = item.getPoint();
		Point markerBottomCenterCoords = new Point();
		mMapView.getProjection().toPixels(point, markerBottomCenterCoords);

		TextPaint paintText = new TextPaint();
		Paint paintRect = new Paint();
		Rect rect = new Rect();

		/* Create rect that is sized to contain the ellipsized text we want to display */
		paintText.setTextSize(CandiConstants.MAP_VIEW_FONT_SIZE);
		title = (String) TextUtils.ellipsize(title, paintText, CandiConstants.MAP_VIEW_TITLE_LENGTH_MAX, TextUtils.TruncateAt.END);
		paintText.getTextBounds(title, 0, title.length(), rect);

		/* Expand the rect by adding margins */
		rect.inset(-CandiConstants.MAP_VIEW_TITLE_MARGIN, -CandiConstants.MAP_VIEW_TITLE_MARGIN);

		/* Move the rect to where we want it to draw */
		rect.offsetTo(markerBottomCenterCoords.x - rect.width() / 2,
				markerBottomCenterCoords.y - item.getMarker(0).getIntrinsicHeight() - rect.height());

		/* Paint the text background into the rect */
		paintRect.setARGB(160, 0, 0, 0);
		canvas.drawRoundRect(new RectF(rect), 4, 4, paintRect);

		/* Paint the text */
		paintText.setTextAlign(Paint.Align.CENTER);
		paintText.setTextSize(CandiConstants.MAP_VIEW_FONT_SIZE);
		paintText.setARGB(255, 255, 255, 255); // alpha, r, g, b (white)
		paintText.setAntiAlias(true);
		canvas.drawText(title, rect.left + rect.width() / 2, rect.bottom - CandiConstants.MAP_VIEW_TITLE_MARGIN, paintText);
	}

	private void drawCount(OverlayItem item, int count, Canvas canvas) {

		GeoPoint point = item.getPoint();
		Point markerBottomCenterCoords = new Point();
		mMapView.getProjection().toPixels(point, markerBottomCenterCoords);

		TextPaint paintText = new TextPaint();
		Paint paintRect = new Paint();
		Rect rect = new Rect();

		/* Create rect that is sized to contain the ellipsized text we want to display */
		paintText.setTextSize(CandiConstants.MAP_VIEW_FONT_SIZE);
		String itemCount = String.valueOf(count);
		paintText.getTextBounds(itemCount, 0, itemCount.length(), rect);

		/* Expand the rect by adding margins */
		rect.inset(-CandiConstants.MAP_VIEW_TITLE_MARGIN, -CandiConstants.MAP_VIEW_TITLE_MARGIN);

		/* Move the rect to where we want it to draw */
		rect.offsetTo(markerBottomCenterCoords.x - rect.width() / 2
				, markerBottomCenterCoords.y - (int) (item.getMarker(0).getIntrinsicHeight() * .80));

		/* Paint the text */
		paintText.setTextAlign(Paint.Align.CENTER);
		paintText.setTextSize(CandiConstants.MAP_VIEW_FONT_SIZE);
		paintText.setARGB(255, 255, 255, 255); // alpha, r, g, b (white)
		paintText.setAntiAlias(true);
		canvas.drawText(itemCount, rect.left + rect.width() / 2, rect.bottom - CandiConstants.MAP_VIEW_TITLE_MARGIN, paintText);
	}

	private void drawMarker(Point point, Canvas canvas, Bitmap marker) {
		canvas.drawBitmap(marker, point.x - (marker.getWidth() / 2), point.y - marker.getHeight(), null);
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {

		if (!shadow) {

			if (mZoomLevel == null || mZoomLevel != mapView.getZoomLevel()) {

				/* We only re-cluster if zoom level changes */

				/* Clear because we might have a new projection */
				mOverlaysClustered.clear();

				/* Assign to bins */
				boolean clusterHit = false;
				for (OverlayItem item : mOverlays) {
					for (List<OverlayItem> cluster : mOverlaysClustered) {
						for (OverlayItem clusterItem : cluster) {
							double distancePx = distanceInPixels(item.getPoint(), clusterItem.getPoint());
							if (distancePx <= PIXEL_CLUSTER) {
								cluster.add(item);
								clusterHit = true;
								break;
							}
						}
						if (clusterHit) {
							break;
						}
					}
					if (!clusterHit) {
						List<OverlayItem> cluster = new ArrayList<OverlayItem>();
						cluster.add(item);
						mOverlaysClustered.add(cluster);
					}
					clusterHit = false;
				}
			}

			/* Draw */
			mZoomLevel = mapView.getZoomLevel();
			for (List<OverlayItem> cluster : mOverlaysClustered) {
				if (cluster.size() > 1) {
					drawGroup(canvas, mapView, cluster);
				}
				else if (cluster.size() == 1) {
					drawSingle(canvas, mapView, cluster);
				}
			}
		}
	}

	private void drawGroup(Canvas canvas, MapView mapView, List<OverlayItem> cluster) {
		GeoPoint point = cluster.get(0).getPoint();
		Point ptScreenCoord = new Point();
		mapView.getProjection().toPixels(point, ptScreenCoord);
		drawMarker(ptScreenCoord, canvas, mMarkerGrouped);
		drawCount(cluster.get(0), cluster.size(), canvas);
	}

	private void drawSingle(Canvas canvas, MapView mapView, List<OverlayItem> cluster) {
		for (OverlayItem item : cluster) {
			GeoPoint geoPoint = item.getPoint();
			Point point = new Point();
			mapView.getProjection().toPixels(geoPoint, point);
			drawMarker(point, canvas, mMarker);
			drawTitle(item, item.getTitle(), canvas);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------
	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}

	@Override
	protected int getIndexToDraw(int arg0) {
		return super.getIndexToDraw(arg0);
	}

	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay);
	}

	public void doPopulate() {
		populate();
	}

	private static boolean isWithin(Point point, MapView mapView) {
		return (point.x > 0 & point.x < mapView.getWidth() & point.y > 0 & point.y < mapView.getHeight());
	}

	private boolean isCurrentLocationVisible(GeoPoint point) {
		Rect currentMapBoundsRect = new Rect();
		Point currentDevicePosition = new Point();

		mMapView.getProjection().toPixels(point, currentDevicePosition);
		mMapView.getDrawingRect(currentMapBoundsRect);

		return currentMapBoundsRect.contains(currentDevicePosition.x, currentDevicePosition.y);
	}

	private List<OverlayItem> getCluster(OverlayItem overlayItem) {
		for (List<OverlayItem> cluster : mOverlaysClustered) {
			for (OverlayItem item : cluster) {
				if (item.equals(overlayItem)) {
					return cluster;
				}
			}
		}
		return null;
	}

	private double distanceInPixels(GeoPoint g1, GeoPoint g2) {
		Projection projection = mMapView.getProjection();
		Point p1 = new Point();
		Point p2 = new Point();
		projection.toPixels(g1, p1);
		projection.toPixels(g2, p2);
		double distancePx = Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
		return distancePx;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	public static class SortMapBeaconsByLatitude implements Comparator<MapBeacon> {

		@Override
		public int compare(MapBeacon object1, MapBeacon object2) {

			double latitude1 = object1.point.getLatitudeE6();
			double latitude2 = object2.point.getLatitudeE6();

			/* Rounded to produce a 5 second bucket that will get further sorted by recent activity */
			if (latitude1 > latitude2) {
				return -1;
			}
			if (latitude1 < latitude2) {
				return 1;
			}
			else {
				return 0;
			}
		}
	}

}
