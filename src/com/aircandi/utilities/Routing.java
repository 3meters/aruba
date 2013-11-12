package com.aircandi.utilities;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.applications.Candigrams;
import com.aircandi.applications.Comments;
import com.aircandi.applications.Maps;
import com.aircandi.applications.Pictures;
import com.aircandi.applications.Places;
import com.aircandi.applications.Users;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.TrackerBase.TrackerCategory;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutMeta;
import com.aircandi.service.objects.ShortcutSettings;
import com.aircandi.ui.AboutForm;
import com.aircandi.ui.AircandiForm;
import com.aircandi.ui.EntityList;
import com.aircandi.ui.HelpForm;
import com.aircandi.ui.PhotoForm;
import com.aircandi.ui.Preferences;
import com.aircandi.ui.SplashForm;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseBrowse;
import com.aircandi.ui.base.BaseEdit;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.edit.ApplinkEdit;
import com.aircandi.ui.edit.ApplinkListEdit;
import com.aircandi.ui.edit.CommentEdit;
import com.aircandi.ui.edit.FeedbackEdit;
import com.aircandi.ui.edit.InviteEdit;
import com.aircandi.ui.edit.ReportEdit;
import com.aircandi.ui.edit.TuningEdit;
import com.aircandi.ui.helpers.AddressBuilder;
import com.aircandi.ui.helpers.ApplicationPicker;
import com.aircandi.ui.helpers.CategoryBuilder;
import com.aircandi.ui.helpers.PhotoPicker;
import com.aircandi.ui.helpers.PhotoSourcePicker;
import com.aircandi.ui.helpers.ShortcutPicker;
import com.aircandi.ui.user.PasswordEdit;
import com.aircandi.ui.user.ProfileForm;
import com.aircandi.ui.user.RegisterEdit;
import com.aircandi.ui.user.SignInEdit;
import com.aircandi.ui.user.UserForm;
import com.aircandi.utilities.Animate.TransitionType;

public final class Routing {

	public static boolean intent(Activity activity, Intent intent) {
		activity.startActivity(intent);
		Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
		return true;
	}

	public static boolean route(final Activity activity, Route route) {
		return route(activity, route, null, null, null);
	}

	public static boolean route(final Activity activity, Route route, Entity entity) {
		return route(activity, route, entity, null, null);
	}

	public static boolean route(final Activity activity, Route route, Entity entity, Shortcut shortcut, ShortcutSettings settings, Bundle extras) {

		if (route == Route.SHORTCUT) {

			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}
			if (shortcut == null) {
				throw new IllegalArgumentException("valid shortcut required for selected route");
			}

			final ShortcutMeta meta = Shortcut.shortcutMeta.get(shortcut.app);
			if ((meta == null || !meta.installDeclined)
					&& shortcut.getIntentSupport()
					&& shortcut.appExists()
					&& !shortcut.appInstalled()) {
				Dialogs.install(activity, shortcut, entity);
				return true;
			}

			if (shortcut.group != null && shortcut.group.size() > 1) {
				IntentBuilder intentBuilder = new IntentBuilder(activity, ShortcutPicker.class).setEntity(entity);
				final Intent intent = intentBuilder.create();
				final List<String> shortcutStrings = new ArrayList<String>();
				for (Shortcut item : shortcut.group) {
					Shortcut clone = item.clone();
					clone.group = null;
					shortcutStrings.add(Json.objectToJson(clone, Json.UseAnnotations.FALSE, Json.ExcludeNulls.TRUE));
				}
				intent.putStringArrayListExtra(Constants.EXTRA_SHORTCUTS, (ArrayList<String>) shortcutStrings);
				activity.startActivity(intent);
				Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			}
			else {
				Routing.shortcut(activity, shortcut, entity, null);
			}

			return true;
		}
		else if (route == Route.ENTITY_LIST && settings != null) {

			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}
			IntentBuilder intentBuilder = new IntentBuilder(activity, EntityList.class)
					.setEntityId(entity.id)
					.setListLinkType(settings.linkType)
					.setListLinkSchema(settings.linkTargetSchema)
					.setListLinkDirection(settings.direction.name())
					.setExtras(extras);

			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_PAGE);
			return true;
		}
		else {
			return route(activity, route, entity, null, extras);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static boolean route(final Activity activity, Route route, Entity entity, String schema, Bundle extras) {

		if (route == Route.UNKNOWN) {

			return false;
		}
		else if (route == Route.HOME) {

			Intent intent = new Intent(activity, AircandiForm.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

			activity.startActivity(intent);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_PAGE);
			return true;
		}

		else if (route == Route.WATCHING) {

			entity = Aircandi.getInstance().getCurrentUser();

			final IntentBuilder intentBuilder = new IntentBuilder(activity, AircandiForm.class)
					.setEntityId(Aircandi.getInstance().getCurrentUser().id);

			Intent intent = intentBuilder.create();
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

			activity.startActivity(intent);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_PAGE);
			return true;
		}

		else if (route == Route.CREATED) {

			entity = Aircandi.getInstance().getCurrentUser();

			final IntentBuilder intentBuilder = new IntentBuilder(activity, AircandiForm.class)
					.setEntityId(Aircandi.getInstance().getCurrentUser().id);

			Intent intent = intentBuilder.create();
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

			activity.startActivity(intent);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_PAGE);
			return true;
		}

		else if (route == Route.PROFILE) {

			if (entity == null) {
				throw new IllegalArgumentException("valid user entity required for selected route");
			}

			final IntentBuilder intentBuilder = new IntentBuilder(activity, UserForm.class)
					.setEntityId(entity.id);

			Intent intent = intentBuilder.create();
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

			activity.startActivity(intent);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_PAGE);
			return true;
		}

		else if (route == Route.NOTIFICATIONS) {

			Intent intent = new Intent(activity, AircandiForm.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			activity.startActivity(intent);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_PAGE);
			return true;
		}

		else if (route == Route.SETTINGS) {

			activity.startActivityForResult(new Intent(activity, Preferences.class), Constants.ACTIVITY_PREFERENCES);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.FEEDBACK) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, FeedbackEdit.class);
			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.REPORT) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, ReportEdit.class)
					.setEntity(entity)
					.setEntitySchema(schema);
			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.INVITE) {

			if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
				Dialogs.signin(activity, R.string.alert_signin_message_invite);
				return true;
			}

			final IntentBuilder intentBuilder = new IntentBuilder(activity, InviteEdit.class);
			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.ABOUT) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, AboutForm.class);
			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.BROWSE) {

			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}
			Class<?> clazz = BaseEntityForm.viewFormBySchema(entity.schema);
			if (clazz != null) {
				final IntentBuilder intentBuilder = new IntentBuilder(activity, BaseEntityForm.viewFormBySchema(entity.schema))
						.setEntityId(entity.id)
						.setExtras(extras);

				if (entity.toId != null) {
					intentBuilder.setEntityParentId(entity.toId);
				}
				activity.startActivity(intentBuilder.create());
				Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_PAGE);
			}
			return true;
		}

		else if (route == Route.EDIT) {

			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}

			if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
				Integer messageResId = R.string.alert_signin_message;
				if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					messageResId = R.string.alert_signin_message_place_edit;
				}
				else if (entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
					messageResId = R.string.alert_signin_message_candigram_edit;
				}
				else if (entity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
					messageResId = R.string.alert_signin_message_picture_edit;
				}
				Dialogs.signin(activity, messageResId);
				return true;
			}

			IntentBuilder intentBuilder = new IntentBuilder(activity, BaseEntityEdit.editFormBySchema(entity.schema))
					.setEntity(entity)
					.setExtras(extras);

			Intent intent = intentBuilder.create();

			//			if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
			//				PendingIntent pendingIntent = PendingIntent.getActivity(activity, Constants.ACTIVITY_ENTITY_EDIT, intent, PendingIntent.FLAG_ONE_SHOT);
			//				Intent signin = new Intent(activity, SignInEdit.class).putExtra("pendingIntent", pendingIntent);
			//				activity.startActivityForResult(signin, Constants.ACTIVITY_ENTITY_EDIT);
			//				Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			//				return true;
			//			}

			activity.startActivityForResult(intent, Constants.ACTIVITY_ENTITY_EDIT);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.ADD) {

			((BaseBrowse) activity).onAdd();
			return true;
		}

		else if (route == Route.NEW) {

			if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
				Integer messageResId = R.string.alert_signin_message;
				if (schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					messageResId = R.string.alert_signin_message_place_add;
				}
				else if (schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
					messageResId = R.string.alert_signin_message_candigram_add;
				}
				else if (schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
					messageResId = R.string.alert_signin_message_picture_add;
				}
				else if (schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
					messageResId = R.string.alert_signin_message_comment_add;
				}
				Dialogs.signin(activity, messageResId);
				return true;
			}

			IntentBuilder intentBuilder = new IntentBuilder(activity, BaseEntityEdit.insertFormBySchema(schema))
					.setEntitySchema(schema)
					.setExtras(extras);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_INSERT);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.NEW_FOR) {

			if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
				Integer messageResId = R.string.alert_signin_message;
				if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					messageResId = R.string.alert_signin_message_place_add_to;
				}
				else if (entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
					messageResId = R.string.alert_signin_message_candigram_add_to;
				}
				else if (entity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
					messageResId = R.string.alert_signin_message_picture_add_to;
				}
				Dialogs.signin(activity, messageResId);
				return true;
			}

			IntentBuilder intentBuilder = new IntentBuilder(activity, ApplicationPicker.class).setEntity(entity);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_APPLICATION_PICK);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.HELP) {

			if (extras == null) {
				((BaseBrowse) activity).onHelp();
			}
			else {
				IntentBuilder intentBuilder = new IntentBuilder(activity, HelpForm.class).setExtras(extras);
				activity.startActivity(intentBuilder.create());
				Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_HELP);
			}
			return true;
		}

		else if (route == Route.SIGNIN_PROFILE) {

			if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
				Dialogs.signin(activity, null);
				return true;
			}

			entity = Aircandi.getInstance().getCurrentUser();
			if (entity == null) {
				throw new IllegalArgumentException("valid user entity required for selected route");
			}
			final IntentBuilder intentBuilder = new IntentBuilder(activity, ProfileForm.class)
					.setEntityId(entity.id);

			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_PAGE);
			return true;
		}

		else if (route == Route.PHOTO) {

			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}

			Intent intent = null;
			final Photo photo = entity.photo;
			photo.setCreatedAt(entity.modifiedDate.longValue());
			photo.setName(entity.name);
			photo.setUser(entity.creator);
			final String jsonPhoto = Json.objectToJson(photo);

			final IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoForm.class)
					.setExtras(extras);

			if (entity.toId != null) {
				intentBuilder.setEntityParentId(entity.toId);
			}

			intent = intentBuilder.create();
			intent.putExtra(Constants.EXTRA_PHOTO, jsonPhoto);

			activity.startActivity(intent);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_PAGE);
			return true;
		}

		else if (route == Route.COMMENT_NEW) {

			if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
				Dialogs.signin(activity, null);
				return true;
			}

			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}
			final IntentBuilder intentBuilder = new IntentBuilder(activity, CommentEdit.class).setEntityParentId(entity.id);
			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.ENTITY_LIST) {

			/* Extras must set list mode. */
			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}

			IntentBuilder intentBuilder = new IntentBuilder(activity, EntityList.class)
					.setEntityId(entity.id)
					.setListLinkSchema(schema)
					.setExtras(extras);
			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_PAGE);
			return true;
		}

		else if (route == Route.ACCEPT) {

			((BaseActivity) activity).onAccept();	// Give activity a chance for discard confirmation
			return true;
		}

		else if (route == Route.CANCEL) {

			((BaseActivity) activity).onCancel(false);	// Give activity a chance for discard confirmation
			return true;
		}

		else if (route == Route.CANCEL_FORCE) {

			((BaseActivity) activity).onCancel(true);	// Give activity a chance for discard confirmation
			return true;
		}

		else if (route == Route.DELETE) {

			((BaseEdit) activity).confirmDelete();	// Give activity a chance for discard confirmation
			return true;
		}

		else if (route == Route.ZOOM_IN) {

			((PhotoForm) activity).onZoomIn();
			return true;
		}
		
		else if (route == Route.ZOOM_OUT) {

			((PhotoForm) activity).onZoomOut();
			return true;
		}
		
		else if (route == Route.DELETE_NOTIFICATIONS) {

			return false;
		}

		else if (route == Route.CANCEL_HELP) {

			((BaseActivity) activity).setResultCode(Activity.RESULT_CANCELED);
			activity.finish();
			Animate.doOverridePendingTransition(activity, TransitionType.HELP_TO_PAGE);
			return true;
		}

		else if (route == Route.SIGNOUT) {

			BaseActivity.signout(activity, false);
			return true;
		}

		else if (route == Route.TEST) {
			((ApplinkEdit) activity).onTestButtonClick();
			return true;
		}

		else if (route == Route.SIGNIN) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, SignInEdit.class);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_SIGNIN);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.REGISTER) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, RegisterEdit.class);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_SIGNIN);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.TERMS) {

			final IntentBuilder intentBuilder = new IntentBuilder(android.content.Intent.ACTION_VIEW);
			intentBuilder.setData(Uri.parse(Constants.URL_AIRCANDI_TERMS));
			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.PRIVACY) {

			final IntentBuilder intentBuilder = new IntentBuilder(android.content.Intent.ACTION_VIEW);
			intentBuilder.setData(Uri.parse(Constants.URL_AIRCANDI_PRIVACY));
			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.LEGAL) {

			final IntentBuilder intentBuilder = new IntentBuilder(android.content.Intent.ACTION_VIEW);
			intentBuilder.setData(Uri.parse(Constants.URL_AIRCANDI_LEGAL));
			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.SETTINGS_LOCATION) {

			activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			activity.finish();
			return true;
		}

		else if (route == Route.SETTINGS_WIFI) {

			activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			activity.finish();
			return true;
		}

		else if (route == Route.ADDRESS_EDIT) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, AddressBuilder.class).setEntity(entity);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ADDRESS_EDIT);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.CATEGORY_EDIT) {

			final Intent intent = new Intent(activity, CategoryBuilder.class);
			if (((Place) entity).category != null) {
				final String jsonCategory = Json.objectToJson(((Place) entity).category);
				intent.putExtra(Constants.EXTRA_CATEGORY, jsonCategory);
			}
			activity.startActivityForResult(intent, Constants.ACTIVITY_CATEGORY_EDIT);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.PASSWORD_CHANGE) {

			final IntentBuilder intentBuilder = new IntentBuilder(activity, PasswordEdit.class);
			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.SPLASH) {

			final Intent intent = new Intent(activity, SplashForm.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if (Constants.SUPPORTS_HONEYCOMB) {
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			}
			activity.startActivity(intent);
			activity.finish();
			Animate.doOverridePendingTransition(activity, TransitionType.FORM_TO_PAGE);
			return true;
		}

		else if (route == Route.PHOTO_SOURCE) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoSourcePicker.class).setEntity(entity);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PICTURE_SOURCE_PICK);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.APPLINKS_EDIT) {

			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}
			IntentBuilder intentBuilder = new IntentBuilder(activity, ApplinkListEdit.class)
					.setEntityId(entity.id)
					.setExtras(extras);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_APPLINKS_EDIT);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.PHOTO_FROM_GALLERY) {

			final Intent intent = new Intent()
					.setType("image/*")
					.setAction(Intent.ACTION_GET_CONTENT);

			/* We want to filter out remove images like the linked in from picasa. */
			if (Constants.SUPPORTS_HONEYCOMB) {
				intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
			}

			activity.startActivityForResult(Intent.createChooser(intent
					, activity.getString(R.string.chooser_gallery_title))
					, Constants.ACTIVITY_PHOTO_PICK_DEVICE);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.PHOTO_FROM_CAMERA) {

			IntentBuilder intentBuilder = new IntentBuilder(MediaStore.ACTION_IMAGE_CAPTURE).setExtras(extras);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PHOTO_MAKE);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.PHOTO_SEARCH) {

			IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoPicker.class).setExtras(extras);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PHOTO_SEARCH);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.PHOTO_PLACE_SEARCH) {

			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}
			IntentBuilder intentBuilder = new IntentBuilder(activity, PhotoPicker.class).setEntityId(entity.id);
			activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PHOTO_PICK_PLACE);
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		else if (route == Route.TUNE) {

			if (entity == null) {
				throw new IllegalArgumentException("valid entity required for selected route");
			}
			IntentBuilder intentBuilder = new IntentBuilder(activity, TuningEdit.class).setEntity(entity);
			activity.startActivity(intentBuilder.create());
			Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
			return true;
		}

		return false;
	}

	public static void shortcut(final Activity activity, Shortcut shortcut, Entity entity, Direction direction) {

		if (shortcut.schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
			Aircandi.tracker.sendEvent(TrackerCategory.UX, "applink_click", shortcut.app, 0);

			if (shortcut.app.equals(Constants.TYPE_APP_PICTURE)
					|| shortcut.app.equals(Constants.TYPE_APP_POST)) {
				if (shortcut.getAction().equals(Constants.ACTION_VIEW)) {
					Pictures.view(activity, shortcut.appId, shortcut.linkType, entity.id);
				}
				else if (shortcut.getAction().equals(Constants.ACTION_VIEW_FOR)) {
					Pictures.viewFor(activity, entity.id, shortcut.linkType, direction);
				}
				else if (shortcut.getAction().equals(Constants.ACTION_VIEW_AUTO)) {
					if (shortcut.count == 1) {
						Pictures.view(activity, shortcut.appId, shortcut.linkType, entity.id);
					}
					else {
						Pictures.viewFor(activity, entity.id, shortcut.linkType, direction);
					}
				}
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_CANDIGRAM)) {
				if (shortcut.getAction().equals(Constants.ACTION_VIEW)) {
					Candigrams.view(activity, entity.id, null, entity.id);
				}
				else if (shortcut.getAction().equals(Constants.ACTION_VIEW_FOR)) {
					Candigrams.viewFor(activity, entity.id, shortcut.linkType, direction);
				}
				else if (shortcut.getAction().equals(Constants.ACTION_VIEW_AUTO)) {
					if (shortcut.count == 1) {
						Candigrams.view(activity, shortcut.appId, shortcut.linkType, entity.id);
					}
					else {
						Candigrams.viewFor(activity, entity.id, shortcut.linkType, direction);
					}
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
					Places.view(activity, shortcut.appId, entity.id);
				}
				else if (shortcut.getAction().equals(Constants.ACTION_VIEW_FOR)) {
					Places.viewFor(activity, entity.id, shortcut.linkType, direction);
				}
			}
			else if (shortcut.app.equals(Constants.TYPE_APP_USER)) {
				if (shortcut.getAction().equals(Constants.ACTION_VIEW)) {
					Users.view(activity, shortcut.appId, shortcut.linkType, entity.id);
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
				if (shortcut.getAction().equals(Constants.ACTION_VIEW)) {
					Maps.view(activity, entity);
				}
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
					if (shortcut.app.equals(Constants.TYPE_APP_PICTURE)
							|| shortcut.app.equals(Constants.TYPE_APP_POST)) {
						Pictures.view(activity, shortcut.getId(), shortcut.linkType, entity != null ? entity.id : null);
					}
					else if (shortcut.app.equals(Constants.TYPE_APP_CANDIGRAM)) {
						Candigrams.view(activity, shortcut.getId(), shortcut.linkType, entity != null ? entity.id : null);
					}
					else if (shortcut.app.equals(Constants.TYPE_APP_COMMENT)) {
						Comments.view(activity, shortcut.getId());
					}
					else if (shortcut.app.equals(Constants.TYPE_APP_PLACE)) {
						Places.view(activity, shortcut.getId(), entity != null ? entity.id : null);
					}
					else if (shortcut.app.equals(Constants.TYPE_APP_USER)) {
						Users.view(activity, shortcut.getId(), shortcut.linkType, entity != null ? entity.id : null);
					}
				}
			}
			else if (shortcut.intent != null) {
				Routing.intent(activity, shortcut.intent);
			}
		}
	}

	public static Route routeForMenuId(int itemId) {
		if (itemId == R.id.edit) {
			return Route.EDIT;
		}
		else if (itemId == R.id.help) {
			return Route.HELP;
		}
		else if (itemId == R.id.refresh) {
			return Route.REFRESH;
		}
		else if (itemId == R.id.settings) {
			return Route.SETTINGS;
		}
		else if (itemId == R.id.profile) {
			return Route.SIGNIN_PROFILE;
		}
		else if (itemId == android.R.id.home) {
			return Route.CANCEL;
		}
		else if (itemId == R.id.home) {
			return Route.HOME;
		}
		else if (itemId == R.id.signout) {
			return Route.SIGNOUT;
		}
		else if (itemId == R.id.signin) {
			return Route.SIGNIN;
		}
		else if (itemId == R.id.test) {
			return Route.TEST;
		}
		else if (itemId == R.id.invite) {
			return Route.INVITE;
		}
		else if (itemId == R.id.feedback) {
			return Route.FEEDBACK;
		}
		else if (itemId == R.id.cancel) {
			return Route.CANCEL;
		}
		else if (itemId == R.id.accept) {
			return Route.ACCEPT;
		}
		else if (itemId == R.id.add) {
			return Route.ADD;
		}
		else if (itemId == R.id.delete) {
			return Route.DELETE;
		}
		else if (itemId == R.id.zoom_in) {
			return Route.ZOOM_IN;
		}
		else if (itemId == R.id.zoom_out) {
			return Route.ZOOM_OUT;
		}
		return Route.UNKNOWN;
	}

	public static enum Route {
		UNKNOWN,
		ADD,
		NEW,
		KICK,
		EDIT,
		BROWSE,
		HELP,
		PROFILE,
		SIGNIN_PROFILE,
		WATCHING,
		PHOTO,
		COMMENT_NEW,
		SHORTCUT,
		ENTITY_LIST,
		HOME,
		SETTINGS,
		FEEDBACK,
		REPORT,
		CANCEL,
		CANCEL_FORCE,
		CANCEL_HELP,
		DELETE,
		REFRESH,
		SIGNOUT,
		SIGNIN,
		REGISTER,
		ACCEPT,
		TERMS,
		PRIVACY,
		LEGAL,
		SETTINGS_LOCATION,
		SETTINGS_WIFI,
		ADDRESS_EDIT,
		CATEGORY_EDIT,
		PASSWORD_CHANGE,
		SPLASH,
		PHOTO_SOURCE,
		APPLINKS_EDIT,
		PHOTO_FROM_GALLERY,
		PHOTO_FROM_CAMERA,
		PHOTO_SEARCH,
		PHOTO_PLACE_SEARCH,
		TUNE,
		NEW_FOR,
		DELETE_NOTIFICATIONS,
		NOTIFICATIONS,
		CREATED,
		INVITE,
		TEST,
		ABOUT,
		ZOOM_IN,
		ZOOM_OUT
	}
}