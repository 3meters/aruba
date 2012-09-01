package com.aircandi.components;

import java.util.ArrayList;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;

import com.aircandi.Aircandi;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.objects.Beacon;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.aircandi.R;

public class CandiItemizedOverlay extends ItemizedOverlay {

	private ArrayList<OverlayItem>	mOverlays	= new ArrayList<OverlayItem>();
	private boolean					mShowTitles	= false;
	private MapView					mMapView;
	private Context					mContext;

	public CandiItemizedOverlay(Drawable defaultMarker, MapView mapView, Boolean showTitles) {
		super(boundCenterBottom(defaultMarker));

		mShowTitles = showTitles;
		mMapView = mapView;
		mContext = mMapView.getContext();
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
		String title = mContext.getString(R.string.dialog_map_title);
		String message = mContext.getString(R.string.dialog_map_message) + overlayItem.getTitle() + " candi";
		Beacon mapBeacon = ProxiExplorer.getInstance().getEntityModel().getMapBeaconById(overlayItem.getSnippet());
		if (mapBeacon != null && mapBeacon.label != null) {
			title = mapBeacon.label;
		}

		if (mapBeacon != null) {
			
			message = "";
			if (mapBeacon.collectionCount > 0) {
				message += Aircandi.applicationContext.getString(R.string.candi_map_label_collections) + " " + String.valueOf(mapBeacon.collectionCount) + "\n";
			}
			if (mapBeacon.linkCount > 0) {
				message += Aircandi.applicationContext.getString(R.string.candi_map_label_links) + " " + String.valueOf(mapBeacon.linkCount) + "\n";
			}
			if (mapBeacon.pictureCount > 0) {
				message += Aircandi.applicationContext.getString(R.string.candi_map_label_pictures) + " " + String.valueOf(mapBeacon.pictureCount) + "\n";
			}
			if (mapBeacon.postCount > 0) {
				message += Aircandi.applicationContext.getString(R.string.candi_map_label_posts) + " " + String.valueOf(mapBeacon.postCount) + "\n";
			}
		}

		// Do stuff here when you tap, i.e. :
		AircandiCommon.showAlertDialog(R.drawable.icon_app, title, message, mContext, null, null, new
				DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {}
					}
				}, null);
		Tracker.trackEvent("DialogMapBeacon", "Open", null, 0);

		return true;
	}

	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay);
		populate();
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {

		super.draw(canvas, mapView, shadow);

		if (!shadow && mShowTitles) {
			for (OverlayItem item : mOverlays) {
				/*
				 * Converts latitude & longitude of this overlay item to coordinates on screen. As we have called
				 * boundCenterBottom() in constructor, so these coordinates will be of the bottom center position of
				 * the displayed marker.
				 */
				GeoPoint point = item.getPoint();
				Point markerBottomCenterCoords = new Point();
				mapView.getProjection().toPixels(point, markerBottomCenterCoords);

				/* Find the width and height of the title */
				if (item.getTitle() != null) {

					TextPaint paintText = new TextPaint();
					Paint paintRect = new Paint();
					Rect rect = new Rect();

					/* Create rect that is sized to contain the ellipsized text we want to display */
					paintText.setTextSize(CandiConstants.MAP_VIEW_FONT_SIZE);
					String title = (String) TextUtils.ellipsize(item.getTitle(), paintText, CandiConstants.MAP_VIEW_TITLE_LENGTH_MAX, TextUtils.TruncateAt.END);
					paintText.getTextBounds(title, 0, title.length(), rect);

					/* Expand the rect by adding margins */
					rect.inset(-CandiConstants.MAP_VIEW_TITLE_MARGIN, -CandiConstants.MAP_VIEW_TITLE_MARGIN);

					/* Move the rect to where we want it to draw */
					rect.offsetTo(markerBottomCenterCoords.x - rect.width() / 2, markerBottomCenterCoords.y - item.getMarker(0).getIntrinsicHeight() - rect.height());

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
			}
		}
	}
}
