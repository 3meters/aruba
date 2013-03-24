// $codepro.audit.disable fileComment
package com.aircandi;

@SuppressWarnings("ucd")
public class PlacesConstants {

	/* For readability */
	public static final int	DIST_ONE_METER								= 1;
	public static final int	DIST_FIVE_METERS							= 5;
	public static final int	DIST_TEN_METERS								= 10;
	public static final int	DIST_TWENTY_FIVE_METERS						= 25;
	public static final int	DIST_THIRTY_METERS							= 30;
	public static final int	DIST_FIFTY_METERS							= 50;
	public static final int	DIST_SEVENTY_FIVE_METERS					= 75;
	public static final int	DIST_ONE_HUNDRED_METERS						= 100;
	public static final int	DIST_TWO_HUNDRED_METERS						= 200;
	public static final int	DIST_FIVE_HUNDRED_METERS					= 500;
	public static final int	DIST_ONE_KILOMETER							= 1000;
	public static final int	DIST_TWO_KILOMETERS							= 2000;
	public static final int	DIST_FIVE_KILOMETERS						= 5000;

	/*
	 * Update criteria for active and passive location updates.
	 * 
	 * We use aggresive criteria for passive updates because they are free
	 * and we aren't doing any processing in response to them.
	 */
	public static long		MAXIMUM_AGE									= CandiConstants.TIME_THIRTY_MINUTES;
	public static long		MAXIMUM_AGE_PREFERRED						= CandiConstants.TIME_TWO_MINUTES;
	public static long		BURST_TIMEOUT								= CandiConstants.TIME_TWO_MINUTES;
	public static long		BUSY_TIMEOUT								= CandiConstants.TIME_THIRTY_SECONDS;

	public static int		MIN_DISTANCE_UPDATES						= DIST_FIFTY_METERS;
	public static Integer	MINIMUM_ACCURACY							= DIST_ONE_KILOMETER;
	public static Integer	DESIRED_ACCURACY_GPS						= DIST_THIRTY_METERS;
	public static Integer	DESIRED_ACCURACY_NETWORK					= DIST_THIRTY_METERS;

	/* Used to determine whether the boot receiver starts passive updates */
	public static String	SP_KEY_RUN_ONCE								= "SP_KEY_RUN_ONCE";												//$NON-NLS-1$

	/* Used to filter for notification that active location update provider has been disabled */
	public static String	ACTIVE_LOCATION_UPDATE_PROVIDER_DISABLED	= "com.aircandi.location.active_location_update_provider_disabled"; //$NON-NLS-1$

}
