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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aircandi.CandiConstants;
import com.aircandi.R;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.CommandType;
import com.aircandi.components.FontManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiManager;
import com.aircandi.components.ProxiManager.ModelResult;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Stat;
import com.aircandi.service.objects.User;
import com.aircandi.ui.CandiForm;
import com.aircandi.ui.PictureDetail;
import com.aircandi.ui.base.CandiActivity;
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
				ModelResult result = ProxiManager.getInstance().getEntityModel().getUser(mCommon.mUserId);
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					String jsonResponse = (String) result.serviceResponse.data;
					mUser = (User) ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.User).data;

					result = ProxiManager.getInstance().getEntityModel().getUserEntities(mCommon.mUserId, true, 25);
					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						mEntities = (List<Entity>) result.data;
					}
				}

				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				ModelResult result = (ModelResult) modelResult;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					mEntityModelRefreshDate = ProxiManager.getInstance().getEntityModel().getLastBeaconRefreshDate();
					mEntityModelActivityDate = ProxiManager.getInstance().getEntityModel().getLastActivityDate();
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
	public void onCandiClick(View view) {
		Entity entity = (Entity) view.getTag();

		IntentBuilder intentBuilder = new IntentBuilder(this, CandiForm.class)
				.setCommandType(CommandType.View)
				.setEntityId(entity.id)
				.setParentEntityId(entity.parentId)
				.setEntityType(entity.type);

		if (entity.parentId != null) {
			intentBuilder.setCollectionId(entity.getParent().id);
		}

		Intent intent = intentBuilder.create();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToPage);
	}

	@SuppressWarnings("ucd")
	public void onImageClick(View view) {
		Intent intent = null;
		Photo photo = mUser.photo;
		photo.setCreatedAt(mUser.photo.getCreatedAt());
		photo.setTitle(mUser.name);
		photo.setUser(mUser);
		ProxiManager.getInstance().getEntityModel().getPhotos().clear();
		ProxiManager.getInstance().getEntityModel().getPhotos().add(photo);
		intent = new Intent(this, PictureDetail.class);
		intent.putExtra(CandiConstants.EXTRA_URI, mUser.photo.getUri());

		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToPage);
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void buildCandiUser(Context context, final User user) {

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
				BitmapRequestBuilder builder = new BitmapRequestBuilder(image).setImageUri(imageUri);
				BitmapRequest imageRequest = builder.create();
				image.setBitmapRequest(imageRequest);
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
			location.setText(Html.fromHtml(user.location));
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
		if (stats != null && user.stats != null && user.stats.size() > 0) {
			String statString = "";
			int tuneCount = 0;
			for (Stat stat : user.stats) {
				if (stat.type.equals("link_proximity")) {
					tuneCount += stat.countBy.intValue();
				}
				else if (stat.type.equals("entity_proximity")) {
					tuneCount += stat.countBy.intValue();
				}
				else if (stat.type.equals("insert_entity_place_custom")) {
					statString += "Places created: " + String.valueOf(stat.countBy) + "<br/>";
				}
				else if (stat.type.equals("insert_entity_picture")) {
					statString += "Pictures created: " + String.valueOf(stat.countBy) + "<br/>";
				}
				else if (stat.type.equals("insert_entity_post")) {
					statString += "Posts created: " + String.valueOf(stat.countBy) + "<br/>";
				}
				else if (stat.type.equals("insert_entity_place_linked")) {
					statString += "Places discovered first: " + String.valueOf(stat.countBy) + "<br/>";
				}
			}
			
			if (tuneCount > 0) {
				statString += "Tunings: " + String.valueOf(tuneCount) + "<br/>";
			}
			
			stats.setText(Html.fromHtml(statString));
			setVisibility(stats, View.VISIBLE);
			setVisibility(findViewById(R.id.section_stats), View.VISIBLE);
		}

		/* All non-source children */
		List<Entity> places = new ArrayList<Entity>();
		List<Entity> pictures = new ArrayList<Entity>();
		List<Entity> posts = new ArrayList<Entity>();

		for (Entity entity : mEntities) {
			if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
				places.add(entity);
			}
			else if (entity.type.equals(CandiConstants.TYPE_CANDI_PICTURE)) {
				pictures.add(entity);
			}
			else if (entity.type.equals(CandiConstants.TYPE_CANDI_POST)) {
				posts.add(entity);
			}
		}

		if (places.size() > 0) {
			ViewStub stub = (ViewStub) findViewById(R.id.stub_candi_places);
			if (stub != null) {
				((ViewStub) findViewById(R.id.stub_candi_places)).inflate();
			}
		}
		if (pictures.size() > 0) {
			ViewStub stub = (ViewStub) findViewById(R.id.stub_candi_pictures);
			if (stub != null) {
				((ViewStub) findViewById(R.id.stub_candi_pictures)).inflate();
			}
		}
		if (posts.size() > 0) {
			ViewStub stub = (ViewStub) findViewById(R.id.stub_candi_posts);
			if (stub != null) {
				((ViewStub) findViewById(R.id.stub_candi_posts)).inflate();
			}
		}

		setVisibility(findViewById(R.id.section_candi_places), View.GONE);
		setVisibility(findViewById(R.id.section_candi_pictures), View.GONE);
		setVisibility(findViewById(R.id.section_candi_posts), View.GONE);

		if (places.size() > 0) {
			SectionLayout section = (SectionLayout) findViewById(R.id.section_candi_places).findViewById(R.id.section_layout_candi);
			if (section != null) {
				section.setHeaderTitle(context.getString(R.string.candi_section_user_candi_places));
				FlowLayout flow = (FlowLayout) findViewById(R.id.section_candi_places).findViewById(R.id.flow_candi);
				drawCandi(context, flow, places, R.layout.temp_place_candi_item);
				setVisibility(findViewById(R.id.section_candi_places), View.VISIBLE);
			}
		}
		if (pictures.size() > 0) {
			SectionLayout section = (SectionLayout) findViewById(R.id.section_candi_pictures).findViewById(R.id.section_layout_candi);
			if (section != null) {
				section.setHeaderTitle(context.getString(R.string.candi_section_user_candi_pictures));
				FlowLayout flow = (FlowLayout) findViewById(R.id.section_candi_pictures).findViewById(R.id.flow_candi);
				drawCandi(context, flow, pictures, R.layout.temp_place_candi_item);
				setVisibility(findViewById(R.id.section_candi_pictures), View.VISIBLE);
			}
		}
		if (posts.size() > 0) {
			SectionLayout section = (SectionLayout) findViewById(R.id.section_candi_posts).findViewById(R.id.section_layout_candi);
			if (section != null) {
				section.setHeaderTitle(context.getString(R.string.candi_section_user_candi_posts));
				FlowLayout flow = (FlowLayout) findViewById(R.id.section_candi_posts).findViewById(R.id.flow_candi);
				drawCandi(context, flow, posts, R.layout.temp_place_candi_item);
				setVisibility(findViewById(R.id.section_candi_posts), View.VISIBLE);
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

		Integer spacing = 3;
		Integer spacingHorizontalPixels = ImageUtils.getRawPixels(context, spacing);
		Integer spacingVerticalPixels = ImageUtils.getRawPixels(context, spacing);

		Integer desiredWidthPixels = (int) (metrics.xdpi * 0.45f);
		if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			desiredWidthPixels = (int) (metrics.ydpi * 0.45f);
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
			TextView badge = (TextView) view.findViewById(R.id.badge);

			FontManager.getInstance().setTypefaceDefault(title);
			FontManager.getInstance().setTypefaceDefault(badge);

			if (entity.type.equals(CandiConstants.TYPE_CANDI_SOURCE)) {
				if (entity.source.name != null && entity.source.name.equals("comments")) {
					if (entity.commentCount != null && entity.commentCount > 0) {
						badge.setText(String.valueOf(entity.commentCount));
						badge.setVisibility(View.VISIBLE);
					}
					else {
						badge.setVisibility(View.GONE);
					}
				}
				title.setText(entity.name);
				title.setVisibility(View.VISIBLE);
			}
			else {
				if (entity.name != null && !entity.name.equals("")) {
					title.setText(entity.name);
					title.setVisibility(View.VISIBLE);
				}
			}

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
		return R.layout.candi_user;
	}
}