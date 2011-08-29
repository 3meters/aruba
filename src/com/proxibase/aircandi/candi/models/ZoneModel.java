package com.proxibase.aircandi.candi.models;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import com.proxibase.aircandi.candi.models.CandiModel.Transition;
import com.proxibase.aircandi.candi.utils.CandiConstants;

public class ZoneModel extends BaseModel {

	public static int			ZONE_CHILDREN_MAX_VISIBLE				= 9;
	public static int			ZONE_CHILDREN_MAX_VISIBLE_WITH_PRIMARY	= 6;

	private List<CandiModel>	mCandiesCurrent							= new ArrayList<CandiModel>();
	private List<CandiModel>	mCandiesNext							= new ArrayList<CandiModel>();
	private int					mZoneIndex;
	private CandiPatchModel		mCandiPatchModel						= null;
	private float				mX;
	private float				mY;
	private float				mCenterX;
	private float				mCenterY;
	private String				mBodyImageId							= "";
	private String				mBodyImageUrl							= "";
	private boolean				mInactive								= false;

	public ZoneModel(int zoneIndex, CandiPatchModel candiPatchModel) {

		mCandiPatchModel = candiPatchModel;
		mZoneIndex = zoneIndex;
		this.initialize();
	}

	private void initialize() {
		updateCoordinates();
	}

	public void updateCoordinates() {
		mX = mCandiPatchModel.getOriginX() + ((CandiConstants.CANDI_VIEW_WIDTH + CandiConstants.CANDI_VIEW_SPACING) * mZoneIndex);
		mY = mCandiPatchModel.getOriginY();
		mCenterX = mX + (CandiConstants.CANDI_VIEW_WIDTH * 0.5f);
		mCenterY = mY + (CandiConstants.CANDI_VIEW_BODY_HEIGHT * 0.5f);
	}

	public void shiftToNext() {
		super.shiftToNext();

		mCandiesCurrent.clear();
		for (CandiModel candiModel : mCandiesNext)
			mCandiesCurrent.add(candiModel);
	}

	public int getCandiIndexCurrent(CandiModel candiModelTarget) {
		int index = 0;
		for (IModel candiModel : mCandiesCurrent) {
			if (candiModel == candiModelTarget)
				break;
			index++;
		}
		return index;
	}

	public int getCandiIndexNext(CandiModel candiModelTarget) {
		int index = 0;
		for (IModel candiModel : mCandiesNext) {
			if (candiModel == candiModelTarget)
				break;
			index++;
		}
		return index;
	}

	public Transition getTransition() {

		if (!this.isVisibleCurrent() && this.isVisibleNext())
			return Transition.FadeIn;

		if (this.isVisibleCurrent() && !this.isVisibleNext())
			return Transition.FadeOut;

		return Transition.None;
	}

	public boolean isOccupiedCurrent() {
		return (mCandiesCurrent.size() > 0);
	}

	public boolean isOccupiedNext() {
		return (mCandiesNext.size() > 0);
	}

	public IModel getFirstCurrent() {
		if (mCandiesCurrent.size() > 0)
			return mCandiesCurrent.get(0);
		else
			return null;
	}

	public IModel getFirstNext() {
		if (mCandiesNext.size() > 0)
			return mCandiesNext.get(0);
		else
			return null;
	}

	public float getX() {
		return this.mX;
	}

	@Override
	public boolean isVisibleCurrent() {
		if (this.mInactive || this.mCandiesCurrent.size() == 0)
			return false;
		else
			return super.isVisibleCurrent();
	}

	@Override
	public boolean isVisibleNext() {
		if (this.mInactive || this.mCandiesNext.size() == 0)
			return false;
		else
			return super.isVisibleNext();
	}

	public Position getChildPositionCurrent(CandiModel candiModelTarget) throws NoSuchElementException {

		if (this.mInactive)
			return new Position();

		if (!mCandiesCurrent.contains(candiModelTarget))
			throw new NoSuchElementException();

		Position position = doChildPosition(candiModelTarget, candiModelTarget.getPositionCurrent(), mCandiesCurrent, candiModelTarget
				.getZoneStatusCurrent());
		position.scale = this.getChildScaleCurrent(candiModelTarget);
		return position;
	}

	public Position getChildPositionNext(CandiModel candiModelTarget) throws NoSuchElementException {

		if (this.mInactive)
			return new Position();

		// TODO: Candies that have been temporarily hidden because of a root change won't show up
		// in the collection unless I make a change.
		if (!mCandiesNext.contains(candiModelTarget))
			throw new NoSuchElementException();

		Position position = doChildPosition(candiModelTarget, candiModelTarget.getPositionNext(), mCandiesNext, candiModelTarget.getZoneStatusNext());
		position.scale = this.getChildScaleNext(candiModelTarget);
		return position;
	}

	private Position doChildPosition(CandiModel candiModelTarget, Position position, List<CandiModel> candiModels, ZoneStatus candiModelZoneStatus) {

		double index = 0;
		for (IModel candiModel : candiModels) {
			if (candiModel == candiModelTarget)
				break;
			index++;
		}

		double offsetX = 0;
		double offsetY = 0;
		double columns = 1;
		double rows = 1;
		position.rowLast = false;
		position.rowFirst = false;
		position.colFirst = false;
		position.colLast = false;

		if (index > ZONE_CHILDREN_MAX_VISIBLE - 1)
			index = ZONE_CHILDREN_MAX_VISIBLE - 1;

		if (candiModelZoneStatus == ZoneStatus.Normal) {
			/*
			 * Showing a collection without a primary
			 */
			int candiesCount = candiModels.size();

			if (candiesCount > 4) {
				offsetX = CandiConstants.CANDI_VIEW_WIDTH / 3;
				offsetY = CandiConstants.CANDI_VIEW_BODY_HEIGHT / 3;
				columns = 3;
				rows = 3;
			}
			else if (candiesCount > 1) {
				offsetX = CandiConstants.CANDI_VIEW_WIDTH / 2;
				offsetY = CandiConstants.CANDI_VIEW_BODY_HEIGHT / 2;
				columns = 2;
				rows = 2;
			}

			// X,Y position
			position.x = (float) (mX + ((index % columns) * offsetX));
			if (candiesCount == 1) {
				position.y = (float) (mY + ((Math.floor(index / rows)) * offsetY));
			}
			else {
				position.y = (float) (CandiConstants.CANDI_VIEW_TITLE_HEIGHT + mY + ((Math.floor(index / rows)) * offsetY));
			}

			// Row/Col position
			position.col = (int) (index % columns) + 1;
			if (position.col == columns) {
				position.colLast = true;
			}
			else if (position.col == 1) {
				position.colFirst = true;
			}

			position.row = (int) Math.floor(index / rows) + 1;
			if (position.row == rows) {
				position.rowLast = true;
			}
			else if (position.row == 1) {
				position.rowFirst = true;
			}
		}
		else {
			if (candiModelZoneStatus == ZoneStatus.Primary) {
				/*
				 * Showing a collection with a primary and this is the primary
				 */
				position.x = mX;
				position.y = (float) (CandiConstants.CANDI_VIEW_TITLE_HEIGHT + mY);
				position.col = 1;
				position.row = 1;
				position.colFirst = true;
				position.colLast = false;
				position.rowFirst = true;
				position.rowLast = false;
			}
			else if (candiModelZoneStatus == ZoneStatus.Secondary) {
				/*
				 * Showing a collection with a primary
				 */
				if (index > 5)
					index = 5;
				offsetX = CandiConstants.CANDI_VIEW_WIDTH / 3;
				offsetY = CandiConstants.CANDI_VIEW_BODY_HEIGHT / 3;
				columns = 3;
				rows = 3;

				if (index == 1)
					index = 2;
				else if (index >= 2)
					index += 3;

				position.x = (float) (mX + ((index % columns) * offsetX));
				position.y = (float) (CandiConstants.CANDI_VIEW_TITLE_HEIGHT + mY + ((Math.floor(index / rows)) * offsetY));
				
				position.col = (int) (index % columns) + 1;
				if (position.col == columns)
					position.colLast = true;
				else if (position.col == 1)
					position.colFirst = true;

				position.row = (int) Math.floor(index / rows) + 1;

				if (position.row == rows)
					position.rowLast = true;
				else if (position.row == 1)
					position.rowFirst = true;
			}
		}

		return position;
	}

	public float getChildScaleCurrent(CandiModel candiModelTarget) {
		if (this.mInactive)
			return 1.0f;

		float scale = 1.0f;

		if (candiModelTarget.getZoneStatusCurrent() == ZoneStatus.Normal) {
			int candiesCount = mCandiesCurrent.size();
			if (candiesCount > 1) {
				scale = 0.5f;
			}
			if (candiesCount > 4) {
				scale = 0.32f;
			}
		}
		else {
			if (candiModelTarget.getZoneStatusCurrent() == ZoneStatus.Primary)
				scale = 0.65f;
			else
				scale = 0.32f;
		}

		return scale;
	}

	public float getChildScaleNext(CandiModel candiModelTarget) {
		if (this.mInactive)
			return 1.0f;

		float scale = 1.0f;

		if (candiModelTarget.getZoneStatusNext() == ZoneStatus.Normal) {
			if (mCandiesNext.size() > 4) {
				scale = 0.32f;
			}
			else if (mCandiesNext.size() > 1) {
				scale = 0.5f;
			}
		}
		else {
			if (candiModelTarget.getZoneStatusNext() == ZoneStatus.Primary)
				scale = 0.65f;
			else
				scale = 0.32f;
		}

		return scale;
	}

	public float getY() {

		return this.mY;
	}

	public float getCenterX() {

		return this.mCenterX;
	}

	public float getCenterY() {

		return this.mCenterY;
	}

	public void setX(float x) {

		this.mX = x;
	}

	public void setY(float y) {

		this.mY = y;
	}

	public void setCenterX(float centerX) {

		this.mCenterX = centerX;
	}

	public void setCenterY(float centerY) {

		this.mCenterY = centerY;
	}

	public String getBodyImageId() {
		return this.mBodyImageId;
	}

	public String getBodyImageUrl() {
		return this.mBodyImageUrl;
	}

	public void setBodyImageId(String bodyImageId) {
		this.mBodyImageId = bodyImageId;
	}

	public void setBodyImageUrl(String bodyImageUrl) {
		this.mBodyImageUrl = bodyImageUrl;
	}

	public List<CandiModel> getCandiesCurrent() {
		return this.mCandiesCurrent;
	}

	public List<CandiModel> getCandiesNext() {
		return this.mCandiesNext;
	}

	public void setCandiesCurrent(List<CandiModel> candiesCurrent) {
		this.mCandiesCurrent = candiesCurrent;
	}

	public void setCandiesNext(List<CandiModel> candiesNext) {
		this.mCandiesNext = candiesNext;
	}

	public int getZoneIndex() {
		return this.mZoneIndex;
	}

	public void setZoneIndex(int zoneIndex) {
		this.mZoneIndex = zoneIndex;
	}

	public static class Position {

		public float	scale		= 1.0f;
		public float	x			= 0;
		public float	y			= 0;
		public int		row			= 0;
		public boolean	rowFirst	= false;
		public boolean	rowLast		= false;
		public int		col			= 0;
		public boolean	colFirst	= false;
		public boolean	colLast		= false;

		@Override
		public boolean equals(Object other) {

			if (this == other) {
				return true;
			}
			if (!(other instanceof Position)) {
				return false;
			}
			Position that = (Position) other;
			return (this.x == that.x && this.y == that.y && this.scale == that.scale);
		}

		@Override
		protected Object clone() {
			try {
				return super.clone();
			}
			catch (CloneNotSupportedException exception) {
				exception.printStackTrace();
				return null;
			}
		}

		@Override
		public int hashCode() {

			int result = 100;
			result = 31 * result + (int) this.x;
			result = 31 * result + (int) this.y;
			result = 31 * result + (int) this.scale;
			return result;
		}

	}

	public void setInactive(boolean inactive) {
		this.mInactive = inactive;
	}

	public boolean isInactive() {
		return mInactive;
	}

	public enum ZoneStatus {
		Primary, Secondary, Normal
	}
}
