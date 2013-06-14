package com.aircandi.ui.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.CommandType;
import com.aircandi.components.EntityManager;
import com.aircandi.components.FontManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ArrayListType;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Stat;
import com.aircandi.service.objects.User;
import com.aircandi.ui.CandiForm;
import com.aircandi.ui.CandiList;
import com.aircandi.ui.PictureDetail;
import com.aircandi.ui.base.CandiActivity;
import com.aircandi.ui.widgets.ComboButton;
import com.aircandi.ui.widgets.FlowLayout;
import com.aircandi.ui.widgets.SectionLayout;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.ImageUtils;

@SuppressWarnings("ucd")
public class CandiUser extends CandiActivity {

	private User			mUser;
	protected List<Entity>	mEntities;
	protected Number		mEntityModelRefreshDate;
	protected Number		mEntityModelActivityDate;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			initialize();
			bind(false);
		}
	}

	private void initialize() {
		/* Custom fonts */
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.name));
	}

	private void bind(Boolean refresh) {
		doBind(refresh);
	}

	private void doBind(final Boolean refresh) {
		/*
		 * Navigation setup for action bar icon and title
		 */
		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy(true);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("GetUser");
				ModelResult result = EntityManager.getInstance().getUser(mCommon.mUserId, true);
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					mUser = (User) result.serviceResponse.data;

					result = EntityManager.getInstance().getUserEntities(mCommon.mUserId, true, ProxiConstants.USER_ENTITY_LIMIT);
					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						mEntities = (List<Entity>) result.data;
					}
				}

				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				final ModelResult result = (ModelResult) modelResult;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					mEntityModelRefreshDate = ProximityManager.getInstance().getLastBeaconLoadDate();
					mEntityModelActivityDate = EntityManager.getInstance().getEntityCache().getLastActivityDate();
					mCommon.mActionBar.setTitle(mUser.name);
					buildCandiUser(CandiUser.this, mUser);
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CandiUser);
				}
				mCommon.hideBusy(true);
			}

		}.execute();
	}

	public void doRefresh() {
		/* Called from AircandiCommon */
		bind(true);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onLikeButtonClick(View view) {
		Tracker.sendEvent("ui_action", "like_place", null, 0, Aircandi.getInstance().getUser());

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("LikeEntity");
				ModelResult result = new ModelResult();
				if (!mUser.byAppUser(Constants.TYPE_LINK_LIKE)) {
					Tracker.sendEvent("ui_action", "like_entity", null, 0, Aircandi.getInstance().getUser());
					result = EntityManager.getInstance().verbSomething(Aircandi.getInstance().getUser().id, mUser.id, "like", "like");
				}
				else {
					Tracker.sendEvent("ui_action", "unlike_entity", null, 0, Aircandi.getInstance().getUser());
					result = EntityManager.getInstance().unverbSomething(Aircandi.getInstance().getUser().id, mUser.id, "like", "unlike");
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;
				setSupportProgressBarIndeterminateVisibility(false);
				mCommon.hideBusy(true);
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					bind(false);
				}
				else {
					if (result.serviceResponse.exception.getStatusCode() == ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE) {
						ImageUtils.showToastNotification(getString(R.string.toast_like_duplicate), Toast.LENGTH_SHORT);
					}
					else {
						mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CandiForm);
					}
				}
			}
		}.execute();

	}

	@SuppressWarnings("ucd")
	public void onWatchButtonClick(View view) {

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("WatchEntity");
				ModelResult result = new ModelResult();
				if (!mUser.byAppUser(Constants.TYPE_LINK_WATCH)) {
					Tracker.sendEvent("ui_action", "watch_entity", null, 0, Aircandi.getInstance().getUser());
					result = EntityManager.getInstance().verbSomething(Aircandi.getInstance().getUser().id, mUser.id, "watch", "watch");
				}
				else {
					Tracker.sendEvent("ui_action", "unwatch_entity", null, 0, Aircandi.getInstance().getUser());
					result = EntityManager.getInstance().unverbSomething(Aircandi.getInstance().getUser().id, mUser.id, "watch", "unwatch");
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;
				setSupportProgressBarIndeterminateVisibility(false);
				mCommon.hideBusy(true);
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					bind(false);
				}
				else {
					if (result.serviceResponse.exception.getStatusCode() != ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE) {
						mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CandiForm);
					}
				}
			}
		}.execute();

	}

	@SuppressWarnings("ucd")
	public void onCandiClick(View view) {
		final Entity entity = (Entity) view.getTag();

		final IntentBuilder intentBuilder = new IntentBuilder(this, CandiForm.class)
				.setCommandType(CommandType.View)
				.setEntityId(entity.id)
				.setParentEntityId(entity.toId)
				.setEntityType(entity.type);

		if (entity.toId != null) {
			intentBuilder.setCollectionId(entity.getParent().id);
		}

		final Intent intent = intentBuilder.create();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToPage);
	}

	@SuppressWarnings("ucd")
	public void onImageClick(View view) {
		Intent intent = null;
		final Photo photo = mUser.photo;
		photo.setCreatedAt(mUser.photo.getCreatedAt().longValue());
		photo.setTitle(mUser.name);
		photo.setUser(mUser);
		EntityManager.getInstance().getPhotos().clear();
		EntityManager.getInstance().getPhotos().add(photo);
		intent = new Intent(this, PictureDetail.class);
		intent.putExtra(Constants.EXTRA_URI, mUser.photo.getUri());

		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToPage);
	}

	public void onMoreButtonClick(View view) {

		String target = (String) view.getTag();
		IntentBuilder intentBuilder = null;

		if (target.equals("places")) {
			intentBuilder = new IntentBuilder(this, CandiList.class);
			intentBuilder.setCommandType(CommandType.View)
					.setArrayListType(ArrayListType.OwnedByUser)
					.setEntityType(Constants.SCHEMA_ENTITY_PLACE)
					.setUserId(mCommon.mUserId);
		}
		else if (target.equals("candigrams")) {
			intentBuilder = new IntentBuilder(this, CandiList.class);
			intentBuilder.setCommandType(CommandType.View)
					.setArrayListType(ArrayListType.OwnedByUser)
					.setEntityType(Constants.SCHEMA_ENTITY_POST)
					.setUserId(mCommon.mUserId);
		}

		Intent intent = intentBuilder.create();
		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToPage);
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void buildCandiUser(Context context, final User user) {

		final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		final WebImageView image = (WebImageView) findViewById(R.id.image);
		final TextView name = (TextView) findViewById(R.id.name);
		final TextView location = (TextView) findViewById(R.id.location);
		final TextView link = (TextView) findViewById(R.id.link);
		final TextView bio = (TextView) findViewById(R.id.bio);
		final TextView stats = (TextView) findViewById(R.id.stats);

		FontManager.getInstance().setTypefaceDefault(name);
		FontManager.getInstance().setTypefaceDefault(location);
		FontManager.getInstance().setTypefaceDefault(link);
		FontManager.getInstance().setTypefaceDefault(bio);
		FontManager.getInstance().setTypefaceDefault(stats);

		if (image != null) {
			String imageUri = null;

			if (user.photo != null) {
				imageUri = user.photo.getUri();
			}

			if (imageUri != null) {
				final BitmapRequestBuilder builder = new BitmapRequestBuilder(image).setImageUri(imageUri);
				final BitmapRequest imageRequest = builder.create();
				image.setBitmapRequest(imageRequest);
			}
		}

		/* Buttons */

		setVisibility(findViewById(R.id.button_tune), View.GONE);
		setVisibility(findViewById(R.id.button_like), View.GONE);
		setVisibility(findViewById(R.id.button_watch), View.GONE);

		if (!user.id.equals(Aircandi.getInstance().getUser().id)) {

			ComboButton watched = (ComboButton) findViewById(R.id.button_watch);
			if (watched != null) {
				if (mUser.byAppUser(Constants.TYPE_LINK_WATCH)) {
					final int color = Aircandi.getInstance().getResources().getColor(R.color.brand_pink_lighter);
					watched.setLabel(getString(R.string.candi_button_unwatch));
					watched.setDrawableId(R.drawable.ic_action_show_dark);
					watched.setAlpha(1);
					watched.getImageIcon().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
				}
				else {
					watched.setLabel(getString(R.string.candi_button_watch));
					watched.getImageIcon().setColorFilter(null);
					if (mCommon.mThemeTone.equals("dark")) {
						watched.setDrawableId(R.drawable.ic_action_show_dark);
					}
					else {
						watched.setDrawableId(R.drawable.ic_action_show_light);
					}
				}
				setVisibility(findViewById(R.id.button_watch), View.VISIBLE);
			}

			ComboButton liked = (ComboButton) findViewById(R.id.button_like);
			if (liked != null) {
				if (mUser.byAppUser(Constants.TYPE_LINK_LIKE)) {
					final int color = Aircandi.getInstance().getResources().getColor(R.color.accent_red);
					liked.setLabel(getString(R.string.candi_button_unlike));
					liked.setDrawableId(R.drawable.ic_action_heart_dark);
					liked.getImageIcon().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
				}
				else {
					liked.setLabel(getString(R.string.candi_button_like));
					liked.getImageIcon().setColorFilter(null);
					if (mCommon.mThemeTone.equals("dark")) {
						liked.setDrawableId(R.drawable.ic_action_heart_dark);
					}
					else {
						liked.setDrawableId(R.drawable.ic_action_heart_light);
					}
				}
				setVisibility(findViewById(R.id.button_like), View.VISIBLE);
			}
		}

		/* Description section */

		setVisibility(findViewById(R.id.section_details), View.GONE);

		setVisibility(name, View.GONE);
		if (name != null && user.name != null && !user.name.equals("")) {
			name.setText(Html.fromHtml(user.name));
			setVisibility(name, View.VISIBLE);
			setVisibility(findViewById(R.id.section_details), View.VISIBLE);
		}

		setVisibility(location, View.GONE);
		if (location != null && user.location != null && !user.location.equals("")) {
			location.setText(Html.fromHtml(user.area));
			setVisibility(location, View.VISIBLE);
			setVisibility(findViewById(R.id.section_details), View.VISIBLE);
		}

		setVisibility(link, View.GONE);
		if (link != null && user.webUri != null && !user.webUri.equals("")) {
			link.setText(Html.fromHtml(user.webUri));
			setVisibility(link, View.VISIBLE);
			setVisibility(findViewById(R.id.section_details), View.VISIBLE);
		}

		setVisibility(bio, View.GONE);
		if (bio != null && user.bio != null && !user.bio.equals("")) {
			bio.setText(Html.fromHtml(user.bio));
			setVisibility(bio, View.VISIBLE);
			setVisibility(findViewById(R.id.section_details), View.VISIBLE);
		}

		/* Stats */

		setVisibility(findViewById(R.id.section_stats), View.GONE);
		setVisibility(stats, View.GONE);
		final StringBuilder statString = new StringBuilder(500);
		if (user.getInCount(Constants.TYPE_LINK_LIKE) > 0) {
			statString.append("Likes: " + String.valueOf(user.getInCount(Constants.TYPE_LINK_LIKE)) + "<br/>");
		}
		if (user.getInCount(Constants.TYPE_LINK_WATCH) > 0) {
			statString.append("Watchers: " + String.valueOf(user.getInCount(Constants.TYPE_LINK_WATCH)) + "<br/>");
		}

		if (stats != null && user.stats != null && user.stats.size() > 0) {

			int tuneCount = 0;
			int tuneFirstCount = 0;
			int editCount = 0;
			int editFirstCount = 0;
			int contentCount = 0;
			int commentCount = 0;

			for (Stat stat : user.stats) {
				if (stat.type.startsWith("link_proximity")) {
					tuneCount += stat.countBy.intValue();
					if (stat.type.startsWith("link_proximity_first")) {
						tuneFirstCount += stat.countBy.intValue();
					}
				}

				if (stat.type.startsWith("update_entity")) {
					editCount += stat.countBy.intValue();
					if (stat.type.contains("_first")) {
						editFirstCount += stat.countBy.intValue();
					}
				}

				if (stat.type.equals("entity_proximity")) {
					tuneCount += stat.countBy.intValue();
				}

				if (stat.type.equals("insert_entity_place_custom")) {
					statString.append("Places created: " + String.valueOf(stat.countBy) + "<br/>");
				}
				else if (stat.type.equals("insert_entity_content")) {
					contentCount += stat.countBy.intValue();
				}
				else if (stat.type.equals("insert_entity_comment")) {
					commentCount += stat.countBy.intValue();
				}
				else if (stat.type.startsWith("update_entity_place")) {
					statString.append("Places edited: " + String.valueOf(stat.countBy) + "<br/>");
				}
			}

			if (contentCount > 0) {
				statString.append("Content created: " + String.valueOf(contentCount) + "<br/>");
			}

			if (commentCount > 0) {
				statString.append("Comments: " + String.valueOf(commentCount) + "<br/>");
			}

			if (editCount > 0) {
				statString.append("Places edited: " + String.valueOf(editCount) + "<br/>");
			}

			if (editFirstCount > 0) {
				statString.append("Places edited first: " + String.valueOf(editFirstCount) + "<br/>");
			}

			if (tuneCount > 0) {
				statString.append("Places tuned: " + String.valueOf(tuneCount) + "<br/>");
			}

			if (tuneFirstCount > 0) {
				statString.append("Places tuned first: " + String.valueOf(tuneFirstCount) + "<br/>");
			}

			stats.setText(Html.fromHtml(statString.toString()));
			setVisibility(stats, View.VISIBLE);
			setVisibility(findViewById(R.id.section_stats), View.VISIBLE);
		}

		/* All non-source children */
		List<Entity> places = new ArrayList<Entity>();
		List<Entity> candigrams = new ArrayList<Entity>();

		for (Entity entity : mEntities) {
			if (entity.type.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				places.add(entity);
			}
			else if (entity.type.equals(Constants.SCHEMA_ENTITY_POST)) {
				candigrams.add(entity);
			}
		}

		if (places.size() > 0) {
			ViewStub stub = (ViewStub) findViewById(R.id.stub_candi_places);
			if (stub != null) {
				((ViewStub) findViewById(R.id.stub_candi_places)).inflate();
			}
		}
		if (candigrams.size() > 0) {
			ViewStub stub = (ViewStub) findViewById(R.id.stub_candigrams);
			if (stub != null) {
				((ViewStub) findViewById(R.id.stub_candigrams)).inflate();
			}
		}

		setVisibility(findViewById(R.id.section_candi_places), View.GONE);
		setVisibility(findViewById(R.id.section_candigrams), View.GONE);

		if (places.size() > 0) {
			SectionLayout section = (SectionLayout) findViewById(R.id.section_candi_places).findViewById(R.id.section_layout_candi);
			if (section != null) {
				section.setHeaderTitle(context.getString(R.string.candi_section_user_candi_places));

				if (places.size() > getResources().getInteger(R.integer.candi_flow_limit)) {
					View footer = inflater.inflate(R.layout.temp_section_footer, null);
					Button button = (Button) footer.findViewById(R.id.button_more);
					FontManager.getInstance().setTypefaceDefault(button);
					button.setText(R.string.candi_section_places_more);
					button.setTag("places");
					section.setFooter(footer); // Replaces if there already is one.
				}

				FlowLayout flow = (FlowLayout) findViewById(R.id.section_candi_places).findViewById(R.id.flow_candi);
				drawCandi(context, flow, places.size() > getResources().getInteger(R.integer.candi_flow_limit)
						? places.subList(0, getResources().getInteger(R.integer.candi_flow_limit))
						: places, R.layout.temp_place_candi_item);

				setVisibility(findViewById(R.id.section_candi_places), View.VISIBLE);
			}
		}

		if (candigrams.size() > 0) {
			SectionLayout section = (SectionLayout) findViewById(R.id.section_candigrams).findViewById(R.id.section_layout_candi);
			if (section != null) {
				section.setHeaderTitle(context.getString(R.string.candi_section_user_candigrams));

				if (candigrams.size() > getResources().getInteger(R.integer.candi_flow_limit)) {
					View footer = inflater.inflate(R.layout.temp_section_footer, null);
					Button button = (Button) footer.findViewById(R.id.button_more);
					FontManager.getInstance().setTypefaceDefault(button);
					button.setText(R.string.candi_section_candigrams_more);
					button.setTag("candigrams");
					section.setFooter(footer); // Replaces if there already is one.
				}

				FlowLayout flow = (FlowLayout) findViewById(R.id.section_candigrams).findViewById(R.id.flow_candi);
				drawCandi(context, flow, candigrams.size() > getResources().getInteger(R.integer.candi_flow_limit)
						? candigrams.subList(0, getResources().getInteger(R.integer.candi_flow_limit))
						: candigrams, R.layout.temp_place_candi_item);

				setVisibility(findViewById(R.id.section_candigrams), View.VISIBLE);
			}
		}
	}

	static private void drawCandi(Context context, FlowLayout layout, List<Entity> entities, Integer viewResId) {

		layout.removeAllViews();
		final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		View parentView = (View) layout.getParent();
		Integer layoutWidthPixels = metrics.widthPixels
				- (parentView.getPaddingLeft() + parentView.getPaddingRight() + layout.getPaddingLeft() + layout.getPaddingRight());
		Integer bonusPadding = ImageUtils.getRawPixels(context, 20);
		layoutWidthPixels -= bonusPadding;

		Integer spacing = 2;
		Integer spacingHorizontalPixels = ImageUtils.getRawPixels(context, spacing);
		Integer spacingVerticalPixels = ImageUtils.getRawPixels(context, spacing);

		Integer desiredWidthPixels = (int) (metrics.density * 75);
		if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			desiredWidthPixels = (int) (metrics.density * 75);
		}

		Integer candiCount = (int) Math.ceil(layoutWidthPixels / desiredWidthPixels);
		Integer candiWidthPixels = (int) (layoutWidthPixels - (spacingHorizontalPixels * (candiCount - 1))) / candiCount;

		Integer candiHeightPixels = (int) (candiWidthPixels * 1);

		layout.setSpacingHorizontal(spacingHorizontalPixels);
		layout.setSpacingVertical(spacingVerticalPixels);

		/*
		 * Insert views for entities that we don't already have a view for
		 */
		for (Entity entity : entities) {

			View view = inflater.inflate(viewResId, null);
			WebImageView webImageView = (WebImageView) view.findViewById(R.id.image);

			TextView title = (TextView) view.findViewById(R.id.title);
			TextView badgeUpper = (TextView) view.findViewById(R.id.badge_upper);
			@SuppressWarnings("unused")
			TextView badgeLower = (TextView) view.findViewById(R.id.badge_lower);

			FontManager.getInstance().setTypefaceDefault(title);
			FontManager.getInstance().setTypefaceDefault(badgeUpper);

			if (entity.name != null && !entity.name.equals("")) {
				title.setText(entity.name);
			}
			else {
				title.setVisibility(View.GONE);
			}

			if (entity.photo == null && entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				Place place = (Place) entity;
				Boolean boostColor = !android.os.Build.MODEL.toLowerCase(Locale.US).equals("nexus 4");
				int color = Place.getCategoryColor(place.category != null ? place.category.name : null, true, boostColor, false);
				webImageView.getImageView().setColorFilter(color, PorterDuff.Mode.MULTIPLY);

				int colorResId = Place.getCategoryColorResId(place.category != null ? place.category.name : null, true, boostColor, false);
				if (view.findViewById(R.id.color_layer) != null) {
					((View) view.findViewById(R.id.color_layer)).setBackgroundResource(colorResId);
					((View) view.findViewById(R.id.color_layer)).setVisibility(View.VISIBLE);
				}
				else {
					webImageView.getImageView().setBackgroundResource(colorResId);
				}
			}

			String imageUri = entity.getPhotoUri();
			if (imageUri != null) {
				BitmapRequestBuilder builder = new BitmapRequestBuilder(webImageView).setImageUri(imageUri);
				BitmapRequest imageRequest = builder.create();
				webImageView.setSizeHint(candiWidthPixels);
				webImageView.setBitmapRequest(imageRequest);
				webImageView.setTag(entity);
			}

			FlowLayout.LayoutParams params = new FlowLayout.LayoutParams(candiWidthPixels, LayoutParams.WRAP_CONTENT);
			params.setCenterHorizontal(false);
			view.setLayoutParams(params);

			RelativeLayout.LayoutParams paramsImage = new RelativeLayout.LayoutParams(candiWidthPixels, candiHeightPixels);
			webImageView.setLayoutParams(paramsImage);

			layout.addView(view);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.edit) {
			Tracker.sendEvent("ui_action", "edit_user", null, 0, Aircandi.getInstance().getUser());
			mCommon.doEditUserClick();
			return true;
		}

		/* In case we add general menu items later */
		mCommon.doOptionsItemSelected(item);
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		/*
		 * We have to be pretty aggressive about refreshing the UI because
		 * there are lots of actions that could have happened while this activity
		 * was stopped that change what the user would expect to see.
		 * 
		 * - Entity deleted or modified
		 * - Entity children modified
		 * - New comments
		 * - Change in user which effects which candi and UI should be visible.
		 * - User profile could have been updated and we don't catch that.
		 */
		if (!isFinishing()) {
			if (mEntityModelRefreshDate != null
					&& ProximityManager.getInstance().getLastBeaconLoadDate() != null
					&& ProximityManager.getInstance().getLastBeaconLoadDate().longValue() > mEntityModelRefreshDate.longValue()) {
				invalidateOptionsMenu();
				bind(true);
			}
			else if (mEntityModelActivityDate != null
					&& EntityManager.getInstance().getEntityCache().getLastActivityDate() != null
					&& EntityManager.getInstance().getEntityCache().getLastActivityDate().longValue() > mEntityModelActivityDate.longValue()) {
				invalidateOptionsMenu();
				bind(true);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.candi_user;
	}
}