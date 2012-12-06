package com.aircandi;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.CommandType;
import com.aircandi.components.FontManager;
import com.aircandi.components.ImageRequest;
import com.aircandi.components.ImageRequestBuilder;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Entity.ImageFormat;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Stat;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.widgets.HorizontalScrollLayout;
import com.aircandi.widgets.SectionLayout;
import com.aircandi.widgets.WebImageView;

public class CandiUser extends CandiActivity {

	protected User			mUser;
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
		FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.name));
	}

	public void bind(Boolean refresh) {
		doBind(refresh);
	}

	public void doBind(final Boolean refresh) {
		/*
		 * Navigation setup for action bar icon and title
		 */
		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				ModelResult result = ProxiExplorer.getInstance().getEntityModel().getUser(mCommon.mUserId);
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					String jsonResponse = (String) result.serviceResponse.data;
					mUser = (User) ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.User).data;

					result = ProxiExplorer.getInstance().getEntityModel().getUserEntities(mCommon.mUserId, true, 25);
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
					mEntityModelRefreshDate = ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate();
					mEntityModelActivityDate = ProxiExplorer.getInstance().getEntityModel().getLastActivityDate();
					mCommon.mActionBar.setTitle(mUser.name);
					buildCandiUser(CandiUser.this, mUser);
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CandiUser);
				}
				mCommon.hideBusy();
			}

		}.execute();
	}

	public void doRefresh() {
		/*
		 * Called from AircandiCommon
		 */
		bind(true);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onMoreButtonClick(View view) {
		String target = (String) view.getTag();
		if (target.equals("candi")) {
			IntentBuilder intentBuilder = new IntentBuilder(this, UserCandiList.class);
			intentBuilder.setCommandType(CommandType.View)
					.setUserId(mUser.id);

			Intent intent = intentBuilder.create();
			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.CandiListToCandiForm);
		}
	}

	public void onCandiClick(View view) {
		Entity entity = (Entity) view.getTag();
		mCommon.showCandiFormForEntity(entity, UserCandiForm.class);
	}

	public void onImageClick(View view) {
		Intent intent = null;
		Photo photo = mUser.photo;
		if (photo.getImageFormat() == ImageFormat.Binary) {
			photo.setCreatedAt(mUser.photo.getCreatedAt());
			photo.setTitle(mUser.name);
			photo.setUser(mUser);
			ProxiExplorer.getInstance().getEntityModel().getPhotos().clear();
			ProxiExplorer.getInstance().getEntityModel().getPhotos().add(photo);
			intent = new Intent(this, PictureDetail.class);
			intent.putExtra(getString(R.string.EXTRA_URI), mUser.photo.getImageUri());

			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
		}
		else {
			AndroidManager.getInstance().callBrowserActivity(this, photo.getImageUri());
		}
	}

	public void onEditButtonClick(View view) {
		mCommon.doProfileClick();
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	public void buildCandiUser(Context context, final User user) {

		final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		final WebImageView image = (WebImageView) findViewById(R.id.image);
		final TextView name = (TextView) findViewById(R.id.name);
		final TextView location = (TextView) findViewById(R.id.location);
		final TextView link = (TextView) findViewById(R.id.link);
		final TextView bio = (TextView) findViewById(R.id.bio);
		final TextView stats = (TextView) findViewById(R.id.stats);

		FontManager.getInstance().setTypefaceLight(name);
		FontManager.getInstance().setTypefaceLight(location);
		FontManager.getInstance().setTypefaceLight(link);
		FontManager.getInstance().setTypefaceLight(bio);
		FontManager.getInstance().setTypefaceLight(stats);

		if (image != null) {
			image.getImageBadge().setVisibility(View.GONE);
			image.getImageZoom().setVisibility(View.GONE);
			String imageUri = null;

			if (user.photo != null) {
				imageUri = user.photo.getImageUri();
			}

			if (imageUri != null) {
				ImageFormat imageFormat = user.photo.getImageFormat();
				ImageRequestBuilder builder = new ImageRequestBuilder(image)
						.setImageUri(imageUri)
						.setImageFormat(imageFormat)
						.setLinkZoom(CandiConstants.LINK_ZOOM)
						.setLinkJavascriptEnabled(CandiConstants.LINK_JAVASCRIPT_ENABLED);

				ImageRequest imageRequest = builder.create();
				image.setImageRequest(imageRequest);
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
			for (Stat stat : user.stats) {
				if (stat.type.equals("tune_link_primary")) {
					statString += "Place tunings: " + String.valueOf(stat.countBy) + "<br/>";
				}
				else if (stat.type.equals("insert_entity")) {
					statString += "Candi created: " + String.valueOf(stat.countBy) + "<br/>";
				}
				else if (stat.type.equals("insert_entity_linked")) {
					statString += "First to tune: " + String.valueOf(stat.countBy) + "<br/>";
				}
				else if (stat.type.equals("insert_entity_custom")) {
					statString += "Custom places created: " + String.valueOf(stat.countBy) + "<br/>";
				}
			}
			stats.setText(Html.fromHtml(statString));
			setVisibility(stats, View.VISIBLE);
			setVisibility(findViewById(R.id.section_stats), View.VISIBLE);
		}

		/* Candi */

		setVisibility(findViewById(R.id.section_candi), View.GONE);
		if (mEntities.size() > 0) {

			SectionLayout section = (SectionLayout) findViewById(R.id.section_candi);
			section.getTextViewHeader().setText(String.valueOf(mEntities.size()) + " " + context.getString(R.string.candi_section_candi));
			HorizontalScrollLayout list = (HorizontalScrollLayout) findViewById(R.id.list_candi);

			for (Entity childEntity : mEntities) {
				View view = inflater.inflate(R.layout.temp_place_candi_item, null);
				WebImageView webImageView = (WebImageView) view.findViewById(R.id.image);

				String imageUri = childEntity.getImageUri();
				ImageRequestBuilder builder = new ImageRequestBuilder(webImageView)
						.setImageUri(imageUri)
						.setImageFormat(childEntity.getImageFormat())
						.setLinkZoom(CandiConstants.LINK_ZOOM)
						.setLinkJavascriptEnabled(CandiConstants.LINK_JAVASCRIPT_ENABLED);

				ImageRequest imageRequest = builder.create();
				webImageView.setImageRequest(imageRequest);
				webImageView.setTag(childEntity);

				list.addView(view);
			}

			if (mEntities.size() > 3) {
				View footer = inflater.inflate(R.layout.temp_section_footer, null);
				Button button = (Button) footer.findViewById(R.id.button_more);
				button.setText(R.string.candi_section_candi_more);
				button.setTag("candi");
				section.addView(footer);
			}

			setVisibility(findViewById(R.id.section_candi), View.VISIBLE);
		}

		/* Buttons */
		buildCandiButtons(context, user);
	}

	private void buildCandiButtons(Context context, final User user) {

		setVisibility(findViewById(R.id.button_edit), View.GONE);
		if (user.id.equals(Aircandi.getInstance().getUser().id)) {
			setVisibility(findViewById(R.id.button_edit), View.VISIBLE);
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
					&& ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate() != null
					&& ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate().longValue() > mEntityModelRefreshDate.longValue()) {
				invalidateOptionsMenu();
				bind(true);
			}
			else if (mEntityModelActivityDate != null
					&& ProxiExplorer.getInstance().getEntityModel().getLastActivityDate() != null
					&& ProxiExplorer.getInstance().getEntityModel().getLastActivityDate().longValue() > mEntityModelActivityDate.longValue()) {
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