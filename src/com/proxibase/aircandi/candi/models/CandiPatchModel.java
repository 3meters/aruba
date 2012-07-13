package com.proxibase.aircandi.candi.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;

import com.proxibase.aircandi.candi.models.CandiModel.DisplayExtra;
import com.proxibase.aircandi.candi.models.CandiModel.ReasonInactive;
import com.proxibase.aircandi.candi.models.ZoneModel.Position;
import com.proxibase.aircandi.candi.models.ZoneModel.ZoneAlignment;
import com.proxibase.aircandi.candi.models.ZoneModel.ZoneStatus;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;
import com.proxibase.aircandi.components.CandiList;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.service.objects.Entity;

/**
 * @author Jayma
 *         Inputs:
 *         patch rotation,
 *         zone ordering, zone membership, zone count, zone current/focus,
 *         camera position, scale
 */

public class CandiPatchModel extends Observable {

	private final CandiList<CandiModel>	mCandiModels		= new CandiList<CandiModel>();
	private final List<ZoneModel>		mZoneModels			= new ArrayList<ZoneModel>();

	private ZoneModel					mZoneInactive		= null;
	private CandiModel					mCandiModelFocused	= null;
	private CandiModel					mCandiModelSelected	= null;
	private IModel						mCandiRootCurrent;
	private IModel						mCandiRootNext;
	private float						mOriginX;
	private float						mOriginY;
	private int							mScreenWidth;

	public CandiPatchModel() {

		/* New zones start out not visible with alpha = 0 */
		mZoneInactive = new ZoneModel(0, this);
		mZoneInactive.setInactive(true);
	}

	/**
	 * Calling update() will notify observers of this and trigger notifications to
	 * observers of child models. If atomically appropriate, update() can be called on
	 * child models independent of this parent model. The changed status for child
	 * models does not bubble up to this parent model.
	 */
	public void update() {

		/*
		 * The super class only updates observers if hasChanged == true.
		 * Super class also handles clearChanged.
		 */
		super.notifyObservers();

		/* Each submodel will notify it's observers only if it has been flagged as changed. */
		for (IModel candi : mCandiModels) {
			candi.update();
		}

		for (ZoneModel zone : mZoneModels) {
			zone.update();
		}
	}

	public void reset() {
		mZoneModels.clear();
		mCandiModels.clear();
		mCandiModelFocused = null;
		mCandiModelSelected = null;
		mCandiRootCurrent = null;
		mCandiRootNext = null;
		super.setChanged();
	}

	public void shiftToNext() {

		mCandiRootCurrent = mCandiRootNext;

		mZoneInactive.shiftToNext();

		for (ZoneModel zone : mZoneModels)
			zone.shiftToNext();

		for (IModel candiModel : mCandiModels)
			candiModel.shiftToNext();
	}

	/**
	 * When new candi models are added they start out
	 * in the inactive zone until they get properly assigned
	 * during the updateZones pass.
	 */
	public void addCandiModel(CandiModel candiModel) {

		mCandiModels.add(candiModel);

		/* Only assign to a zone if the model is visible */
		if (candiModel.getEntity().hidden) {
			candiModel.getViewStateCurrent().setVisible(false);
			candiModel.getViewStateNext().setVisible(false);
		}
		else {
			candiModel.getViewStateCurrent().setVisible(false);
			candiModel.getViewStateNext().setVisible(true);
		}

		candiModel.setReasonInactive(ReasonInactive.New);
		mZoneInactive.getCandiesCurrent().add(candiModel);
		mZoneInactive.getCandiesNext().add(candiModel);

		candiModel.getZoneStateCurrent().setZone(mZoneInactive);
		candiModel.getZoneStateNext().setZone(mZoneInactive);

		super.setChanged();
	}

	/**
	 * Updates if the model exists but never fails
	 */
	public CandiModel updateCandiModel(Entity entity, DisplayExtra displayExtra) {
		/*
		 * This only gets called when doing a partial update and a candi model already exists. A partial
		 * update only pulls in entities for new beacons but does not pick up service side changes for the
		 * entities for old beacons. There can however be local changes to existing entities which include
		 * hidden status based on signal fencing and visibleTime property.
		 */

		/*
		 * Stuff that could have been updated
		 * - Any property of the entity proxy
		 * - Entity properties that propogate up to the model
		 * - Beacon properties (these get pulled in with the entity)
		 */

		/*
		 * Transfer the entire entity proxy.
		 * 
		 * Before we replace the old entity, we need to note any changes that should
		 * be flagged for later handling.
		 */
		CandiModel candiModelManaged = mCandiModels.getByKey(String.valueOf(entity.id));
		Entity originalEntity = candiModelManaged.getEntity();
		if (!entity.getMasterImageUri().equals(originalEntity.getMasterImageUri())) {
			candiModelManaged.setMasterImageUpdated(true);
		}

		entity.rookie = originalEntity.rookie;
		entity.discoveryTime = originalEntity.discoveryTime;
		if (entity.rookie) {
			entity.discoveryTime = DateUtils.nowDate();
			if (!entity.hidden) {
				entity.rookie = false;
			}
		}

		candiModelManaged.setEntity(entity);
		candiModelManaged.setTitleText(entity.label);

		if (candiModelManaged.getReasonInactive() != ReasonInactive.Navigation) {
			if (entity.hidden) {
				candiModelManaged.getViewStateNext().setVisible(false);
			}
			else {
				candiModelManaged.getViewStateNext().setVisible(true);
			}
		}

		candiModelManaged.setDisplayExtra(displayExtra);

		/* If transitioning from hidden to visible, it might not have a zone yet */
		if (candiModelManaged.getViewStateNext().isVisible() && candiModelManaged.getZoneStateCurrent().getZone().isInactive()) {

			ZoneModel zoneTarget = null;
			for (ZoneModel zone : mZoneModels)
				if (zone.getCandiesCurrent().size() == 0) {
					zoneTarget = zone;
					break;
				}

			if (zoneTarget == null) {
				zoneTarget = new ZoneModel(mZoneModels.size(), this);
				zoneTarget.getViewStateCurrent().setVisible(false);
				zoneTarget.getViewStateNext().setVisible(true);
				mZoneModels.add(zoneTarget);
			}

			zoneTarget.getCandiesCurrent().add(candiModelManaged);
			zoneTarget.getCandiesNext().add(candiModelManaged);

			candiModelManaged.getZoneStateCurrent().setZone(zoneTarget);
			candiModelManaged.getZoneStateNext().setZone(zoneTarget);
		}
		return candiModelManaged;
	}

	public void updateVisibilityNext(boolean chunking) {

		/* Reset inactive zone */
		mZoneInactive.getCandiesNext().clear();

		/*
		 * Manage visibility and touch areas.
		 * mCandiModels is a flattened list of all candi models.
		 */
		for (CandiModel candiModel : mCandiModels) {
			candiModel.setTouchAreaActive(false);
			candiModel.setChanged();
			if (candiModel.getReasonInactive() == ReasonInactive.Deleting) {

				/* Assign candies being deleted to the inactive zone */
				candiModel.getViewStateNext().setVisible(false);
				candiModel.getZoneStateNext().setZone(mZoneInactive);
				mZoneInactive.getCandiesNext().add(candiModel);
			}
			else {
				boolean underNextRoot = mCandiRootNext.getChildren().containsKey(String.valueOf(candiModel.getModelId())) || mCandiRootNext
						.getChildren().containsKey(String.valueOf(((CandiModel) candiModel.getParent()).getModelId()));
				if (!underNextRoot) {
					/* Assign candies being hidden because of navigation to the inactive zone */
					candiModel.getViewStateNext().setVisible(false);
					candiModel.setReasonInactive(ReasonInactive.Navigation);
					candiModel.getZoneStateNext().setZone(mZoneInactive);
					candiModel.getZoneStateNext().setStatus(ZoneStatus.Normal);
					mZoneInactive.getCandiesNext().add(candiModel);
				}
				else {
					candiModel.setReasonInactive(ReasonInactive.None);
					if (candiModel.getEntity().hidden) {
						/* Assign hidden candies to the inactive zone */
						candiModel.getViewStateNext().setVisible(false);
						candiModel.setReasonInactive(ReasonInactive.Hidden);
						candiModel.getZoneStateNext().setZone(mZoneInactive);
						candiModel.getZoneStateNext().setStatus(ZoneStatus.Normal);
						mZoneInactive.getCandiesNext().add(candiModel);
					}
					else {
						candiModel.getViewStateNext().setVisible(true);
						candiModel.setReasonInactive(ReasonInactive.None);
						candiModel.getZoneStateNext().setStatus(ZoneStatus.Normal);
						candiModel.setTouchAreaActive(true);
					}
				}
			}
			/*
			 * If the model with the current focus is going to be hidden/gone then we
			 * will need a new focus.
			 */
			if (candiModel == mCandiModelFocused && !candiModel.getViewStateNext().isVisible()) {
				mCandiModelFocused = null;
			}
		}
	}

	public void updateZonesNext(Navigation navigation, boolean chunking) {

		/* Clear candies from zones that already exist (new ones might be created later). */
		for (ZoneModel zoneModel : mZoneModels) {
			zoneModel.getCandiesNext().clear();
		}

		/* Assign to zones */
		int focusedZoneIndex = 0;
		if (mCandiModelFocused != null) {
			focusedZoneIndex = mCandiModelFocused.getZoneStateCurrent().getZone().getZoneIndex();
		}

		int visibleCandiNextCount = ((CandiModel) mCandiRootNext).visibleChildrenNextCount();

		int zoneIndex = 0;
		if (visibleCandiNextCount < (focusedZoneIndex + 1)) {
			zoneIndex = (focusedZoneIndex + 1) - visibleCandiNextCount;
			zoneIndex++;
		}

		if (navigation == Navigation.Down && mCandiModelFocused != null) {
			CandiModel candiRootNext = (CandiModel) mCandiRootNext;
			synchronized (candiRootNext.getChildren()) {
				if (candiRootNext.getChildren().size() > focusedZoneIndex) {
					CandiModel childModel = (CandiModel) ((CandiModel) mCandiRootNext).getChildren().get(focusedZoneIndex);
					if (childModel.getEntity().type == CandiConstants.TYPE_CANDI_COMMAND) {
						zoneIndex++;
					}
				}
			}
		}
		else if (chunking) {
			/* We want the starting zone index to remain unchanged. */
			CandiModel candiModelFirst = (CandiModel) ((CandiModel) mCandiRootNext).getChildren().get(0);
			zoneIndex = candiModelFirst.getZoneStateCurrent().getZone().getZoneIndex();
		}

		for (IModel model : mCandiRootNext.getChildren()) {

			CandiModel candiModel = (CandiModel) model;
			candiModel.setChanged();
			if (candiModel.getViewStateNext().isVisible()) {

				/* Allocate new zone if needed */
				if (mZoneModels.size() < zoneIndex + 1) {
					mZoneModels.add(new ZoneModel(zoneIndex, this));
				}

				/* Hookup */
				ZoneModel zone = mZoneModels.get(zoneIndex);
				zone.getCandiesNext().add(candiModel);
				candiModel.getZoneStateNext().setZone(zone);

				/*
				 * Special treatment for zone one. If the user is currently focused on a
				 * candi in zone one we make sure their focus stays on whatever candi
				 * ends up in zone one next. Does not apply if we are drilling in.
				 */
				if (zoneIndex == 0 && focusedZoneIndex == 0 && navigation == Navigation.None) {
					mCandiModelFocused = candiModel;
				}

				/* Hookup any children */
				if (!candiModel.hasVisibleChildrenNext()) {
					candiModel.getZoneStateNext().setStatus(ZoneStatus.Normal);
				}
				else {
					candiModel.getZoneStateNext().setStatus(ZoneStatus.Primary);

					/* We might have children that are hidden even though the parent isn't. */
					for (IModel childModel : candiModel.getChildren()) {
						CandiModel childCandiModel = (CandiModel) childModel;
						childCandiModel.setChanged();

						if (childCandiModel.getViewStateNext().isVisible()) {
							zone.getCandiesNext().add(childCandiModel);
							childCandiModel.getZoneStateNext().setStatus(ZoneStatus.Secondary);
							childCandiModel.getZoneStateNext().setZone(zone);
						}
					}
				}
				zoneIndex++;
			}
		}

		/*
		 * If navigation is down and focusedZone now has command candi then shift everything one zone to the right
		 */

		/*
		 * If needed, move the candi model with the current focus back to the
		 * slot the user is currently looking at.
		 */
		boolean swappingEnabled = false;
		if (chunking == false && (mCandiModelFocused == null || (focusedZoneIndex == 0 && navigation != Navigation.None) || focusedZoneIndex != 0)) {
			swappingEnabled = true;
		}

		if (swappingEnabled) {
			if (!mCandiModelFocused.getZoneStateCurrent().getZone().isInactive() && !mCandiModelFocused.getZoneStateNext().getZone().isInactive()) {
				if (mCandiModelFocused.getZoneStateCurrent().getZone().getZoneIndex() != mCandiModelFocused.getZoneStateNext().getZone()
						.getZoneIndex()) {

					ZoneModel zoneNextOld = mCandiModelFocused.getZoneStateNext().getZone();
					ZoneModel zoneNextNew = mZoneModels.get(mCandiModelFocused.getZoneStateCurrent().getZone().getZoneIndex());
					ArrayList<CandiModel> candiModelsTemp = new ArrayList<CandiModel>();

					/* Move out the old tenants */
					for (CandiModel modelTemp : zoneNextNew.getCandiesNext()) {
						candiModelsTemp.add(modelTemp);
					}
					zoneNextNew.getCandiesNext().clear();

					/* Move in the new */
					for (CandiModel modelTemp : zoneNextOld.getCandiesNext()) {
						zoneNextNew.getCandiesNext().add(modelTemp);
						modelTemp.getZoneStateNext().setZone(zoneNextNew);
					}
					zoneNextOld.getCandiesNext().clear();

					/* Move old tenents into vacated zone */
					for (CandiModel modelTemp : candiModelsTemp) {
						zoneNextOld.getCandiesNext().add(modelTemp);
						modelTemp.getZoneStateNext().setZone(zoneNextOld);
					}
				}
			}
		}

		/* Show zone UI if we have multiple candies */
		for (ZoneModel zoneModel : mZoneModels) {
			zoneModel.setChanged();

			if (zoneModel.getCandiesNext().size() == 0) {
				zoneModel.getViewStateNext().setVisible(false);
			}
			else if (zoneModel.getCandiesNext().size() == 1) {
				if (zoneModel.getCandiesNext().get(0).countObservers() == 0) {
					zoneModel.getViewStateNext().setVisible(true);
				}
				else {
					zoneModel.getViewStateNext().setVisible(false);
				}
			}
			else {
				zoneModel.getViewStateNext().setVisible(true);
			}
		}

		/* Check to see if any zones have overflow */
		for (CandiModel candiModel : mCandiModels) {
			candiModel.getZoneStateNext().setOverflow(false);
			if (!candiModel.getZoneStateNext().getZone().isInactive()) {

				int maxVisible = ZoneModel.ZONE_CHILDREN_MAX_VISIBLE;
				if (candiModel.getZoneStateNext().getZone().getCandiesNext().size() > 0) {
					if (candiModel.getZoneStateNext().getZone().getCandiesNext().get(0).getZoneStateNext().getStatus() != ZoneStatus.Normal) {
						maxVisible = ZoneModel.ZONE_CHILDREN_MAX_VISIBLE_WITH_PRIMARY;
					}
				}

				if (candiModel.getZoneStateNext().getZone().getCandiIndexNext(candiModel) > (maxVisible - 1)) {

					/* Overflow get assigned to the inactive zone too. */
					candiModel.getZoneStateNext().setOverflow(true);
					candiModel.getViewStateNext().setVisible(false);
					candiModel.setTouchAreaActive(false);
					candiModel.setReasonInactive(ReasonInactive.Navigation);
					candiModel.getZoneStateNext().setZone(mZoneInactive);
					mZoneInactive.getCandiesNext().add(candiModel);
				}
			}
		}
		setChanged();
	}

	public void updatePositionsNext() {

		/* Assign next positions */
		for (CandiModel candiModel : mCandiModels) {
			Position positionNext = candiModel.getZoneStateNext().getZone().getChildPositionNext(candiModel);
			candiModel.getZoneStateNext().setAlignment(positionNext.rowLast ? ZoneAlignment.Bottom : ZoneAlignment.None);
			candiModel.getViewStateNext().setX(positionNext.x);
			candiModel.getViewStateNext().setY(positionNext.y);
			candiModel.getViewStateNext().setScale(positionNext.scale);
		}
	}

	public void updateMiscNext() {

		/* Zone titling */
		for (ZoneModel zoneModel : mZoneModels) {
			if (zoneModel.getCandiesNext().size() > 1) {
				for (CandiModel candiModel : zoneModel.getCandiesNext()) {
					if (candiModel.getZoneStateNext().getStatus() == ZoneStatus.Primary) {
						zoneModel.setTitleText(candiModel.getTitleText());
						break;
					}
				}
			}
			else if (zoneModel.getCandiesNext().size() == 1) {
				/* We don't need the title if the candi model has a candi view */
				CandiModel candiModel = zoneModel.getCandiesNext().get(0);
				if (candiModel.countObservers() == 0) {
					zoneModel.setTitleText(zoneModel.getCandiesNext().get(0).getTitleText());
				}
			}
		}

		for (IModel model : mCandiRootNext.getChildren()) {
			CandiModel candiModel = (CandiModel) model;
			if (candiModel.getViewStateNext().isVisible()) {
				candiModel.getViewStateNext().setHasReflection(candiModel.getZoneStateNext().getAlignment() == ZoneAlignment.Bottom);
				candiModel.getViewStateNext().setCollapsed(candiModel.getViewStateNext().getScale() != CandiPatchPresenter.SCALE_NORMAL);

				/* Include any children */
				for (IModel childModel : candiModel.getChildren()) {
					CandiModel childCandiModel = (CandiModel) childModel;
					childCandiModel.getViewStateNext().setHasReflection(childCandiModel.getZoneStateNext().getAlignment() == ZoneAlignment.Bottom);
					childCandiModel.getViewStateNext()
							.setCollapsed(childCandiModel.getViewStateNext().getScale() != CandiPatchPresenter.SCALE_NORMAL);
				}
			}
		}
	}

	public void sortCandiModels(List list) {
		Collections.sort(list, new Entity.SortEntitiesByDiscoveryTimeModifiedDate());
	}

	public ZoneModel getZoneNeighbor(ZoneModel targetZoneModel, boolean forward) {
		int targetIndex = targetZoneModel.getZoneIndex();
		for (ZoneModel zone : mZoneModels) {
			if (zone.getCandiesCurrent().size() > 0) {
				if (Math.abs(zone.getZoneIndex() - targetIndex) == 1) {
					if (forward && zone.getZoneIndex() > targetIndex)
						return zone;
					else if (!forward && zone.getZoneIndex() < targetIndex)
						return zone;
				}
			}
		}
		return null;
	}

	public int getZonesOccupiedNextCount() {
		int zoneOccupiedCount = 0;
		for (ZoneModel zone : mZoneModels)
			if (zone.getCandiesNext().size() > 0)
				zoneOccupiedCount++;
		return zoneOccupiedCount;
	}

	public int getZonesOccupiedCurrentCount() {
		int zoneOccupiedCount = 0;
		for (ZoneModel zone : mZoneModels)
			if (zone.getCandiesCurrent().size() > 0)
				zoneOccupiedCount++;
		return zoneOccupiedCount;
	}

	public boolean containsCandiModel(CandiModel candiModel) {
		boolean contains = mCandiModels.containsKey(String.valueOf(candiModel.getModelId()));
		return contains;
	}

	public boolean hasCandiModelForEntity(String entityId) {
		for (CandiModel candiModel : mCandiModels) {
			if (candiModel.getEntity().id.equals(entityId))
				return true;
		}
		return false;
	}

	public CandiModel getCandiModelForEntity(String entityId) {
		for (CandiModel candiModel : mCandiModels) {
			if (candiModel.getEntity().id.equals(entityId))
				return candiModel;
		}
		return null;
	}

	public CandiList<CandiModel> getCandiModels() {
		return mCandiModels;
	}

	public List<ZoneModel> getZones() {
		return mZoneModels;
	}

	public CandiModel getCandiModelFocused() {
		return mCandiModelFocused;
	}

	public CandiModel getCandiModelSelected() {
		return mCandiModelSelected;
	}

	public IModel getCandiRootCurrent() {
		return mCandiRootCurrent;
	}

	public IModel getCandiRootNext() {
		return mCandiRootNext;
	}

	public float getOriginX() {
		return mOriginX;
	}

	public float getOriginY() {
		return mOriginY;
	}

	public void setCandiModelFocused(CandiModel candiFocused) {
		if (mCandiModelFocused != candiFocused) {
			if (candiFocused != null) {
				Logger.d(null, "Changing candi model focus: " + candiFocused.getTitleText());
			}
			else {
				Logger.d(null, "Changing candi model focus: null");
			}
		}
		mCandiModelFocused = candiFocused;
	}

	public void setCandiModelSelected(CandiModel candiSelected) {
		mCandiModelSelected = candiSelected;
	}

	public void setOriginX(float originX) {
		mOriginX = originX;
	}

	public void setOriginY(float originY) {
		mOriginY = originY;
	}

	public void setCandiRootCurrent(IModel candiRootCurrent) {
		mCandiRootCurrent = candiRootCurrent;
	}

	public void setCandiRootNext(IModel candiRootNext) {
		mCandiRootNext = candiRootNext;
	}

	public void setScreenWidth(int screenWidth) {
		this.mScreenWidth = screenWidth;
	}

	public int getScreenWidth() {
		return mScreenWidth;
	}

	public static enum Navigation {
		Up,
		Down,
		None
	}

}
