package com.proxibase.aircandi.candi.views;

import java.util.Observable;
import java.util.Observer;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.opengl.buffer.BufferObjectManager;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
import org.anddev.andengine.opengl.util.GLHelper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.text.DynamicLayout;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import com.proxibase.aircandi.candi.models.BaseModel;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;
import com.proxibase.aircandi.candi.sprites.CandiAnimatedSprite;
import com.proxibase.aircandi.candi.sprites.CandiSprite;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.utils.BitmapTextureSource;
import com.proxibase.aircandi.utils.BitmapTextureSource.IBitmapAdapter;

public abstract class BaseView extends Entity implements Observer, IView {

	protected CandiPatchPresenter	mCandiPatchPresenter;
	protected Object				mModel;

	protected TiledTextureRegion	mProgressTextureRegion;
	protected CandiAnimatedSprite	mProgressSprite;

	protected TextureRegion			mPlaceholderTextureRegion;
	protected CandiSprite			mPlaceholderSprite;

	protected boolean				mBound							= false;

	protected Texture				mTitleTexture;
	private TextureRegion			mTitleTextureRegion;
	protected CandiSprite			mTitleSprite;

	protected String				mTitleText;
	private int						mTitleTextColor;
	protected boolean				mHardwareTexturesInitialized	= false;

	public BaseView() {
		this(null, null);
	}

	public BaseView(CandiPatchPresenter candiPatchPresenter) {
		this(null, candiPatchPresenter);
	}

	public BaseView(Object model, CandiPatchPresenter candiPatchPresenter) {
		super();

		mModel = model;
		setCandiPatchPresenter(candiPatchPresenter);
		makeProgressSprites();
	}

	public void initialize() {
		updateTextureSources();
		mBound = true;
	}

	public void initializeModel() {
		updateTextureSources();
		mBound = true;
	}

	@Override
	public void update(Observable observable, Object data) {
		/*
		 * Any requested display extras are added to the title text by
		 * the getTitleText() method.
		 */
		String titleTextModel = ((BaseModel) mModel).getTitleText();
		if (mTitleSprite != null && titleTextModel != null && !titleTextModel.equals(mTitleText)) {
			mTitleText = titleTextModel;
			updateTextureSources();
		}
	}

	private void makeTitleSprite() {
		mTitleSprite = new CandiSprite(0,
				CandiConstants.CANDI_VIEW_TITLE_HEIGHT - (mTitleTextureRegion.getHeight() + CandiConstants.CANDI_VIEW_TITLE_SPACER_HEIGHT),
				mTitleTextureRegion);
		mTitleSprite.setBlendFunction(CandiConstants.GL_BLEND_FUNCTION_SOURCE, CandiConstants.GL_BLEND_FUNCTION_DESTINATION);
		mTitleSprite.setAlpha(0);
		mTitleSprite.setVisible(false);
		mTitleSprite.setZIndex(0);
		attachChild(mTitleSprite);
	}

	private void makeProgressSprites() {

		// Placeholder
		mPlaceholderTextureRegion = mCandiPatchPresenter.mPlaceholderTextureRegion.clone();
		mPlaceholderSprite = new CandiSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, mPlaceholderTextureRegion);
		mPlaceholderSprite.setBlendFunction(CandiConstants.GL_BLEND_FUNCTION_SOURCE, CandiConstants.GL_BLEND_FUNCTION_DESTINATION);
		mPlaceholderSprite.setAlpha(0);
		mPlaceholderSprite.setVisible(false);
		mPlaceholderSprite.setZIndex(-10);
		attachChild(mPlaceholderSprite);

		// Progress
		mProgressTextureRegion = mCandiPatchPresenter.mProgressTextureRegion.clone();
		float progressX = (mPlaceholderSprite.getWidth() - mProgressTextureRegion.getTileWidth()) * 0.5f;
		float progressY = CandiConstants.CANDI_VIEW_TITLE_HEIGHT + (mPlaceholderSprite.getHeight() * 0.5f)
							- (mProgressTextureRegion.getTileHeight() * 0.5f);
		mProgressSprite = new CandiAnimatedSprite(progressX, progressY, mProgressTextureRegion);
		mProgressSprite.setBlendFunction(CandiConstants.GL_BLEND_FUNCTION_SOURCE, CandiConstants.GL_BLEND_FUNCTION_DESTINATION);
		mProgressSprite.setAlpha(0);
		mProgressSprite.setVisible(false);
		mProgressSprite.setZIndex(-9);
		attachChild(mProgressSprite);
	}

	protected void updateTextureSources() {
		if (((BaseModel) mModel).getTitleText() != null) {

			mTitleTexture.clearTextureSources();
			Bitmap titleBitmap = makeTextBitmap(CandiConstants.CANDI_VIEW_WIDTH, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, ((BaseModel) mModel)
					.getTitleText());
			mTitleTextureRegion = TextureRegionFactory.createFromSource(mTitleTexture, new BitmapTextureSource(titleBitmap, new IBitmapAdapter() {

				@Override
				public Bitmap reloadBitmap() {
					/*
					 * This gets called if the bitmap being used as the texture source has been recycled
					 */
					Bitmap titleBitmap = null;
					if (mModel != null) {
						titleBitmap = makeTextBitmap(CandiConstants.CANDI_VIEW_WIDTH, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, ((BaseModel) mModel)
								.getTitleText());
					}
					return titleBitmap;
				}
			}), 0, 0);

			if (mTitleSprite == null) {
				makeTitleSprite();
			}
		}
	}

	public void progressVisible(boolean visible) {
		if (visible) {
			mProgressSprite.setVisible(true);
			mProgressSprite.setAlpha(1);
			mProgressSprite.animate(150, true);
		}
		else {
			mProgressSprite.setVisible(false);
			mProgressSprite.stopAnimation();
		}
	}

	private Bitmap makeTextBitmap(int width, int height, CharSequence text) {
		final TextPaint tp = new TextPaint();
		tp.setTextSize(CandiConstants.CANDI_VIEW_FONT_SIZE);
		tp.setColor(mTitleTextColor);
		tp.setTypeface(Typeface.SANS_SERIF);
		tp.setAntiAlias(true);
		// tp.setXfermode(new PorterDuffXfermode(Mode.DARKEN));
		// tp.setShadowLayer(1f, 1, 1, Color.parseColor("#ffffff"));

		DynamicLayout textLayout = new DynamicLayout(text, text, tp, width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false,
				TextUtils.TruncateAt.END, CandiConstants.CANDI_VIEW_WIDTH);
		int cappedHeight = textLayout.getHeight() > CandiConstants.CANDI_VIEW_TITLE_HEIGHT ? CandiConstants.CANDI_VIEW_TITLE_HEIGHT : textLayout
				.getHeight();
		Bitmap bitmap = Bitmap.createBitmap(width, cappedHeight, CandiConstants.IMAGE_CONFIG_DEFAULT);
		Canvas canvas = new Canvas(bitmap);
		textLayout.draw(canvas);
		canvas = null;

		return bitmap;
	}

	@SuppressWarnings("unused")
	private Bitmap makeTextBitmapStatic(int width, int height, CharSequence text) {
		final TextPaint tp = new TextPaint();
		tp.setTextSize(CandiConstants.CANDI_VIEW_FONT_SIZE);
		tp.setColor(mTitleTextColor);
		tp.setTypeface(Typeface.SANS_SERIF);
		tp.setAntiAlias(true);

		StaticLayout sl = new StaticLayout(text, tp, width, Layout.Alignment.ALIGN_NORMAL, 0.95f, 0.0f, false);
		int cappedHeight = sl.getHeight() > CandiConstants.CANDI_VIEW_TITLE_HEIGHT ? CandiConstants.CANDI_VIEW_TITLE_HEIGHT : sl.getHeight();
		Bitmap bitmap = Bitmap.createBitmap(width, cappedHeight, CandiConstants.IMAGE_CONFIG_DEFAULT);
		Canvas canvas = new Canvas(bitmap);
		sl.draw(canvas);
		canvas = null;

		return bitmap;
	}

	public void unloadResources() {
		/*
		 * Completely remove all resources associated with this sprite.
		 */
		if (mTitleSprite != null)
			mTitleSprite.removeResources();

		if (mTitleTextureRegion != null)
			BufferObjectManager.getActiveInstance().unloadBufferObject(mTitleTextureRegion.getTextureBuffer());

		if (mTitleTexture != null)
			mCandiPatchPresenter.getEngine().getTextureManager().unloadTexture(mTitleTexture);
	}

	public void resetTextureSources() {
		updateTextureSources();
	}

	public void loadHardwareTextures() {
		mTitleTexture = new Texture(256, 128, CandiConstants.GL_TEXTURE_OPTION);
		mCandiPatchPresenter.getEngine().getTextureManager().loadTextures(mTitleTexture);
	}

	@Override
	protected void applyRotation(final GL10 pGL) {

		// Disable culling so we can see the backside of this sprite.
		GLHelper.disableCulling(pGL);

		final float rotation = mRotation;

		if (rotation != 0) {
			final float rotationCenterX = mRotationCenterX;
			final float rotationCenterY = mRotationCenterY;

			pGL.glTranslatef(rotationCenterX, rotationCenterY, 0);
			// Note we are applying rotation around the y-axis and not the z-axis anymore!
			pGL.glRotatef(rotation, 0, 1, 0);
			pGL.glTranslatef(-rotationCenterX, -rotationCenterY, 0);
		}
	}

	public BaseView setTitleTextColor(int titleTextColor) {
		this.mTitleTextColor = titleTextColor;
		return this;
	}

	public BaseView setCandiPatchPresenter(CandiPatchPresenter candiPatchPresenter) {
		this.mCandiPatchPresenter = candiPatchPresenter;
		return this;
	}

	protected boolean isVisibleToCamera(final Camera camera) {

		if (mPlaceholderSprite != null && mPlaceholderSprite.isVisibleToCamera(camera)) {
			return true;
		}
		else if (mTitleSprite != null && mTitleSprite.isVisibleToCamera(camera)) {
			return true;
		}
		return false;
	}

	@Override
	public void setAlpha(float alpha) {
		super.setAlpha(alpha);

		for (int i = 0; i < getChildCount(); i++) {
			getChild(i).setAlpha(alpha);
		}
	}

	@Override
	public void setZIndex(final int zIndex) {
		super.setZIndex(zIndex);

		for (int i = 0; i < getChildCount(); i++) {
			getChild(i).setZIndex(zIndex);
		}
	}

	public BaseModel getModel() {
		return (BaseModel) mModel;
	}

	public BaseView setModel(Object model) {
		mModel = model;
		return this;
	}

	public void setHardwareTexturesInitialized(boolean hardwareTexturesInitialized) {
		this.mHardwareTexturesInitialized = hardwareTexturesInitialized;
	}

	public boolean isHardwareTexturesInitialized() {
		return mHardwareTexturesInitialized;
	}

	public void setUnbound(boolean unbound) {
		this.mBound = unbound;
	}

	public boolean isUnbound() {
		return mBound;
	}

}