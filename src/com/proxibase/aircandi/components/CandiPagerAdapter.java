package com.proxibase.aircandi.components;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.proxibase.aircandi.R;
import com.proxibase.aircandi.CandiSearchActivity.PagerView;
import com.proxibase.aircandi.components.ImageManager.ImageRequest;
import com.proxibase.aircandi.components.ImageManager.ImageRequest.ImageShape;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.aircandi.widgets.ViewPagerIndicator;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.aircandi.widgets.ViewPagerIndicator.PageInfoProvider;
import com.proxibase.sdk.android.proxi.consumer.Command;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.consumer.User;

public class CandiPagerAdapter extends PagerAdapter implements ViewPagerIndicator.PageInfoProvider {

	private EntityProxy			mEntity;
	private ViewPagerIndicator	mViewPagerIndicator;
	private Context				mContext;
	private User				mUser;
	private ViewPager			mViewPager;
	private LayoutInflater		mInflater;

	public CandiPagerAdapter(Context context, ViewPager viewPager, ViewPagerIndicator viewPagerIndicator) {
		super();
		mContext = context;
		mViewPager = viewPager;
		mViewPagerIndicator = viewPagerIndicator;
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		if (mEntity == null) {
			return 0;
		}
		else {
			if (mEntity.children != null) {
				return mEntity.hasVisibleChildren() ? 2 : 1;
			}
			else
			{
				return 1;
			}
		}
	}

	@Override
	public void startUpdate(View arg0) {}

	@Override
	public Object instantiateItem(View collection, int position) {

		if (mEntity != null && mUser != null) {
			if (position == PagerView.CandiInfo.ordinal()) {

				ViewGroup candiInfoView = (ViewGroup) mInflater.inflate(R.layout.temp_candi_info, null);
				candiInfoView = buildCandiInfo(mEntity, candiInfoView);

				if (mEntity != null && mEntity.children != null && mEntity.hasVisibleChildren()) {
					mViewPagerIndicator.setVisibility(View.VISIBLE);
					mViewPagerIndicator.initialize(1, 2, (PageInfoProvider) mViewPager.getAdapter());
				}
				else {
					mViewPagerIndicator.setVisibility(View.GONE);
				}

				((ViewPager) collection).addView(candiInfoView, 0);
				return candiInfoView;
			}
			else if (position == PagerView.CandiList.ordinal()) {

				View candiInfoList = (View) mInflater.inflate(R.layout.temp_candi_list, null);
				final ListView candiListView = (ListView) candiInfoList.findViewById(R.id.list_candi_children);
				CandiListAdapter adapter = new CandiListAdapter(mContext, mUser, mEntity.children);
				candiListView.setAdapter(adapter);
				candiListView.setClickable(true);
				((ViewPager) collection).addView(candiInfoList, 0);
				return candiInfoList;
			}
		}
		return null;
	}

	public int getItemPosition(Object object) {
		return POSITION_NONE;
	}

	@Override
	public void destroyItem(View collection, int position, Object view) {
		((ViewPager) collection).removeView((View) view);
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == ((View) object);
	}

	@Override
	public void finishUpdate(View arg0) {}

	@Override
	public Parcelable saveState() {
		return null;
	}

	@Override
	public void restoreState(Parcelable state, ClassLoader loader) {}

	@Override
	public String getTitle(int position) {

		String entityType = null;
		if (mEntity != null) {
			entityType = mEntity.entityType;
			if (position == PagerView.CandiInfo.ordinal()) {
				if (entityType.equals(CandiConstants.TYPE_CANDI_GALLERY)) {
					return "GALLERY";
				}
				else if (entityType.equals(CandiConstants.TYPE_CANDI_TOPIC)) {
					return "TOPIC";
				}
				else if (entityType.equals(CandiConstants.TYPE_CANDI_WEB_BOOKMARK)) {
					return "BOOKMARK";
				}
			}
			else if (position == PagerView.CandiList.ordinal()) {
				if (entityType.equals(CandiConstants.TYPE_CANDI_GALLERY)) {
					return "PICTURES";
				}
				else if (entityType.equals(CandiConstants.TYPE_CANDI_TOPIC)) {
					return "POSTS";

				}
			}
		}
		return null;
	}

	public ViewGroup buildCandiInfo(final EntityProxy entity, final ViewGroup candiInfoView) {

		/* Build menus */
		TableLayout table = configureMenus(entity);
		if (table != null) {
			RelativeLayout slideContent = (RelativeLayout) candiInfoView.findViewById(R.id.candi_info_slider_content);
			slideContent.removeAllViews();
			slideContent.addView(table);
			((View) candiInfoView.findViewById(R.id.slide_actions_info)).setVisibility(View.VISIBLE);
		}
		else {
			((View) candiInfoView.findViewById(R.id.slide_actions_info)).setVisibility(View.GONE);
		}

		/* Update any UI indicators related to child candies */
		if (entity.children != null && entity.hasVisibleChildren()) {
			((ImageView) candiInfoView.findViewById(R.id.image_forward)).setVisibility(View.VISIBLE);
		}
		else {
			((ImageView) candiInfoView.findViewById(R.id.image_forward)).setVisibility(View.GONE);
		}

		final WebImageView image = (WebImageView) candiInfoView.findViewById(R.id.image_public);
		final ImageView imageReflection = (ImageView) candiInfoView.findViewById(R.id.image_public_reflection);
		final TextView title = (TextView) candiInfoView.findViewById(R.id.candi_info_title);
		final TextView subtitle = (TextView) candiInfoView.findViewById(R.id.candi_info_subtitle);
		final TextView description = (TextView) candiInfoView.findViewById(R.id.candi_info_description);
		final AuthorBlock authorBlock = (AuthorBlock) candiInfoView.findViewById(R.id.block_author);
		final Button comments = (Button) candiInfoView.findViewById(R.id.button_comments);

		/* Candi image */
		if (entity.imageUri != null && entity.imageUri.length() != 0) {
			ImageRequest imageRequest = new ImageRequest(entity.imageUri, ImageShape.Square, entity.imageFormat,
					entity.javascriptEnabled,
					CandiConstants.IMAGE_WIDTH_SEARCH_MAX, true, true, true, 1, this, null);
			image.setImageRequest(imageRequest, imageReflection);
		}

		/* Author block */
		if (entity.author != null) {
			authorBlock.bindToAuthor(entity.author, DateUtils.wcfToDate(entity.createdDate));
		}
		else {
			authorBlock.setVisibility(View.GONE);
		}

		/* Comments */
		comments.setText(String.valueOf(entity.commentCount) + (entity.commentCount == 1 ? "\nComment" : "\nComments"));
		comments.setTag(entity);

		/* Candi text */
		title.setText(null);
		subtitle.setText(null);
		description.setText(null);

		if (entity.title != null) {
			title.setText(Html.fromHtml(entity.title));
		}
		if (entity.subtitle != null) {
			subtitle.setText(Html.fromHtml(entity.subtitle));
		}
		if (entity.description != null) {
			description.setText(Html.fromHtml(entity.description));
		}

		return candiInfoView;
	}

	public TableLayout configureMenus(EntityProxy entity) {

		Boolean needMoreButton = false;

		if (entity.commands == null || entity.commands.size() == 0) {
			return null;
		}
		if (entity.commands.size() > 6) {
			needMoreButton = true;
		}

		/* Get the table we use for grouping and clear it */
		final TableLayout table = new TableLayout(mContext);

		/* Make the first row */
		TableRow tableRow = (TableRow) mInflater.inflate(R.layout.temp_tablerow_commands, null);
		final TableRow.LayoutParams rowLp = new TableRow.LayoutParams();
		rowLp.setMargins(0, 0, 0, 0);
		TableLayout.LayoutParams tableLp;

		/* Loop the streams */
		Integer commandCount = 0;
		RelativeLayout commandButtonContainer;
		for (Command command : entity.commands) {
			/*
			 * TODO: This is a temporary hack. The service shouldn't pass commands
			 * that this user doesn't have sufficient permissions for.
			 */
			if (command.name.toLowerCase().contains("edit")) {
				if (entity.createdById != null && !entity.createdById.toString().equals(mUser.id)) {
					continue;
				}
			}

			/* Make a button and configure it */
			command.entity = entity;
			commandButtonContainer = (RelativeLayout) mInflater.inflate(R.layout.temp_button_command, null);

			final TextView commandButton = (TextView) commandButtonContainer.findViewById(R.id.CommandButton);
			commandButtonContainer.setTag(command);
			if (needMoreButton && commandCount == 5) {
				commandButton.setText("More...");
				commandButton.setTag(command);
			}
			else {
				commandButton.setText(command.labelCustom != null ? command.labelCustom : command.label);
				commandButton.setTag(command);
			}

			/* Add button to row */
			tableRow.addView(commandButtonContainer, rowLp);
			commandCount++;

			/* If we have three in a row then commit it and make a new row */
			int newRow = 3;

			if (commandCount % newRow == 0) {
				tableLp = new TableLayout.LayoutParams();
				tableLp.setMargins(0, 0, 0, 0);
				table.addView(tableRow, tableLp);
				tableRow = (TableRow) mInflater.inflate(R.layout.temp_tablerow_commands, null);
			}
			else if (commandCount == 6) {
				break;
			}
		}

		/* We might have an uncommited row with stream buttons in it */
		if (commandCount != 3) {
			tableLp = new TableLayout.LayoutParams();
			tableLp.setMargins(0, 3, 0, 3);
			table.addView(tableRow, tableLp);
		}
		return table;
	}

	public void setEntity(EntityProxy entity) {
		this.mEntity = entity;
	}

	public EntityProxy getEntity() {
		return mEntity;
	}

	public void setUser(User user) {
		this.mUser = user;
	}

	public User getUser() {
		return mUser;
	}
}
