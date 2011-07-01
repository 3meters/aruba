package com.proxibase.aircandi.candi.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;

import com.proxibase.aircandi.utils.CandiList;
import com.proxibase.sdk.android.proxi.consumer.Beacon;
import com.proxibase.sdk.android.proxi.consumer.ProxiEntity;

/**
 * @author Jayma
 *         Inputs:
 *         patch rotation,
 *         zone ordering, zone membership, zone count, zone current/focus,
 *         camera position, scale
 */

public class CandiPatchModel extends Observable {

	private final CandiList<CandiModel>	mCandiModels		= new CandiList<CandiModel>();
	private final LinkedList<ZoneModel>	mZoneModels			= new LinkedList<ZoneModel>();

	private ZoneModel					mZoneInactive		= null;
	private CandiModel					mCandiModelFocused	= null;
	private CandiModel					mCandiModelSelected	= null;
	private IModel						mCandiRootCurrent;
	private IModel						mCandiRootNext;
	private IModel						mCandiRootPrevious;
	private float						mOriginX;
	private float						mOriginY;

	public CandiPatchModel() {}

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
		if (candiModel.getProxiEntity().isHidden) {
			candiModel.setVisibleCurrent(false);
			candiModel.setVisibleNext(false);
		}
		else {
			candiModel.setVisibleCurrent(false);
			candiModel.setVisibleNext(true);
		}

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
	public void updateCandiModel(CandiModel candiModel) {
		doUpdateCandiModel(candiModel);
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
			zoneModel.setVisibleNext(false);
		}

		// If switching root then we need to hide everything not at the current level
		if (!mCandiRootNext.isSuperRoot()) {
			for (CandiModel candiModel : mCandiModels)
				if (!mCandiRootNext.getChildren().contains(candiModel)) {
					candiModel.setVisibleNext(false);
					candiModel.setTouchAreaActive(false);
					candiModel.setZoneNext(mZoneInactive);
					mZoneInactive.getCandiesNext().add(candiModel);
				}
		}
		else {
			// Restore 'normal' visibility
			for (CandiModel candiModel : mCandiModels)
				if (candiModel.getProxiEntity().isHidden) {
					candiModel.setVisibleNext(false);
					candiModel.setTouchAreaActive(false);
				}
				else {
					candiModel.setVisibleNext(true);
					candiModel.setTouchAreaActive(true);
				}
		}

		// Re-assign to zones based on re-sorted collection
		int zoneIndex = 0;
		for (CandiModel candiModel : mCandiRootNext.getChildren()) {

			if (candiModel.isVisibleNext()) {

				if (mZoneModels.size() < zoneIndex + 1)
					mZoneModels.add(new ZoneModel(zoneIndex, this));

				ZoneModel zone = mZoneModels.get(zoneIndex);

				candiModel.setZoneNext(zone);
				candiModel.setChanged();

				if (candiModel.getChildren().size() == 0) {
					zone.getCandiesNext().add(candiModel);
				}
				else {
					zone.setTitleText(candiModel.getTitleText());
					for (CandiModel candiChild : candiModel.getChildren()) {
						zone.getCandiesNext().add(candiChild);
						candiChild.setZoneNext(zone);
						candiChild.setChanged();
					}
				}

				zoneIndex++;
			}
			else {

				// Candi models that won't be visible are not assigned to zones

				candiModel.setZoneNext(mZoneInactive);
				mZoneInactive.getCandiesNext().add(candiModel);
				candiModel.setChanged();

				for (CandiModel candiChild : candiModel.getChildren()) {
					mZoneInactive.getCandiesNext().add(candiChild);
					candiChild.setZoneNext(mZoneInactive);
					candiChild.setChanged();
				}
			}
		}

		// If needed, move the candi model with the current focus back to the
		// slot the user is currently looking at.
		if (mCandiModelFocused != null) {
			if (!mCandiModelFocused.getZoneCurrent().isInactive() && !mCandiModelFocused.getZoneNext().isInactive())
				if (mCandiModelFocused.getZoneCurrent().getZoneIndex() != mCandiModelFocused.getZoneNext().getZoneIndex()) {

					// Moved zones because current zone transitioned to unused.
					if (mCandiModelFocused.getZoneCurrent().getZoneIndex() > this.getZonesOccupiedNextCount()) {
					}
					else {
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
		}

		// Show zone UI if we have multiple candies
		for (ZoneModel zoneModel : mZoneModels) {
			if (zoneModel.getCandiesNext().size() > 1)
				zoneModel.setVisibleNext(true);
		}

		super.setChanged();
	}

	public void buildCandiTree() {
		/*
		 * Currently assumes the candiModel collection gets a primary sort by entitytype
		 */
		this.sortCandiModels();

		IModel candiRoot = new CandiModel();
		candiRoot.setRoot(true);

		int candiModelCount = mCandiModels.size();
		CandiModel candiGroup = null;

		for (int i = 0; i < candiModelCount; i++) {
			CandiModel candiModel = mCandiModels.get(i);

			// We have an active group
			if (candiGroup != null)
				if (candiModel.getProxiEntity().entityType.equals(candiGroup.getProxiEntity().entityType)) {
					candiModel.setGrouped(true);
					candiModel.setParent(candiGroup);
					candiGroup.getChildren().add(candiModel);
					continue;
				}
				else {
					candiRoot.getChildren().add(candiGroup);
					candiGroup = null;
				}

			// We don't have an active group
			if (i + 1 < candiModelCount) {

				// Activate a new group if this one and the next on are the same type
				if (candiModel.getProxiEntity().entityType.equals(mCandiModels.get(i + 1).getProxiEntity().entityType)) {

					ProxiEntity proxiEntity = new ProxiEntity();
					proxiEntity.entityType = candiModel.getProxiEntity().entityType;
					proxiEntity.label = candiModel.getProxiEntity().entityTypeLabel;

					candiGroup = CandiModelBuilder.createCandiModel(proxiEntity);
					candiGroup.setParent(candiRoot);
					candiModel.setGrouped(true);
					candiModel.setParent(candiGroup);
					candiGroup.getChildren().add(candiModel);
					continue;

				}
			}
			candiModel.setGrouped(false);
			candiModel.setParent(candiRoot);
			candiRoot.getChildren().add(candiModel);
		}

		this.mCandiRootNext = candiRoot;
	}

	private void doUpdateCandiModel(CandiModel candiModelUpdate) {
	
		if (mCandiModels.containsKey(candiModelUpdate.getProxiEntity().entityId)) {
	
			CandiModel candiModelManaged = mCandiModels.getByKey(candiModelUpdate.getProxiEntity().entityId);
	
			if (candiModelUpdate.getProxiEntity().isHidden)
				candiModelManaged.setVisibleNext(false);
			else
				candiModelManaged.setVisibleNext(true);
	
			candiModelManaged.setDisplayExtra(candiModelUpdate.getDisplayExtra());
	
			Beacon beaconManaged = candiModelManaged.getProxiEntity().beacon;
			Beacon beaconUpdate = candiModelUpdate.getProxiEntity().beacon;
	
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
		}
	}

	private void sortCandiModels() {
		Collections.sort(mCandiModels, new SortEntitiesByVisibleTime());
		Collections.sort(mCandiModels, new SortEntitiesByEntityType());
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
		boolean contains = mCandiModels.containsKey(candiModel.getProxiEntity().entityId);
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

	public ZoneModel getZoneInactive() {
		return mZoneInactive;
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

	public void setZoneInactive(ZoneModel zoneInactive) {
		this.mZoneInactive = zoneInactive;
	}

	class SortEntitiesByTagLevelDb implements Comparator<CandiModel> {

		@Override
		public int compare(CandiModel object1, CandiModel object2) {

			if (!object1.getProxiEntity().beacon.isLocalOnly && object2.getProxiEntity().beacon.isLocalOnly)
				return -1;
			else if (!object2.getProxiEntity().beacon.isLocalOnly && object1.getProxiEntity().beacon.isLocalOnly)
				return 1;
			else
				return object2.getProxiEntity().beacon.getAvgBeaconLevel() - object1.getProxiEntity().beacon.getAvgBeaconLevel();
		}
	}

	class SortEntitiesByDiscoveryTime implements Comparator<CandiModel> {

		@Override
		public int compare(CandiModel object1, CandiModel object2) {

			if (!object1.getProxiEntity().beacon.isLocalOnly && object2.getProxiEntity().beacon.isLocalOnly)
				return -1;
			else if (!object2.getProxiEntity().beacon.isLocalOnly && object1.getProxiEntity().beacon.isLocalOnly)
				return 1;
			else {
				if ((object2.getProxiEntity().discoveryTime.getTime() / 100) - (object1.getProxiEntity().discoveryTime.getTime() / 100) < 0)
					return -1;
				else if ((object2.getProxiEntity().discoveryTime.getTime() / 100) - (object1.getProxiEntity().discoveryTime.getTime() / 100) > 0)
					return 1;
				else {
					if (object2.getProxiEntity().label.compareToIgnoreCase(object1.getProxiEntity().label) < 0)
						return 1;
					else if (object2.getProxiEntity().label.compareToIgnoreCase(object1.getProxiEntity().label) > 0)
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

			if (!object1.getProxiEntity().beacon.isLocalOnly && object2.getProxiEntity().beacon.isLocalOnly)
				return -1;
			else if (!object2.getProxiEntity().beacon.isLocalOnly && object1.getProxiEntity().beacon.isLocalOnly)
				return 1;
			else {
				if ((object2.getProxiEntity().visibleTime.getTime() / 100) - (object1.getProxiEntity().visibleTime.getTime() / 100) < 0)
					return -1;
				else if ((object2.getProxiEntity().visibleTime.getTime() / 100) - (object1.getProxiEntity().visibleTime.getTime() / 100) > 0)
					return 1;
				else {
					if (object2.getProxiEntity().label.compareToIgnoreCase(object1.getProxiEntity().label) < 0)
						return 1;
					else if (object2.getProxiEntity().label.compareToIgnoreCase(object1.getProxiEntity().label) > 0)
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

			return object1.getProxiEntity().entityType.compareToIgnoreCase(object2.getProxiEntity().entityType);
		}
	}

}
