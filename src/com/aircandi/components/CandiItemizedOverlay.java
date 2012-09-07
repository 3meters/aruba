package com.aircandi.components;

import java.util.ArrayList;
import java.util.List;

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

import com.aircandi.Aircandi;
import com.aircandi.CandiRadar;
import com.aircandi.CandiMap.MapBeacon;
import com.aircandi.components.AnimUtils.TransitionType;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.objects.Beacon;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;
import com.aircandi.R;

@SuppressWarnings("unused")
public class CandiItemizedOverlay extends ItemizedOverlay {

	private ArrayList<OverlayItem>			mOverlays	= new ArrayList<OverlayItem>();
	private MapView							mMapView;
	private List<MapBeacon>					mMapBeacons;
	private List<GeoPoint>					mGeoPoints;
	private List<List<List<OverlayItem>>>	mMapGrid;
	private Context							mContext;
	private Bitmap							mMarker;
	private Bitmap							mMarkerGrouped;
	private Integer							mZoomLevel;

	private static int						DENSITY_X	= 20;
	private static int						DENSITY_Y	= 20;

	public CandiItemizedOverlay(List<MapBeacon> mapBeacons, List<GeoPoint> geoPoints, Drawable defaultMarker, MapView mapView) {
		super(boundCenterBottom(defaultMarker));

		mMapView = mapView;
		mContext = mMapView.getContext();
		mMapBeacons = mapBeacons;

		mMarker = ((BitmapDrawable) mapView.getResources().getDrawable(R.drawable.icon_map_candi_ii)).getBitmap();
		mMarkerGrouped = ((BitmapDrawable) mapView.getResources().getDrawable(R.drawable.icon_map_candi)).getBitmap();

		/* 2D array with some configurable, fixed density */
		mMapGrid = new ArrayList<List<List<OverlayItem>>>(DENSITY_X);
		for (int i = 0; i < DENSITY_X; i++) {
			ArrayList<List<OverlayItem>> column = new ArrayList<List<OverlayItem>>(DENSITY_Y);
			for (int j = 0; j < DENSITY_Y; j++) {
				column.add(new ArrayList<OverlayItem>());
			}
			mMapGrid.add(column);
		}

		populate();
	}

	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}

	@Override
	protected boolean onTap(int index) {
		OverlayItem overlayItem = getItem(index);
		List<OverlayItem> cell = getCell(overlayItem);

		String title = "";
		int collectionCount = 0;
		int linkCount = 0;
		int pictureCount = 0;
		int postCount = 0;

		for (OverlayItem item : cell) {
			title += item.getTitle() + ", ";
			Beacon mapBeacon = ProxiExplorer.getInstance().getEntityModel().getMapBeaconById(overlayItem.getSnippet());
			if (mapBeacon != null) {

				collectionCount += mapBeacon.collectionCount;
				linkCount += mapBeacon.linkCount;
				pictureCount += mapBeacon.pictureCount;
				postCount += mapBeacon.postCount;
			}
		}
		title = title.substring(0, title.length() - 2);
		String message = "";
		if (collectionCount > 0) {
			message += Aircandi.applicationContext.getString(R.string.candi_map_label_collections) + " " + String.valueOf(collectionCount) + "\n";
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
		AircandiCommon.showAlertDialog(R.drawable.icon_app
				, title
				, message
				, mContext
				, R.string.alert_candimap_ok
				, R.string.alert_candimap_cancel
				, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							Logger.d(mContext, "View map candi");
						}
					}
				}
				, null);

		Tracker.trackEvent("DialogMapBeacon", "Open", null, 0);

		return true;
	}

	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay);
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

	private List<OverlayItem> getCell(OverlayItem overlayItem) {
		for (List<List<OverlayItem>> column : mMapGrid) {
			for (List<OverlayItem> cell : column) {
				for (OverlayItem item : cell) {
					if (item.equals(overlayItem)) {
						return cell;
					}
				}
			}
		}
		return null;
	}

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
		paintRect.setARGB(96, 0, 0, 0);
		canvas.drawRoundRect(new RectF(rect), 4, 4, paintRect);

		/* Paint the text */
		paintText.setTextAlign(Paint.Align.CENTER);
		paintText.setTextSize(CandiConstants.MAP_VIEW_FONT_SIZE);
		paintText.setARGB(255, 255, 255, 255); // alpha, r, g, b (white)
		paintText.setAntiAlias(true);
		canvas.drawText(title, rect.left + rect.width() / 2, rect.bottom - CandiConstants.MAP_VIEW_TITLE_MARGIN, paintText);
	}

	private void drawMarker(Point point, Canvas canvas) {
		canvas.drawBitmap(mMarker, point.x - (mMarker.getWidth() / 2), point.y - mMarker.getHeight(), null);
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {

		if (!shadow) {

			if (mZoomLevel == null || mZoomLevel != mapView.getZoomLevel()) {
				
				/* We only re-bin if zoom level changes */

				/* Clear because we might have a new projection */
				for (List<List<OverlayItem>> column : mMapGrid) {
					for (List<OverlayItem> cell : column) {
						cell.clear();
					}
				}

				/* Assign to bins */
				for (OverlayItem item : mOverlays) {
					int binX;
					int binY;

					if (isCurrentLocationVisible(item.getPoint())) {

						Projection projection = mapView.getProjection();
						Point point = projection.toPixels(item.getPoint(), null);

						double fractionX = ((double) point.x / (double) mapView.getWidth());
						binX = (int) (Math.floor(DENSITY_X * fractionX));
						double fractionY = ((double) point.y / (double) mapView.getHeight());
						binY = (int) (Math.floor(DENSITY_Y * fractionY));
						mMapGrid.get(binX).get(binY).add(item);
					}
				}
			}

			/* Draw */
			mZoomLevel = mapView.getZoomLevel();
			for (int i = 0; i < DENSITY_X; i++) {
				for (int j = 0; j < DENSITY_Y; j++) {
					List<OverlayItem> cell = mMapGrid.get(i).get(j);
					if (cell.size() > 1) {
						drawGroup(canvas, mapView, cell);
					}
					else if (cell.size() == 1) {
						// draw single marker
						drawSingle(canvas, mapView, cell);
					}
				}
			}
		}
	}

	private void drawGroup(Canvas canvas, MapView mapView, List<OverlayItem> cell) {
		GeoPoint point = cell.get(0).getPoint();
		Point ptScreenCoord = new Point();
		mapView.getProjection().toPixels(point, ptScreenCoord);
		drawMarker(ptScreenCoord, canvas);
		drawTitle(cell.get(0), mapView.getResources().getString(R.string.candi_map_label_grouped) + " (" + String.valueOf(cell.size()) + ")", canvas);
	}

	private void drawSingle(Canvas canvas, MapView mapView, List<OverlayItem> cell) {
		for (OverlayItem item : cell) {
			GeoPoint geoPoint = item.getPoint();
			Point point = new Point();
			mapView.getProjection().toPixels(geoPoint, point);
			drawMarker(point, canvas);
			drawTitle(item, item.getTitle(), canvas);
		}
	}
}
