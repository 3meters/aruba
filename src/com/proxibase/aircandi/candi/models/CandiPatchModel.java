package com.proxibase.aircandi.candi.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Observable;

import com.proxibase.aircandi.candi.models.CandiModel.DisplayExtra;
import com.proxibase.aircandi.candi.models.CandiModel.ReasonInactive;
import com.proxibase.aircandi.candi.models.ZoneModel.ZoneStatus;
import com.proxibase.aircandi.utils.CandiList;
import com.proxibase.aircandi.utils.ImageManager.ImageFormat;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;

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

	public CandiPatchModel() {
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

		// The super class only updates observers if hasChanged == true.
		// Super class also handles clearChanged.
		super.notifyObservers();

		// Each submodel will notify it's observers only if it has been flagged as changed.
		for (IModel candi : mCandiModels)
			candi.update();

		for (ZoneModel zone : mZoneModels)
			zone.update();
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

		// Only assign to a zone if the model is visible
		if (candiModel.getEntityProxy().isHidden) {
			candiModel.setVisibleCurrent(false);
			candiModel.setVisibleNext(false);
		}
		else {
			candiModel.setVisibleCurrent(false);
			candiModel.setVisibleNext(true);
		}

		candiModel.setReasonInactive(ReasonInactive.New);
		mZoneInactive.getCandiesCurrent().add(candiModel);
		mZoneInactive.getCandiesNext().add(candiModel);

		candiModel.setZoneCurrent(mZoneInactive);
		candiModel.setZoneNext(mZoneInactive);

		super.setChanged();
	}

	/**
	 * Updates if the model exists but never fails
	 */
	public CandiModel updateCandiModel(EntityProxy entityProxy, DisplayExtra displayExtra) {
		return doUpdateCandiModel(entityProxy, displayExtra);
	}

	public void updateZones() {

		// Reset zones
		mZoneInactive.getCandiesNext().clear();
		for (ZoneModel zoneModel : mZoneModels) {
			zoneModel.getCandiesNext().clear();
			zoneModel.setVisibleNext(false);
		}

		// Clear touch areas
		for (CandiModel candiModel : mCandiModels) {
			candiModel.setTouchAreaActive(false);
		}

		// Manage visibility
		if (!mCandiRootNext.isSuperRoot()) {
			for (CandiModel candiModel : mCandiModels)
				if (candiModel.getReasonInactive() == ReasonInactive.Deleting) {
					candiModel.setVisibleNext(false);
					candiModel.setZoneNext(mZoneInactive);
					mZoneInactive.getCandiesNext().add(candiModel);
				}
				else {
					if (!mCandiRootNext.getChildren().containsKey(String.valueOf(candiModel.getModelId()))) {
						candiModel.setVisibleNext(false);
						candiModel.setReasonInactive(ReasonInactive.Navigation);
						candiModel.setZoneNext(mZoneInactive);
						mZoneInactive.getCandiesNext().add(candiModel);
					}
					else {
						candiModel.setReasonInactive(ReasonInactive.None);
						if (candiModel.getEntityProxy().isHidden) {
							candiModel.setVisibleNext(false);
						}
						else {
							candiModel.setVisibleNext(true);
							candiModel.setTouchAreaActive(true);
						}
					}
				}
		}
		else {
			// Restore 'normal' visibility
			for (CandiModel candiModel : mCandiModels) {
				if (candiModel.getReasonInactive() == ReasonInactive.Deleting) {
					candiModel.setVisibleNext(false);
					candiModel.setZoneNext(mZoneInactive);
					mZoneInactive.getCandiesNext().add(candiModel);
				}
				else {
					if (candiModel.getReasonInactive() != ReasonInactive.Deleting) {
						candiModel.setReasonInactive(ReasonInactive.None);
						if (candiModel.getEntityProxy().isHidden) {
							candiModel.setVisibleNext(false);
						}
						else {
							candiModel.setVisibleNext(true);
							candiModel.setTouchAreaActive(true);
						}
					}
				}
			}
		}

		// Assign to zones
		int focusedZoneIndex = 0;
		if (mCandiModelFocused != null) {
			focusedZoneIndex = mCandiModelFocused.getZoneCurrent().getZoneIndex();
		}

		int visibleCandiNext = ((CandiModel) mCandiRootNext).visibleChildrenNext();

		int zoneIndex = 0;
		if (visibleCandiNext < (focusedZoneIndex + 1)) {
			zoneIndex = (focusedZoneIndex + 1) - visibleCandiNext;
			zoneIndex++;
		}

		for (IModel model : mCandiRootNext.getChildren()) {

			CandiModel candiModel = (CandiModel) model;

			if (candiModel.isVisibleNext()) {

				candiModel.setReasonInactive(ReasonInactive.None);
				if (mZoneModels.size() < zoneIndex + 1)
					mZoneModels.add(new ZoneModel(zoneIndex, this));

				ZoneModel zone = mZoneModels.get(zoneIndex);

				if (!candiModel.hasVisibleChildrenNext()) {
					zone.getCandiesNext().add(candiModel);
					candiModel.setZoneStatusNext(ZoneStatus.Normal);
					candiModel.setZoneNext(zone);
					candiModel.setChanged();

					/*
					 * Still might have children that will be hidden and
					 * should be assigned to the inactive zone
					 */
					for (IModel childModel : candiModel.getChildren()) {
						CandiModel childCandiModel = (CandiModel) childModel;
						childCandiModel.setReasonInactive(ReasonInactive.Hidden);
						childCandiModel.setZoneNext(mZoneInactive);
						childCandiModel.setZoneStatusNext(ZoneStatus.Normal);
						mZoneInactive.getCandiesNext().add(childCandiModel);
						childCandiModel.setChanged();
					}
				}
				else {
					zone.getCandiesNext().add(candiModel);
					candiModel.setZoneNext(zone);
					candiModel.setChanged();
					candiModel.setZoneStatusNext(ZoneStatus.Primary);

					/*
					 * We might have children that are hidden even though
					 * the parent isn't.
					 */
					for (IModel childModel : candiModel.getChildren()) {
						CandiModel childCandiModel = (CandiModel) childModel;

						if (childCandiModel.isVisibleNext()) {
							zone.getCandiesNext().add(childCandiModel);
							childCandiModel.setZoneStatusNext(ZoneStatus.Secondary);
							childCandiModel.setZoneNext(zone);
							childCandiModel.setChanged();
						}
						else {
							childCandiModel.setReasonInactive(ReasonInactive.Hidden);
							childCandiModel.setZoneNext(mZoneInactive);
							childCandiModel.setZoneStatusNext(ZoneStatus.Normal);
							mZoneInactive.getCandiesNext().add(childCandiModel);
							childCandiModel.setChanged();
						}
					}
				}
				zoneIndex++;
			}
			else {
				// Candi models that won't be visible are assigned to the special inactive zone
				candiModel.setReasonInactive(ReasonInactive.Hidden);
				candiModel.setZoneNext(mZoneInactive);
				candiModel.setZoneStatusNext(ZoneStatus.Normal);
				mZoneInactive.getCandiesNext().add(candiModel);
				candiModel.setChanged();

				for (IModel childModel : candiModel.getChildren()) {
					CandiModel childCandiModel = (CandiModel) childModel;
					mZoneInactive.getCandiesNext().add(childCandiModel);
					childCandiModel.setZoneNext(mZoneInactive);
					childCandiModel.setZoneStatusNext(ZoneStatus.Normal);
					childCandiModel.setChanged();
				}
			}
		}

		// If needed, move the candi model with the current focus back to the
		// slot the user is currently looking at.
		if (mCandiModelFocused != null) {
			if (!mCandiModelFocused.getZoneCurrent().isInactive() && !mCandiModelFocused.getZoneNext().isInactive())
				if (mCandiModelFocused.getZoneCurrent().getZoneIndex() != mCandiModelFocused.getZoneNext().getZoneIndex()) {

					ZoneModel zoneNextOld = mCandiModelFocused.getZoneNext();
					ZoneModel zoneNextNew = mZoneModels.get(mCandiModelFocused.getZoneCurrent().getZoneIndex());
					ArrayList<CandiModel> candiModelsTemp = new ArrayList<CandiModel>();

					// Move out the old tenants
					for (CandiModel modelTemp : zoneNextNew.getCandiesNext()) {
						candiModelsTemp.add(modelTemp);
					}
					zoneNextNew.getCandiesNext().clear();

					// Move in the new
					for (CandiModel modelTemp : zoneNextOld.getCandiesNext()) {
						zoneNextNew.getCandiesNext().add(modelTemp);
						modelTemp.setZoneNext(zoneNextNew);
					}
					zoneNextOld.getCandiesNext().clear();

					// Move old tenents into vacated zone
					for (CandiModel modelTemp : candiModelsTemp) {
						zoneNextOld.getCandiesNext().add(modelTemp);
						modelTemp.setZoneNext(zoneNextOld);
					}
				}
		}

		// Show zone UI if we have multiple candies
		for (ZoneModel zoneModel : mZoneModels) {
			if (zoneModel.getCandiesNext().size() > 1)
				zoneModel.setVisibleNext(true);
		}

		// Check to see if any zones have overflow
		for (CandiModel candiModel : mCandiModels) {
			candiModel.setOverflowNext(false);
			if (!candiModel.getZoneNext().isInactive()) {

				int maxVisible = ZoneModel.ZONE_CHILDREN_MAX_VISIBLE;
				if (candiModel.getZoneNext().getCandiesNext().get(0).getZoneStatusNext() != ZoneStatus.Normal)
					maxVisible = ZoneModel.ZONE_CHILDREN_MAX_VISIBLE_WITH_PRIMARY;

				if (candiModel.getZoneNext().getCandiIndexNext(candiModel) > (maxVisible - 1)) {
					candiModel.setOverflowNext(true);
					candiModel.setVisibleNext(false);
					candiModel.setTouchAreaActive(false);
					candiModel.setReasonInactive(ReasonInactive.Navigation);
					candiModel.setZoneNext(mZoneInactive);
					mZoneInactive.getCandiesNext().add(candiModel);
				}
			}
		}

		// Assign next positions
		for (CandiModel candiModel : mCandiModels) {
			candiModel.setPositionNext(candiModel.getZoneNext().getChildPositionNext(candiModel));
		}

		super.setChanged();
	}

	private CandiModel doUpdateCandiModel(EntityProxy entityProxy, DisplayExtra displayExtra) {
		/*
		 * This only gets called when doing a partial update and a candi model already exists. A partial
		 * update only pulls in entities for new beacons but does not pick up service side changes for the
		 * entitis for old beacons. There can however be local changes to existing entities which include
		 * hidden status based on signal fencing and visibleTime property.
		 */

		/*
		 * Stuff that could have been updated
		 * - Any property of the entity proxy
		 * - Entity properties that propogate up to the model
		 * - Beacon properties (these get pulled in with the entity)
		 */

		// Transfer the entire entity proxy
		CandiModel candiModelManaged = mCandiModels.getByKey(String.valueOf(entityProxy.id));
		candiModelManaged.setEntityProxy(entityProxy);
		candiModelManaged.setTitleText(entityProxy.label);
		candiModelManaged.setBodyImageId(entityProxy.imageUri);
		candiModelManaged.setBodyImageUri(entityProxy.imageUri);
		candiModelManaged.setBodyImageFormat(entityProxy.imageFormat.equals("html") ? ImageFormat.Html : ImageFormat.Binary);

		if (candiModelManaged.getReasonInactive() != ReasonInactive.Navigation) {
			if (entityProxy.isHidden)
				candiModelManaged.setVisibleNext(false);
			else
				candiModelManaged.setVisibleNext(true);
		}

		if (candiModelManaged.isVisibleCurrent())
			candiModelManaged.setRookie(false);

		candiModelManaged.setDisplayExtra(displayExtra);

		// If transitioning from hidden to visible, it might not have a zone yet
		if (candiModelManaged.isVisibleNext() && candiModelManaged.getZoneCurrent().isInactive()) {

			ZoneModel zoneTarget = null;
			for (ZoneModel zone : mZoneModels)
				if (zone.getCandiesCurrent().size() == 0) {
					zoneTarget = zone;
					break;
				}

			if (zoneTarget == null) {
				zoneTarget = new ZoneModel(mZoneModels.size(), this);
				zoneTarget.setVisibleCurrent(false);
				zoneTarget.setVisibleNext(true);
				mZoneModels.add(zoneTarget);
			}

			zoneTarget.getCandiesCurrent().add(candiModelManaged);
			zoneTarget.getCandiesNext().add(candiModelManaged);

			candiModelManaged.setZoneCurrent(zoneTarget);
			candiModelManaged.setZoneNext(zoneTarget);
		}

		candiModelManaged.setChanged();
		return candiModelManaged;
	}

	public void sortCandiModels() {
		Collections.sort(mCandiModels, new SortEntitiesByDiscoveryTime());
		Collections.sort(mCandiModels, new SortEntitiesByEntityType());
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

	public boolean hasCandiModelForEntity(Integer entityId) {
		for (CandiModel candiModel : mCandiModels) {
			if (candiModel.getEntityProxy().id.equals(entityId))
				return true;
		}
		return false;
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
		mCandiModelFocused = candiFocused;
		super.setChanged();
	}

	public void setCandiModelSelected(CandiModel candiSelected) {
		mCandiModelSelected = candiSelected;
		super.setChanged();
	}

	public void setOriginX(float originX) {
		mOriginX = originX;
		super.setChanged();
	}

	public void setOriginY(float originY) {
		mOriginY = originY;
		super.setChanged();
	}

	public void setCandiRootCurrent(IModel candiRootCurrent) {
		mCandiRootCurrent = candiRootCurrent;
	}

	public void setCandiRootNext(IModel candiRootNext) {
		mCandiRootNext = candiRootNext;
	}

	public ZoneModel getZoneInactive() {
		return mZoneInactive;
	}

	public void setZoneInactive(ZoneModel zoneInactive) {
		mZoneInactive = zoneInactive;
	}

	class SortEntitiesByTagLevelDb implements Comparator<CandiModel> {

		@Override
		public int compare(CandiModel object1, CandiModel object2) {
			if (!object1.getEntityProxy().beacon.isUnregistered && object2.getEntityProxy().beacon.isUnregistered)
				return -1;
			else if (!object2.getEntityProxy().beacon.isUnregistered && object1.getEntityProxy().beacon.isUnregistered)
				return 1;
			else
				return object2.getEntityProxy().beacon.getAvgBeaconLevel() - object1.getEntityProxy().beacon.getAvgBeaconLevel();
		}
	}

	class SortEntitiesByDiscoveryTime implements Comparator<CandiModel> {

		@Override
		public int compare(CandiModel object1, CandiModel object2) {
			if (!object1.getEntityProxy().beacon.isUnregistered && object2.getEntityProxy().beacon.isUnregistered)
				return -1;
			else if (!object2.getEntityProxy().beacon.isUnregistered && object1.getEntityProxy().beacon.isUnregistered)
				return 1;
			else {
				if ((object2.getEntityProxy().beacon.discoveryTime.getTime() / 100) - (object1.getEntityProxy().beacon.discoveryTime.getTime() / 100) < 0)
					return -1;
				else if ((object2.getEntityProxy().beacon.discoveryTime.getTime() / 100) - (object1.getEntityProxy().beacon.discoveryTime.getTime() / 100) > 0)
					return 1;
				else {
					if (object2.getEntityProxy().label.compareToIgnoreCase(object1.getEntityProxy().label) < 0)
						return 1;
					else if (object2.getEntityProxy().label.compareToIgnoreCase(object1.getEntityProxy().label) > 0)
						return -1;
					else
						return 0;
				}
			}
		}
	}

	class SortEntitiesByEntityType implements Comparator<CandiModel> {

		@Override
		public int compare(CandiModel object1, CandiModel object2) {

			return object1.getEntityProxy().entityType.compareToIgnoreCase(object2.getEntityProxy().entityType);
		}
	}

}
