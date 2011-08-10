package com.proxibase.aircandi.candi.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Observable;

import com.proxibase.aircandi.candi.models.CandiModel.ReasonInactive;
import com.proxibase.aircandi.utils.CandiList;
import com.proxibase.sdk.android.proxi.consumer.Beacon;

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
	private IModel						mCandiRootPrevious;
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
		this.mZoneModels.clear();
		this.mCandiModels.clear();
		this.mCandiModelFocused = null;
		this.mCandiModelSelected = null;
		this.mCandiRootCurrent = null;
		this.mCandiRootNext = null;
		super.setChanged();
	}

	public void shiftToNext() {

		this.mCandiRootCurrent = this.mCandiRootNext;

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
	 * Updates if the model exists otherwise but never fails
	 * 
	 * @param candiModel
	 */
	public CandiModel updateCandiModel(CandiModel candiModel) {
		return doUpdateCandiModel(candiModel);
	}

	/**
	 * Updates if the model exists otherwise but never fails
	 * 
	 * @param candiModel
	 */
	public void updateCandiModels(List<CandiModel> candiModels) {
		for (CandiModel candiModel : candiModels) {
			doUpdateCandiModel(candiModel);
		}
	}

	public void updateZones() {

		// Reset zones
		mZoneInactive.getCandiesNext().clear();
		for (ZoneModel zoneModel : mZoneModels) {
			zoneModel.getCandiesNext().clear();
			zoneModel.setCandiModelPrimaryNext(null);
			zoneModel.setVisibleNext(false);
		}

		// Manage visibility
		if (!mCandiRootNext.isSuperRoot()) {
			for (CandiModel candiModel : mCandiModels)
				if (!mCandiRootNext.getChildren().containsKey(candiModel.getModelId())) {
					candiModel.setVisibleNext(false);
					candiModel.setTouchAreaActive(false);
					candiModel.setReasonInactive(ReasonInactive.Navigation);
					candiModel.setZoneNext(mZoneInactive);
					mZoneInactive.getCandiesNext().add(candiModel);
				}
				else {
					candiModel.setReasonInactive(ReasonInactive.None);
					if (candiModel.getEntityProxy().isHidden) {
						candiModel.setVisibleNext(false);
						candiModel.setTouchAreaActive(false);
					}
					else {
						candiModel.setVisibleNext(true);
						candiModel.setTouchAreaActive(true);
					}
				}
		}
		else {
			// Restore 'normal' visibility
			for (CandiModel candiModel : mCandiModels) {
				candiModel.setReasonInactive(ReasonInactive.None);
				if (candiModel.getEntityProxy().isHidden) {
					candiModel.setVisibleNext(false);
					candiModel.setTouchAreaActive(false);
				}
				else {
					candiModel.setVisibleNext(true);
					candiModel.setTouchAreaActive(true);
				}
			}
		}

		// Assign to zones
		int zoneIndex = 0;
		for (IModel model : mCandiRootNext.getChildren()) {

			CandiModel candiModel = (CandiModel) model;

			if (candiModel.isVisibleNext()) {

				candiModel.setReasonInactive(ReasonInactive.None);
				if (mZoneModels.size() < zoneIndex + 1)
					mZoneModels.add(new ZoneModel(zoneIndex, this));

				ZoneModel zone = mZoneModels.get(zoneIndex);

				if (candiModel.getChildren().size() == 0) {
					zone.getCandiesNext().add(candiModel);
					candiModel.setZoneNext(zone);
					candiModel.setChanged();
				}
				else {
					zone.getCandiesNext().add(candiModel);
					zone.setCandiModelPrimaryNext(candiModel);
					candiModel.setZoneNext(zone);
					candiModel.setChanged();
					for (IModel childModel : candiModel.getChildren()) {
						CandiModel childCandiModel = (CandiModel) childModel;
						zone.getCandiesNext().add(childCandiModel);
						childCandiModel.setZoneNext(zone);
						childCandiModel.setChanged();
					}
				}
				zoneIndex++;
			}
			else {
				// Candi models that won't be visible are assigned to the special inactive zone
				candiModel.setReasonInactive(ReasonInactive.Hidden);
				candiModel.setZoneNext(mZoneInactive);
				mZoneInactive.getCandiesNext().add(candiModel);
				candiModel.setChanged();

				for (IModel childModel : candiModel.getChildren()) {
					CandiModel childCandiModel = (CandiModel) childModel;
					mZoneInactive.getCandiesNext().add(childCandiModel);
					childCandiModel.setZoneNext(mZoneInactive);
					childCandiModel.setChanged();
				}
			}
		}

		// If needed, move the candi model with the current focus back to the
		// slot the user is currently looking at.
		if (mCandiModelFocused != null) {
			if (!mCandiModelFocused.getZoneCurrent().isInactive() && !mCandiModelFocused.getZoneNext().isInactive())
				if (mCandiModelFocused.getZoneCurrent().getZoneIndex() != mCandiModelFocused.getZoneNext().getZoneIndex()) {

					// Moved zones because current zone transitioned to unused.
					if (mCandiModelFocused.getZoneCurrent().getZoneIndex() + 1 > this.getZonesOccupiedNextCount()) {
					}
					else {
						ZoneModel zoneNextOld = mCandiModelFocused.getZoneNext();
						ZoneModel zoneNextNew = mZoneModels.get(mCandiModelFocused.getZoneCurrent().getZoneIndex());
						ArrayList<CandiModel> candiModelsTemp = new ArrayList<CandiModel>();
						CandiModel candiModelPrimaryTemp;

						// Move out the old tenants
						for (CandiModel modelTemp : zoneNextNew.getCandiesNext()) {
							candiModelsTemp.add(modelTemp);
						}
						candiModelPrimaryTemp = zoneNextNew.getCandiModelPrimaryNext();
						zoneNextNew.getCandiesNext().clear();

						// Move in the new
						for (CandiModel modelTemp : zoneNextOld.getCandiesNext()) {
							zoneNextNew.getCandiesNext().add(modelTemp);
							modelTemp.setZoneNext(zoneNextNew);
						}
						zoneNextNew.setCandiModelPrimaryNext(zoneNextOld.getCandiModelPrimaryNext());
						zoneNextOld.getCandiesNext().clear();

						// Move old tenents into vacated zone
						for (CandiModel modelTemp : candiModelsTemp) {
							zoneNextOld.getCandiesNext().add(modelTemp);
							modelTemp.setZoneNext(zoneNextOld);
						}
						zoneNextOld.setCandiModelPrimaryNext(candiModelPrimaryTemp);
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
				if (candiModel.getZoneNext().getCandiModelPrimaryNext() != null)
					maxVisible = ZoneModel.ZONE_CHILDREN_MAX_VISIBLE_WITH_PRIMARY;

				if (candiModel.getZoneNext().getCandiIndexNext(candiModel) > (maxVisible - 1)) {
					candiModel.setVisibleNext(false);
					candiModel.setOverflowNext(true);
					candiModel.setTouchAreaActive(false);
				}
			}
		}

		super.setChanged();
	}

	private CandiModel doUpdateCandiModel(CandiModel candiModelUpdate) {
		/*
		 * This only gets called when doing a partial update and a candi model already exists. A partial
		 * update only pulls in entities for new beacons but does not pick up service side changes for the
		 * entitis for old beacons. There can however be local changes to existing entities which include
		 * hidden status based on signal fencing and visibleTime property.
		 */
		if (!mCandiModels.containsKey(candiModelUpdate.getEntityProxy().id))
			return null;

		CandiModel candiModelManaged = mCandiModels.getByKey(candiModelUpdate.getEntityProxy().id);
		candiModelManaged.getEntityProxy().isHidden = candiModelUpdate.getEntityProxy().isHidden;
		candiModelManaged.getEntityProxy().visibleTime = candiModelUpdate.getEntityProxy().visibleTime;

		if (candiModelManaged.getReasonInactive() != ReasonInactive.Navigation) {
			if (candiModelUpdate.getEntityProxy().isHidden)
				candiModelManaged.setVisibleNext(false);
			else
				candiModelManaged.setVisibleNext(true);
		}

		if (candiModelManaged.isVisibleCurrent())
			candiModelManaged.setRookie(false);

		candiModelManaged.setDisplayExtra(candiModelUpdate.getDisplayExtra());

		Beacon beaconManaged = candiModelManaged.getEntityProxy().beacon;
		Beacon beaconUpdate = candiModelUpdate.getEntityProxy().beacon;

		beaconManaged.detectedLastPass = beaconUpdate.detectedLastPass;
		beaconManaged.levelDb = beaconUpdate.levelDb;
		beaconManaged.scanMisses = beaconUpdate.scanMisses;
		beaconManaged.scanPasses = beaconUpdate.scanPasses;

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
		Collections.sort(mCandiModels, new SortEntitiesByVisibleTime());
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
		boolean contains = mCandiModels.containsKey(candiModel.getModelId());
		return contains;
	}

	public CandiList<CandiModel> getCandiModels() {
		return this.mCandiModels;
	}

	public List<ZoneModel> getZones() {
		return this.mZoneModels;
	}

	public CandiModel getCandiModelFocused() {
		return this.mCandiModelFocused;
	}

	public CandiModel getCandiModelSelected() {
		return this.mCandiModelSelected;
	}

	public IModel getCandiRootCurrent() {
		return this.mCandiRootCurrent;
	}

	public IModel getCandiRootNext() {
		return this.mCandiRootNext;
	}

	public IModel getCandiRootPrevious() {
		return mCandiRootPrevious;
	}

	public float getOriginX() {
		return this.mOriginX;
	}

	public float getOriginY() {
		return this.mOriginY;
	}

	public void setCandiModelFocused(CandiModel candiFocused) {
		this.mCandiModelFocused = candiFocused;
		super.setChanged();
	}

	public void setCandiModelSelected(CandiModel candiSelected) {
		this.mCandiModelSelected = candiSelected;
		super.setChanged();
	}

	public void setOriginX(float originX) {
		this.mOriginX = originX;
		super.setChanged();
	}

	public void setOriginY(float originY) {
		this.mOriginY = originY;
		super.setChanged();
	}

	public void setCandiRootCurrent(IModel candiRootCurrent) {
		this.mCandiRootCurrent = candiRootCurrent;
	}

	public void setCandiRootNext(IModel candiRootNext) {
		this.mCandiRootNext = candiRootNext;
	}

	public void setCandiRootPrevious(IModel candiRootPrevious) {
		this.mCandiRootPrevious = candiRootPrevious;
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
				if ((object2.getEntityProxy().discoveryTime.getTime() / 100) - (object1.getEntityProxy().discoveryTime.getTime() / 100) < 0)
					return -1;
				else if ((object2.getEntityProxy().discoveryTime.getTime() / 100) - (object1.getEntityProxy().discoveryTime.getTime() / 100) > 0)
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

	class SortEntitiesByVisibleTime implements Comparator<CandiModel> {

		@Override
		public int compare(CandiModel object1, CandiModel object2) {
			if (!object1.getEntityProxy().beacon.isUnregistered && object2.getEntityProxy().beacon.isUnregistered)
				return -1;
			else if (!object2.getEntityProxy().beacon.isUnregistered && object1.getEntityProxy().beacon.isUnregistered)
				return 1;
			else {
				if ((object2.getEntityProxy().visibleTime.getTime() / 100) - (object1.getEntityProxy().visibleTime.getTime() / 100) < 0)
					return -1;
				else if ((object2.getEntityProxy().visibleTime.getTime() / 100) - (object1.getEntityProxy().visibleTime.getTime() / 100) > 0)
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
