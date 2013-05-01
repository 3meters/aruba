package com.aircandi.ui;

import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.CommandType;
import com.aircandi.components.FontManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiManager;
import com.aircandi.components.ProxiManager.ArrayListType;
import com.aircandi.components.ProxiManager.ModelResult;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.ServiceEntryBase;
import com.aircandi.service.objects.User;
import com.aircandi.ui.base.CandiActivity;
import com.aircandi.ui.user.CandiUser;
import com.aircandi.ui.widgets.FlowLayout;
import com.aircandi.ui.widgets.SectionLayout;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.ImageUtils;

@SuppressWarnings("ucd")
public class CandiWatch extends CandiActivity {

	protected List<Entity>	mEntities;
	protected List<User>	mUsers;
	protected Number		mEntityModelRefreshDate;
	protected Number		mEntityModelActivityDate;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			initialize();
			bind(true);
		}
	}

	private void initialize() {
		mCommon.mActionBar.setIcon(R.drawable.img_watch);
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
				Thread.currentThread().setName("GetWatching");
				ModelResult result = ProxiManager.getInstance().getEntityModel()
						.getUserWatched(Aircandi.getInstance().getUser().id, "users", refresh, ProxiConstants.USER_WATCHING_USER_LIMIT);
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					mUsers = (List<User>) result.data;

					result = ProxiManager.getInstance().getEntityModel()
							.getUserWatched(Aircandi.getInstance().getUser().id, "entities", refresh, ProxiConstants.USER_WATCHING_ENTITY_LIMIT);
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
					mEntityModelRefreshDate = ProxiManager.getInstance().getEntityModel().getLastBeaconRefreshDate();
					mEntityModelActivityDate = ProxiManager.getInstance().getEntityModel().getLastActivityDate();
					buildCandiWatch(CandiWatch.this);
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
	public void onCandiClick(View view) {
		final ServiceEntryBase entry = (ServiceEntryBase) view.getTag();
		Intent intent = null;

		if (entry instanceof Entity) {
			Entity entity = (Entity) entry;

			final IntentBuilder intentBuilder = new IntentBuilder(this, CandiForm.class)
					.setCommandType(CommandType.View)
					.setEntityId(entity.id)
					.setParentEntityId(entity.parentId)
					.setEntityType(entity.type);

			if (entity.parentId != null) {
				intentBuilder.setCollectionId(entity.getParent().id);
			}
			intent = intentBuilder.create();
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		else if (entry instanceof User) {
			User user = (User) entry;
			intent = new Intent(this, CandiUser.class);
			intent.putExtra(CandiConstants.EXTRA_USER_ID, user.id);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		}

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
					.setEntityType(CandiConstants.TYPE_CANDI_PLACE)
					.setUserId(mCommon.mUserId);
		}
		else if (target.equals("candigrams")) {
			intentBuilder = new IntentBuilder(this, CandiList.class);
			intentBuilder.setCommandType(CommandType.View)
					.setArrayListType(ArrayListType.OwnedByUser)
					.setEntityType(CandiConstants.TYPE_CANDI_CANDIGRAM)
					.setUserId(mCommon.mUserId);
		}

		Intent intent = intentBuilder.create();
		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToPage);
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void buildCandiWatch(Context context) {

		final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		if (mEntities.size() > 0) {
			ViewStub stub = (ViewStub) findViewById(R.id.stub_candi_places);
			if (stub != null) {
				((ViewStub) findViewById(R.id.stub_candi_places)).inflate();
			}
		}
		if (mUsers.size() > 0) {
			ViewStub stub = (ViewStub) findViewById(R.id.stub_candi_users);
			if (stub != null) {
				((ViewStub) findViewById(R.id.stub_candi_users)).inflate();
			}
		}

		setVisibility(findViewById(R.id.section_candi_places), View.GONE);
		setVisibility(findViewById(R.id.section_candi_users), View.GONE);

		if (mEntities.size() > 0) {
			SectionLayout section = (SectionLayout) findViewById(R.id.section_candi_places).findViewById(R.id.section_layout_candi);
			if (section != null) {
				section.setHeaderTitle(context.getString(R.string.candi_section_watching_places));

				if (mEntities.size() > getResources().getInteger(R.integer.candi_flow_limit)) {
					View footer = inflater.inflate(R.layout.temp_section_footer, null);
					Button button = (Button) footer.findViewById(R.id.button_more);
					FontManager.getInstance().setTypefaceDefault(button);
					button.setText(R.string.candi_section_places_more);
					button.setTag("places");
					section.setFooter(footer); // Replaces if there already is one.
				}

				FlowLayout flow = (FlowLayout) findViewById(R.id.section_candi_places).findViewById(R.id.flow_candi);
				drawCandi(context, flow, mEntities.size() > getResources().getInteger(R.integer.candi_flow_limit)
						? mEntities.subList(0, getResources().getInteger(R.integer.candi_flow_limit))
						: mEntities, R.layout.temp_place_candi_item);

				setVisibility(findViewById(R.id.section_candi_places), View.VISIBLE);
			}
		}

		if (mUsers.size() > 0) {
			SectionLayout section = (SectionLayout) findViewById(R.id.section_candi_users).findViewById(R.id.section_layout_candi);
			if (section != null) {
				section.setHeaderTitle(context.getString(R.string.candi_section_watching_users));

				if (mUsers.size() > getResources().getInteger(R.integer.candi_flow_limit)) {
					View footer = inflater.inflate(R.layout.temp_section_footer, null);
					Button button = (Button) footer.findViewById(R.id.button_more);
					FontManager.getInstance().setTypefaceDefault(button);
					button.setText(R.string.candi_section_users_more);
					button.setTag("users");
					section.setFooter(footer); // Replaces if there already is one.
				}

				FlowLayout flow = (FlowLayout) findViewById(R.id.section_candi_users).findViewById(R.id.flow_candi);
				drawCandi(context, flow, mUsers.size() > getResources().getInteger(R.integer.candi_flow_limit)
						? mUsers.subList(0, getResources().getInteger(R.integer.candi_flow_limit))
						: mUsers, R.layout.temp_place_candi_item);

				setVisibility(findViewById(R.id.section_candi_users), View.VISIBLE);
			}
		}
	}

	static private void drawCandi(Context context, FlowLayout layout, List<? extends ServiceEntryBase> entries, Integer viewResId) {

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
		for (ServiceEntryBase entry : entries) {

			View view = inflater.inflate(viewResId, null);
			WebImageView webImageView = (WebImageView) view.findViewById(R.id.image);

			TextView title = (TextView) view.findViewById(R.id.title);
			TextView badgeUpper = (TextView) view.findViewById(R.id.badge_upper);
			@SuppressWarnings("unused")
			TextView badgeLower = (TextView) view.findViewById(R.id.badge_lower);

			FontManager.getInstance().setTypefaceDefault(title);
			FontManager.getInstance().setTypefaceDefault(badgeUpper);

			if (entry.name != null && !entry.name.equals("")) {
				title.setText(entry.name);
			}
			else {
				title.setVisibility(View.GONE);
			}

			if (entry instanceof Entity) {
				Entity entity = (Entity) entry;
				if (entity.photo == null && entity.place != null) {
					Boolean boostColor = !android.os.Build.MODEL.toLowerCase(Locale.US).equals("nexus 4");
					int color = Place.getCategoryColor(entity.place.category != null ? entity.place.category.name : null, true, boostColor, false);
					webImageView.getImageView().setColorFilter(color, PorterDuff.Mode.MULTIPLY);

					int colorResId = Place.getCategoryColorResId(entity.place.category != null ? entity.place.category.name : null, true, boostColor, false);
					if (view.findViewById(R.id.color_layer) != null) {
						((View) view.findViewById(R.id.color_layer)).setBackgroundResource(colorResId);
						((View) view.findViewById(R.id.color_layer)).setVisibility(View.VISIBLE);
					}
					else {
						webImageView.getImageView().setBackgroundResource(colorResId);
					}
				}
				String imageUri = entity.getEntityPhotoUri();
				if (imageUri != null) {
					BitmapRequestBuilder builder = new BitmapRequestBuilder(webImageView).setImageUri(imageUri);
					BitmapRequest imageRequest = builder.create();
					webImageView.setSizeHint(candiWidthPixels);
					webImageView.setBitmapRequest(imageRequest);
					webImageView.setTag(entry);
				}
			}
			else if (entry instanceof User) {
				User user = (User) entry;
				String imageUri = user.getUserPhotoUri();
				if (imageUri != null) {
					BitmapRequestBuilder builder = new BitmapRequestBuilder(webImageView).setImageUri(imageUri);
					BitmapRequest imageRequest = builder.create();
					webImageView.setSizeHint(candiWidthPixels);
					webImageView.setBitmapRequest(imageRequest);
					webImageView.setTag(entry);
				}
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
					&& ProxiManager.getInstance().getEntityModel().getLastBeaconRefreshDate() != null
					&& ProxiManager.getInstance().getEntityModel().getLastBeaconRefreshDate().longValue() > mEntityModelRefreshDate.longValue()) {
				invalidateOptionsMenu();
				bind(true);
			}
			else if (mEntityModelActivityDate != null
					&& ProxiManager.getInstance().getEntityModel().getLastActivityDate() != null
					&& ProxiManager.getInstance().getEntityModel().getLastActivityDate().longValue() > mEntityModelActivityDate.longValue()) {
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
		return R.layout.candi_watch;
	}
}