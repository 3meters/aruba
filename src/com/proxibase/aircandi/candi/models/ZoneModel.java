package com.proxibase.aircandi.candi.models;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import com.proxibase.aircandi.candi.models.CandiModel.Transition;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;
import com.proxibase.aircandi.components.Exceptions;
import com.proxibase.aircandi.core.CandiConstants;

public class ZoneModel extends BaseModel {

	public static int			ZONE_CHILDREN_MAX_VISIBLE				= 9;
	public static int			ZONE_CHILDREN_MAX_VISIBLE_WITH_PRIMARY	= 6;

	private List<CandiModel>	mCandiesCurrent							= new ArrayList<CandiModel>();
	private List<CandiModel>	mCandiesNext							= new ArrayList<CandiModel>();
	private int					mZoneIndex;
	private String				mBodyImageUri;
	private boolean				mInactive								= false;

	public ZoneModel(int zoneIndex, CandiPatchModel candiPatchModel) {

		mCandiPatchModel = candiPatchModel;
		mZoneIndex = zoneIndex;
		initialize();
	}

	private void initialize() {
		/* 
		 * Zones have a locked in position right from the beginning 
		 */
		mViewStateCurrent.setX(mCandiPatchModel.getX(mZoneIndex) + CandiPatchPresenter.mRadarPaddingLeft);
		mViewStateCurrent.setY(mCandiPatchModel.getY(mZoneIndex) + CandiPatchPresenter.mRadarPaddingTop);
		mViewStateCurrent.setWidth(CandiConstants.CANDI_VIEW_WIDTH);
		mViewStateCurrent.setHeight(CandiConstants.CANDI_VIEW_BODY_HEIGHT);
	}

	public void shiftToNext() {
		mViewStateCurrent.setVisible(mViewStateNext.isVisible());
		mCandiesCurrent.clear();
		for (CandiModel candiModel : mCandiesNext) {
			mCandiesCurrent.add(candiModel);
		}
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
		if (mViewStateCurrent.isVisible() != mViewStateNext.isVisible()) {
			return mViewStateNext.isVisible() ? Transition.FadeIn : Transition.FadeOut;
		}
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

	public Position getChildPositionNext(CandiModel candiModelTarget) throws NoSuchElementException {

		if (mInactive) {
			return new Position();
		}
		/*
		 * TODO: Candies that have been temporarily hidden because of a root change won't show up
		 * in the collection unless I make a change.
		 */
		if (!mCandiesNext.contains(candiModelTarget)) {
			throw new NoSuchElementException();
		}

		Position position = doChildPosition(candiModelTarget, mCandiesNext, candiModelTarget.getZoneStateNext().getStatus());
		position.scale = getChildScaleNext(candiModelTarget);
		return position;
	}

	private Position doChildPosition(CandiModel candiModelTarget, List<CandiModel> candiModels, ZoneStatus candiModelZoneStatus) {

		double index = 0;
		for (IModel candiModel : candiModels) {
			if (candiModel == candiModelTarget) {
				break;
			}
			index++;
		}

		double offsetX = 0;
		double offsetY = 0;
		double columns = 1;
		double rows = 1;
		Position position = new Position();
		position.rowLast = false;
		position.rowFirst = false;
		position.colFirst = false;
		position.colLast = false;

		if (index > ZONE_CHILDREN_MAX_VISIBLE - 1) {
			index = ZONE_CHILDREN_MAX_VISIBLE - 1;
		}

		if (candiModelZoneStatus == ZoneStatus.Normal) {

			/* Showing a collection without a primary */
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

			/* X,Y position */
			position.x = (float) (this.getViewStateCurrent().getX() + ((index % columns) * offsetX));
			if (candiesCount == 1) {
				position.y = (float) (this.getViewStateCurrent().getY() + ((Math.floor(index / rows)) * offsetY));
			}
			else {
				position.y = (float) (CandiConstants.CANDI_VIEW_TITLE_HEIGHT + this.getViewStateCurrent().getY() + ((Math.floor(index / rows)) * offsetY));
			}

			/* Row/Col position */
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

				/* Showing a collection with a primary and this is the primary */
				position.x = this.getViewStateCurrent().getX();
				position.y = (float) (CandiConstants.CANDI_VIEW_TITLE_HEIGHT + this.getViewStateCurrent().getY());
				position.col = 1;
				position.row = 1;
				position.colFirst = true;
				position.colLast = false;
				position.rowFirst = true;
				position.rowLast = false;
			}
			else if (candiModelZoneStatus == ZoneStatus.Secondary) {

				Random rand = new Random(candiModelTarget.getParent().hashCode());
				int[] candies = { 2, 5, 6, 7, 8 };

				for (int i = 0; i < candies.length; i++) {
					int randomPosition = rand.nextInt(candies.length);
					int temp = candies[i];
					candies[i] = candies[randomPosition];
					candies[randomPosition] = temp;
				}

				if (index >= 5) {
					index = 5;
				}

				index = candies[(int) (index - 1)];

				offsetX = CandiConstants.CANDI_VIEW_WIDTH / 3;
				offsetY = CandiConstants.CANDI_VIEW_BODY_HEIGHT / 3;
				columns = 3;
				rows = 3;

				position.col = (int) (index % columns) + 1;

				int fixedX = 0;
				if (position.col == columns) {
					position.colLast = true;
					fixedX = 170;
				}
				else if (position.col == 1) {
					position.colFirst = true;
				}
				else if (position.col == 2) {
					position.colFirst = true;
					fixedX = 85;
				}

				position.row = (int) Math.floor(index / rows) + 1;

				int fixedY = 0;
				if (position.row == rows) {
					position.rowLast = true;
					fixedY = 170;
				}
				else if (position.row == 1) {
					position.rowFirst = true;
				}
				else if (position.row == 2) {
					position.rowFirst = true;
					fixedY = 85;
				}

				position.x = (float) (this.getViewStateCurrent().getX() + fixedX);
				position.y = (float) (CandiConstants.CANDI_VIEW_TITLE_HEIGHT + this.getViewStateCurrent().getY() + fixedY);
			}
		}

		return position;
	}

	public float getChildScaleCurrent(CandiModel candiModelTarget) {
		if (mInactive) {
			return 1.0f;
		}

		float scale = 1.0f;

		if (candiModelTarget.getZoneStateCurrent().getStatus() == ZoneStatus.Normal) {
			int candiesCount = mCandiesCurrent.size();
			if (candiesCount > 1) {
				scale = 0.5f;
			}
			if (candiesCount > 4) {
				scale = 0.32f;
			}
		}
		else {
			if (candiModelTarget.getZoneStateCurrent().getStatus() == ZoneStatus.Primary)
				scale = 0.65f;
			else
				scale = 0.32f;
		}

		return scale;
	}

	public float getChildScaleNext(CandiModel candiModelTarget) {
		if (mInactive) {
			return CandiConstants.CANDI_VIEW_SCALE;
		}

		float scale = CandiConstants.CANDI_VIEW_SCALE;

		if (candiModelTarget.getZoneStateNext().getStatus() == ZoneStatus.Normal) {
			if (mCandiesNext.size() > 4) {
				scale = 0.32f;
			}
			else if (mCandiesNext.size() > 1) {
				scale = 0.5f;
			}
		}
		else {
			if (candiModelTarget.getZoneStateNext().getStatus() == ZoneStatus.Primary)
				scale = 0.66f;
			else
				scale = 0.32f;
		}

		return scale;
	}

	public String getBodyImageUri() {
		return mBodyImageUri;
	}

	public void setBodyImageUri(String bodyImageUri) {
		mBodyImageUri = bodyImageUri;
	}

	public List<CandiModel> getCandiesCurrent() {
		return mCandiesCurrent;
	}

	public List<CandiModel> getCandiesNext() {
		return mCandiesNext;
	}

	public void setCandiesCurrent(List<CandiModel> candiesCurrent) {
		mCandiesCurrent = candiesCurrent;
	}

	public void setCandiesNext(List<CandiModel> candiesNext) {
		mCandiesNext = candiesNext;
	}

	public int getZoneIndex() {
		return mZoneIndex;
	}

	public static class Position {

		public float	scale		= CandiConstants.CANDI_VIEW_SCALE;
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
				Exceptions.Handle(exception);
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
		mInactive = inactive;
	}

	public boolean isInactive() {
		return mInactive;
	}

	public static enum ZoneAlignment {
		None, Bottom, Center, Left, Right, Top
	}

	public static enum ZoneStatus {
		Primary, Secondary, Normal
	}
}
