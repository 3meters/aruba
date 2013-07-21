package com.aircandi.utilities;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ConnectTimeoutException;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Toast;

import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.applications.Comments;
import com.aircandi.applications.Places;
import com.aircandi.applications.Posts;
import com.aircandi.applications.Users;
import com.aircandi.beta.R;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.EntityManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.NotificationManager;
import com.aircandi.components.Tracker;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ExcludeNulls;
import com.aircandi.service.HttpService.UseAnnotations;
import com.aircandi.service.HttpServiceException;
import com.aircandi.service.HttpServiceException.ErrorType;
import com.aircandi.service.WalledGardenException;
import com.aircandi.service.objects.AirLocation;
import com.aircandi.service.objects.AirNotification;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutMeta;
import com.aircandi.service.objects.ShortcutSettings;
import com.aircandi.ui.EntityList;
import com.aircandi.ui.HelpForm;
import com.aircandi.ui.PhotoForm;
import com.aircandi.ui.Preferences;
import com.aircandi.ui.RadarForm;
import com.aircandi.ui.SplashForm;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseActivity.ServiceOperation;
import com.aircandi.ui.base.BaseBrowse;
import com.aircandi.ui.base.BaseEdit;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.edit.ApplinkListEdit;
import com.aircandi.ui.edit.CommentEdit;
import com.aircandi.ui.edit.FeedbackEdit;
import com.aircandi.ui.edit.TuningEdit;
import com.aircandi.ui.helpers.AddressBuilder;
import com.aircandi.ui.helpers.CategoryBuilder;
import com.aircandi.ui.helpers.PhotoPicker;
import com.aircandi.ui.helpers.PhotoSourcePicker;
import com.aircandi.ui.helpers.ShortcutPicker;
import com.aircandi.ui.user.PasswordEdit;
import com.aircandi.ui.user.RegisterEdit;
import com.aircandi.ui.user.SignInEdit;
import com.aircandi.ui.user.UserForm;
import com.aircandi.utilities.Animate.TransitionType;

public final class Routing {
	
	public static boolean intent(Activity activity, Intent intent) {
		activity.startActivity(intent);
		Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
		return true;
	}

	public static boolean route(final Activity activity, Route route) {
		return route(activity, route, null, null, null);
	}

	public static boolean route(final Activity activity, Route route, Entity entity) {
		return route(activity, route, entity, null, null);
	}

	public static boolean route(final Activity activity, Route route, Entity entity, Shortcut shortcut, ShortcutSettings settings, Bundle extras) {

		if (route == Route.Shortcut) {
			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}
			if (shortcut == null) {
				throw new IllegalArgumentException("valid shortcut required for selected route");
			}

			final ShortcutMeta meta = Shortcut.shortcutMeta.get(shortcut.app);
			if (meta != null && !meta.installDeclined
					&& shortcut.getIntentSupport()
					&& shortcut.appExists()
					&& !shortcut.appInstalled()) {
				Dialogs.install(activity, shortcut, entity);
			}

			if (shortcut.group != null && shortcut.group.size() > 1) {
				IntentBuilder intentBuilder = new IntentBuilder(activity, ShortcutPicker.class).setEntity(entity);
				final Intent intent = intentBuilder.create();
				final List<String> shortcutStrings = new ArrayList<String>();
				for (Shortcut item : shortcut.group) {
					Shortcut clone = item.clone();
					clone.group = null;
					shortcutStrings.add(HttpService.objectToJson(clone, UseAnnotations.False, ExcludeNulls.True));
				}
				intent.putStringArrayListExtra(Constants.EXTRA_SHORTCUTS, (ArrayList<String>) shortcutStrings);
				activity.startActivity(intent);
				Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			}
			else {
				Routing.shortcut(activity, shortcut, entity, null);
			}

			return true;
		}
		else if (route == Route.EntityList && settings != null) {

			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}
			IntentBuilder intentBuilder = new IntentBuilder(activity, EntityList.class)
					.setEntityId(entity.id)
					.setListLinkType(settings.linkType)
					.setListLinkSchema(settings.linkSchema)
					.setListLinkDirection(settings.direction.name())
					.setExtras(extras);

			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PageToPage);
			return true;
		}
		else {
			return route(activity, route, entity, null, extras);
		}
	}

	public static boolean route(final Activity activity, Route route, Entity entity, String schema, Bundle extras) {

		if (route == Route.Browse) {

			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}
			final IntentBuilder intentBuilder = new IntentBuilder(activity, BaseEntityForm.viewFormBySchema(entity.schema))
					.setEntityId(entity.id)
					.setExtras(extras);

			if (entity.toId != null) {
				intentBuilder.setEntityParentId(entity.toId);
			}
			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PageToPage);
			return true;
		}

		else if (route == Route.Edit) {

			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}
			Tracker.sendEvent("ui_action", "edit_" + entity.schema, null, 0, Aircandi.getInstance().getUser());
			IntentBuilder intentBuilder = new IntentBuilder(activity, BaseEntityEdit.editFormBySchema(entity.schema))
					.setEntity(entity)
					.setExtras(extras);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_EDIT);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			return true;
		}

		else if (route == Route.Add) {

			((BaseBrowse) activity).onAdd();
			return true;
		}

		else if (route == Route.New) {

			Tracker.sendEvent("ui_action", "new_" + schema, null, 0, Aircandi.getInstance().getUser());
			IntentBuilder intentBuilder = new IntentBuilder(activity, BaseEntityEdit.editFormBySchema(schema))
					.setEntitySchema(schema)
					.setExtras(extras);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_INSERT);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			return true;
		}

		else if (route == Route.Help) {
			/*
			 * Extra constructed here because this gets routed from menus.
			 */
			Intent intent = new Intent(activity, HelpForm.class);
			if (activity.getClass().getSimpleName().equals("RadarForm")) {
				intent.putExtra(Constants.EXTRA_HELP_ID, R.layout.radar_help);
			}
			else if (activity.getClass().getSimpleName().equals("PlaceForm")) {
				intent.putExtra(Constants.EXTRA_HELP_ID, R.layout.place_help);
			}
			activity.startActivity(intent);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToHelp);
			return true;
		}

		else if (route == Route.SigninProfile) {

			entity = Aircandi.getInstance().getUser();
			if (entity == null) {
				throw new IllegalArgumentException("valid user entity required for selected route");
			}
			final IntentBuilder intentBuilder = new IntentBuilder(activity, UserForm.class)
					.setEntityId(entity.id);

			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PageToPage);
			return true;
		}

		else if (route == Route.Profile) {

			if (entity == null) {
				throw new IllegalArgumentException("valid user entity required for selected route");
			}
			final IntentBuilder intentBuilder = new IntentBuilder(activity, UserForm.class)
					.setEntityId(entity.id);

			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PageToPage);
			return true;
		}

		else if (route == Route.Watching) {

			entity = Aircandi.getInstance().getUser();
			if (entity == null) {
				throw new IllegalArgumentException("valid user entity required for selected route");
			}
			Bundle stuff = new Bundle();
			stuff.putInt(Constants.EXTRA_TAB_POSITION, 2);
			final IntentBuilder intentBuilder = new IntentBuilder(activity, UserForm.class)
					.setEntityId(entity.id)
					.setExtras(stuff);

			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PageToPage);
			//			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			//			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			return true;
		}

		else if (route == Route.Photo) {

			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}

			Intent intent = null;
			final Photo photo = entity.photo;
			photo.setCreatedAt(entity.modifiedDate.longValue());
			photo.setName(entity.name);
			photo.setUser(entity.creator);
			EntityManager.getInstance().getPhotos().clear();
			EntityManager.getInstance().getPhotos().add(photo);
			intent = new Intent(activity, PhotoForm.class);
			intent.putExtra(Constants.EXTRA_URI, photo.getUri());
			intent.putExtra(Constants.EXTRA_PAGING_ENABLED, false);

			activity.startActivity(intent);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToPage);
			return true;
		}

		else if (route == Route.CommentNew) {

			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}
			final IntentBuilder intentBuilder = new IntentBuilder(activity, CommentEdit.class).setEntityParentId(entity.id);
			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			return true;
		}

		else if (route == Route.EntityList) {

			/* Extras must set list mode. */
			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}

			IntentBuilder intentBuilder = new IntentBuilder(activity, EntityList.class)
					.setEntityId(entity.id)
					.setListLinkSchema(schema)
					.setExtras(extras);
			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PageToPage);
			return true;
		}

		else if (route == Route.Accept) {

			((BaseEdit) activity).onAccept();	// Give activity a chance for discard confirmation
			return true;
		}

		else if (route == Route.Cancel) {

			((BaseActivity) activity).onCancel(false);	// Give activity a chance for discard confirmation
			return true;
		}

		else if (route == Route.CancelForce) {

			((BaseActivity) activity).onCancel(true);	// Give activity a chance for discard confirmation
			return true;
		}

		else if (route == Route.Delete) {

			((BaseEdit) activity).confirmDelete();	// Give activity a chance for discard confirmation
			return true;
		}

		else if (route == Route.CancelHelp) {

			activity.setResult(Activity.RESULT_CANCELED);
			activity.finish();
			Animate.doOverridePendingTransition(activity, TransitionType.HelpToPage);
			return true;
		}

		else if (route == Route.Home) {

			Intent intent = new Intent(activity, RadarForm.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			activity.finish();
			activity.startActivity(intent);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToPage);
			return true;
		}

		else if (route == Route.Settings) {

			activity.startActivityForResult(new Intent(activity, Preferences.class), Constants.ACTIVITY_PREFERENCES);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			return true;
		}

		else if (route == Route.Feedback) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, FeedbackEdit.class);
			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			return true;
		}

		else if (route == Route.Signout) {

			((BaseActivity) activity).signout();
			return true;
		}

		else if (route == Route.Signin) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, SignInEdit.class);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_SIGNIN);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			return true;
		}

		else if (route == Route.Register) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, RegisterEdit.class);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_SIGNIN);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			return true;
		}

		else if (route == Route.Terms) {

			final IntentBuilder intentBuilder = new IntentBuilder(android.content.Intent.ACTION_VIEW);
			intentBuilder.setData(Uri.parse(Constants.URL_AIRCANDI_TERMS));
			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			return true;
		}

		else if (route == Route.SettingsLocation) {

			activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			activity.finish();
			return true;
		}

		else if (route == Route.SettingsWifi) {

			activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			activity.finish();
			return true;
		}

		else if (route == Route.AddressEdit) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, AddressBuilder.class).setEntity(entity);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ADDRESS_EDIT);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			return true;
		}

		else if (route == Route.CategoryEdit) {

			final Intent intent = new Intent(activity, CategoryBuilder.class);
			if (((Place) entity).category != null) {
				final String jsonCategory = HttpService.objectToJson(((Place) entity).category);
				intent.putExtra(Constants.EXTRA_CATEGORY, jsonCategory);
			}
			activity.startActivityForResult(intent, Constants.ACTIVITY_CATEGORY_EDIT);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			return true;
		}

		else if (route == Route.PasswordChange) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, PasswordEdit.class);
			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			return true;
		}

		else if (route == Route.Splash) {

			final Intent intent = new Intent(activity, SplashForm.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			activity.startActivity(intent);
			activity.finish();
			Animate.doOverridePendingTransition(activity, TransitionType.FormToPage);
			return true;
		}

		else if (route == Route.PhotoSource) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoSourcePicker.class).setEntity(entity);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PICTURE_SOURCE_PICK);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			return true;
		}

		else if (route == Route.ApplinksEdit) {

			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}
			IntentBuilder intentBuilder = new IntentBuilder(activity, ApplinkListEdit.class)
					.setEntityId(entity.id)
					.setExtras(extras);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_APPLINKS_EDIT);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			return true;
		}

		else if (route == Route.PhotoFromGallery) {

			final Intent intent = new Intent()
					.setType("image/*")
					.setAction(Intent.ACTION_GET_CONTENT);

			/* We want to filter out remove images like the linked in from picasa. */
			if (Constants.SUPPORTS_HONEYCOMB) {
				intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
			}

			activity.startActivityForResult(Intent.createChooser(intent
					, activity.getString(R.string.chooser_gallery_title))
					, Constants.ACTIVITY_PICTURE_PICK_DEVICE);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			return true;
		}

		else if (route == Route.PhotoFromCamera) {

			IntentBuilder intentBuilder = new IntentBuilder(MediaStore.ACTION_IMAGE_CAPTURE).setExtras(extras);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PICTURE_MAKE);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			return true;
		}

		else if (route == Route.PhotoSearch) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoPicker.class).setExtras(extras);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PICTURE_PICK_PLACE);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			return true;
		}

		else if (route == Route.PhotoPlaceSearch) {

			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}
			IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoPicker.class).setEntityId(entity.id);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PICTURE_PICK_PLACE);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			return true;
		}

		else if (route == Route.Tune) {

			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}
			IntentBuilder intentBuilder = new IntentBuilder(activity, TuningEdit.class).setEntity(entity);
			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
			return true;
		}

		return false;
	}

	public static void shortcut(final Activity activity, Shortcut shortcut, Entity entity, Direction direction) {

		if (shortcut.schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {

			if (shortcut.app.equals(Constants.TYPE_APP_POST)) {
				if (shortcut.getAction().equals(Constants.ACTION_VIEW)) {
					Posts.view(activity, entity.id);
				}
				else if (shortcut.getAction().equals(Constants.ACTION_VIEW_FOR)) {
					Posts.viewFor(activity, entity.id, shortcut.linkType, direction);
				}
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_COMMENT)) {
				if (shortcut.getAction().equals(Constants.ACTION_VIEW)) {
					Comments.view(activity, entity.id);
				}
				else if (shortcut.getAction().equals(Constants.ACTION_VIEW_FOR)) {
					Comments.viewFor(activity, entity.id, shortcut.linkType, direction);
				}
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_PLACE)) {
				if (shortcut.getAction().equals(Constants.ACTION_VIEW)) {
					Places.view(activity, entity.id);
				}
				else if (shortcut.getAction().equals(Constants.ACTION_VIEW_FOR)) {
					Places.viewFor(activity, entity.id, shortcut.linkType, direction);
				}
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_USER)) {
				if (shortcut.getAction().equals(Constants.ACTION_VIEW)) {
					Users.view(activity, entity.id);
				}
				else if (shortcut.getAction().equals(Constants.ACTION_VIEW_FOR)) {
					Users.viewFor(activity, entity.id, shortcut.linkType, direction);
				}
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_TWITTER)) {
				AndroidManager.getInstance().callTwitterActivity(activity, (shortcut.appId != null) ? shortcut.appId : shortcut.appUrl);
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_FOURSQUARE)) {
				AndroidManager.getInstance().callFoursquareActivity(activity, (shortcut.appId != null) ? shortcut.appId : shortcut.appUrl);
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_FACEBOOK)) {
				AndroidManager.getInstance().callFacebookActivity(activity, (shortcut.appId != null) ? shortcut.appId : shortcut.appUrl);
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_MAP) && entity != null) {
				Tracker.sendEvent("ui_action", "map_place", null, 0, Aircandi.getInstance().getUser());
				final AirLocation location = entity.getLocation();
				AndroidManager.getInstance().callMapActivity(activity
						, String.valueOf(location.lat.doubleValue())
						, String.valueOf(location.lng.doubleValue())
						, entity.name);
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_YELP)) {
				AndroidManager.getInstance().callYelpActivity(activity, shortcut.appId, shortcut.appUrl);
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_OPENTABLE)) {
				AndroidManager.getInstance().callOpentableActivity(activity, shortcut.appId, shortcut.appUrl);
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_WEBSITE)) {
				AndroidManager.getInstance().callBrowserActivity(activity, (shortcut.appUrl != null) ? shortcut.appUrl : shortcut.appId);
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_EMAIL)) {
				AndroidManager.getInstance().callSendToActivity(activity, shortcut.name, shortcut.appId, null, null);
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_LIKE)) {
				/*
				 * We don't do anything right now. Goal is to show a form with more detail on the likes.
				 */
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_WATCH)) {
				/*
				 * We don't do anything right now. Goal is to show a form with more detail on the watchers.
				 */
			}
			else {
				AndroidManager.getInstance().callGenericActivity(activity, (shortcut.appUrl != null) ? shortcut.appUrl : shortcut.appId);
			}
		}
		else {
			if (shortcut.isContent()) {
				if (shortcut.getAction().equals(Constants.ACTION_VIEW)) {
					if (shortcut.app.equals(Constants.TYPE_APP_POST)) {
						Posts.view(activity, shortcut.getId());
					}
					else if (shortcut.app.equals(Constants.TYPE_APP_COMMENT)) {
						Comments.view(activity, shortcut.getId());
					}
					else if (shortcut.app.equals(Constants.TYPE_APP_PLACE)) {
						Places.view(activity, shortcut.getId());
					}
					else if (shortcut.app.equals(Constants.TYPE_APP_USER)) {
						Users.view(activity, shortcut.getId());
					}
				}
			}
		}
	}

	public static void serviceError(final Activity activity, ServiceResponse serviceResponse) {
		serviceError(activity, serviceResponse, null);
	}

	public static void serviceError(final Activity activity, ServiceResponse serviceResponse, ServiceOperation serviceOperation) {

		final ErrorType errorType = serviceResponse.exception.getErrorType();
		final String errorMessage = serviceResponse.exception.getMessage();
		final Float statusCode = serviceResponse.exception.getStatusCode();

		/* We always make sure the progress indicator has been stopped */
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				((BaseActivity) activity).mBusyManager.hideBusy();
			}
		});

		/*
		 * Client errors occur when we are unable to get a response from a service, or when the client is
		 * unable to understand a response from a service. This includes protocol, network and timeout errors.
		 */
		if (errorType == ErrorType.Client) {

			if (serviceResponse.exception.getInnerException() instanceof IOException) {
				/*
				 * We have a bad network connection.
				 * 
				 * This could be any of these:
				 * 
				 * Handled above
				 * - SocketException: thrown during socket creation or setting options
				 * - ConnectTimeoutException: timeout expired trying to connect to service
				 * - SocketTimeoutException: timeout expired on a socket
				 * 
				 * Still left
				 * - NoHttpResponseException: target server failed to respond with a valid HTTP response
				 * - UnknownHostException: hostname didn't exist in the dns system
				 */
				if (serviceResponse.exception.getInnerException() instanceof SocketException) {
					/*
					 * We don't have a network connection.
					 */
					final Intent intent = new Intent(activity, RadarForm.class);
					AirNotification airNotification = new AirNotification();
					airNotification.title = activity.getString(R.string.error_connection_none_notification_title);
					airNotification.subtitle = activity.getString(R.string.error_connection_none_notification_message);
					airNotification.intent = intent;
					airNotification.type = "network";
					NotificationManager.getInstance().showNotification(airNotification, activity);

					Dialogs.alertDialogSimple(activity, null, activity.getString(R.string.error_connection_none));
				}
				else if (serviceResponse.exception.getInnerException() instanceof WalledGardenException) {
					/*
					 * We have a connection but user is locked in a walled garden until they sign-in, pay, etc.
					 */
					Dialogs.alertDialogSimple(activity, null, activity.getString(R.string.error_connection_walled_garden));
				}
				else if (serviceResponse.exception.getInnerException() instanceof ConnectTimeoutException) {
					/*
					 * This exception signals that HttpClient is unable to establish a connection with the target server
					 * or proxy server within the given period of time.
					 */
					UI.showToastNotification(activity.getString(R.string.error_service_unavailable), Toast.LENGTH_SHORT);
				}
				else if (serviceResponse.exception.getInnerException() instanceof SocketTimeoutException) {
					/*
					 * We have a connection but got tired of waiting for data. Could be a
					 * poor connection or service is slow.
					 */
					UI.showToastNotification(activity.getString(R.string.error_connection_poor), Toast.LENGTH_SHORT);
				}
				else if (serviceResponse.exception.getInnerException() instanceof UnknownHostException) {
					/*
					 * We have a connection but got tired of waiting for data. Could be a
					 * poor connection or service is slow.
					 */
					Dialogs.alertDialogSimple(activity, null, activity.getString(R.string.error_client_unknown_host));
				}
				else if (serviceResponse.exception.getInnerException() instanceof ClientProtocolException) {
					/*
					 * Something wrong with the request. In most cases, this is a bug and
					 * not something that a user should cause unless they provided a bad uri.
					 */
					UI.showToastNotification(activity.getString(R.string.error_client_request_error), Toast.LENGTH_SHORT);
				}
				else {
					UI.showToastNotification(activity.getString(R.string.error_connection_poor), Toast.LENGTH_SHORT);
				}
			}
			else {
				/*
				 * Something wrong with the request. In most cases, this is a bug and
				 * not something that a user should cause unless they provided a bad uri.
				 */
				if (serviceResponse.exception.getInnerException() instanceof URISyntaxException) {
					UI.showToastNotification(activity.getString(R.string.error_client_request_error), Toast.LENGTH_SHORT);
				}
				else {
					/* Something without special handling */
					UI.showToastNotification(serviceResponse.exception.getMessage(), Toast.LENGTH_SHORT);
				}
			}
		}
		else if (errorType == ErrorType.Service) {

			if (serviceResponse.exception.getInnerException() instanceof HttpServiceException.NotFoundException) {
				/*
				 * Reached the service but requested something that doesn't exist. This is a bug and
				 * not something that a user should cause.
				 */
				UI.showToastNotification(activity.getString(R.string.error_client_request_not_found), Toast.LENGTH_SHORT);
			}
			else if (serviceResponse.exception.getInnerException() instanceof HttpServiceException.UnauthorizedException) {
				/*
				 * Reached the service but requested something that the user can't access.
				 */
				UI.showToastNotification(activity.getString(R.string.error_service_unauthorized), Toast.LENGTH_SHORT);
			}
			else if (serviceResponse.exception.getInnerException() instanceof HttpServiceException.ForbiddenException) {
				/*
				 * Reached the service but request was invalid per service policy.
				 */
				UI.showToastNotification(activity.getString(R.string.error_service_forbidden), Toast.LENGTH_SHORT);
			}
			else if (serviceResponse.exception.getInnerException() instanceof HttpServiceException.GatewayTimeoutException) {
				/*
				 * Reached the service but request was invalid per service policy.
				 */
				UI.showToastNotification(activity.getString(R.string.error_service_gateway_timeout), Toast.LENGTH_SHORT);
			}
			else if (serviceResponse.exception.getInnerException() instanceof HttpServiceException.ClientVersionException) {
				/*
				 * Reached the service but a more current client version is required.
				 */
				Aircandi.applicationUpdateRequired = true;
				final Intent intent = new Intent(activity, SplashForm.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				activity.startActivity(intent);
				activity.finish();
				Animate.doOverridePendingTransition(activity, TransitionType.FormToPage);
			}
			else {
				String title = null;
				String message = null;
				if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
					/*
					 * Reached the service with a good call but the service failed for an unknown reason. Examples
					 * are service bugs like missing indexes causing mongo queries to throw errors.
					 * 
					 * - 500: Something bad and unknown has happened in the service.
					 */
					UI.showToastNotification(activity.getString(R.string.error_service_unknown), Toast.LENGTH_SHORT);
				}
				else {
					/*
					 * Reached the service with a good call but failed for a well known reason.
					 * 
					 * This could have been caused by any problem while inserting/updating.
					 * We look first for ones that are known responses from the service.
					 * 
					 * - 403.x: password not strong enough
					 * - 403.x: email not unique
					 * - 401.2: expired session
					 * - 401.1: invalid or missing session
					 */
					if (statusCode == ProxiConstants.HTTP_STATUS_CODE_UNAUTHORIZED_SESSION_EXPIRED) {
						title = activity.getString(R.string.error_session_expired_title);
						message = activity.getString(R.string.error_session_expired);
						/*
						 * Make sure the user is logged out
						 */
						((BaseActivity) activity).signout();

					}
					else if (statusCode == ProxiConstants.HTTP_STATUS_CODE_UNAUTHORIZED_CREDENTIALS) {
						message = activity.getString(R.string.error_session_invalid);
						if (serviceOperation != null) {
							if (serviceOperation == ServiceOperation.PasswordChange) {
								message = activity.getString(R.string.error_change_password_unauthorized);
							}
							else if (serviceOperation == ServiceOperation.Signin) {
								message = activity.getString(R.string.error_signin_invalid_signin);
							}
						}
						else {
							((BaseActivity) activity).signout();
						}
					}
					else if (statusCode == ProxiConstants.HTTP_STATUS_CODE_UNAUTHORIZED_WHITELIST) {
						message = activity.getString(R.string.error_whitelist_unauthorized);
					}
					else if (statusCode == ProxiConstants.HTTP_STATUS_CODE_UNAUTHORIZED_UNVERIFIED) {
						message = activity.getString(R.string.error_unverified_unauthorized);
					}
					else if (statusCode == ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_USER_PASSWORD_WEAK) {
						message = activity.getString(R.string.error_signup_password_weak);
					}
					else if (statusCode == ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE) {
						message = activity.getString(R.string.error_signup_email_taken);
					}
					else {
						UI.showToastNotification(serviceResponse.exception.getMessage(), Toast.LENGTH_SHORT);
					}
				}

				if (message != null) {
					Dialogs.alertDialogSimple(activity, title, message);
				}
			}
		}

		Logger.w(activity, "Service error: " + errorMessage);
	}

	public static Route routeForMenu(MenuItem menuItem) {
		if (menuItem.getItemId() == R.id.edit) {
			return Route.Edit;
		}
		else if (menuItem.getItemId() == R.id.help) {
			return Route.Help;
		}
		else if (menuItem.getItemId() == R.id.refresh) {
			return Route.Refresh;
		}
		else if (menuItem.getItemId() == R.id.settings) {
			return Route.Settings;
		}
		else if (menuItem.getItemId() == R.id.feedback) {
			return Route.Feedback;
		}
		else if (menuItem.getItemId() == R.id.profile) {
			return Route.SigninProfile;
		}
		else if (menuItem.getItemId() == R.id.watching) {
			return Route.Watching;
		}
		else if (menuItem.getItemId() == android.R.id.home) {
			return Route.Cancel;
		}
		else if (menuItem.getItemId() == R.id.home) {
			return Route.Home;
		}
		else if (menuItem.getItemId() == R.id.signout) {
			return Route.Signout;
		}
		else if (menuItem.getItemId() == R.id.cancel) {
			return Route.Cancel;
		}
		else if (menuItem.getItemId() == R.id.accept) {
			return Route.Accept;
		}
		else if (menuItem.getItemId() == R.id.add) {
			return Route.Add;
		}
		else if (menuItem.getItemId() == R.id.delete) {
			return Route.Delete;
		}
		return Route.Unknown;
	}

	public static enum Route {
		Unknown,
		Add,
		New,
		Edit,
		Browse,
		Help,
		Profile,
		SigninProfile,
		Watching,
		Photo,
		CommentNew,
		Shortcut,
		EntityList,
		Back,
		BackConfirm,
		Home,
		Settings,
		Feedback,
		Cancel,
		CancelForce,
		CancelHelp,
		Delete,
		Refresh,
		Signout,
		Signin,
		Register,
		Accept,
		Terms,
		SettingsLocation,
		SettingsWifi,
		AddressEdit,
		CategoryEdit,
		PasswordChange,
		Splash,
		PhotoSource,
		ApplinksEdit,
		PhotoFromGallery,
		PhotoFromCamera,
		PhotoSearch,
		PhotoPlaceSearch,
		Tune,
	}
}