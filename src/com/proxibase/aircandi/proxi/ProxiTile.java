package com.proxibase.aircandi.proxi;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.modifier.FadeInModifier;
import org.anddev.andengine.entity.modifier.FadeOutModifier;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.sprite.AnimatedSprite;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.entity.text.Text;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.buffer.BufferObjectManager;
import org.anddev.andengine.opengl.font.Font;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.Texture.TextureSourceWithLocation;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
import org.anddev.andengine.opengl.util.GLHelper;
import org.anddev.andengine.util.HorizontalAlign;
import org.anddev.andengine.util.modifier.SequenceModifier;
import org.anddev.andengine.util.modifier.ease.EaseQuartOut;

import com.proxibase.aircandi.utilities.BitmapTextureSource;
import com.proxibase.sdk.android.core.ProxibaseService;
import com.proxibase.sdk.android.core.Utilities;
import com.proxibase.sdk.android.core.proxi.ProxiEntity;

import android.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.text.DynamicLayout;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

@SuppressWarnings("unused")
public class ProxiTile extends Entity {

	public static enum DisplayExtra {
		None, Level, Tag, Time
	}


	private Context						mContext;
	private final ProxiEntity			mProxiEntity;
	private ProxiUiHandler				mProxiHandler;
	public final int					mIndex;
	public int							mSlotVisible;

	public Texture						mBodyTexture		= null;
	public Texture						mTitleTexture		= null;

	public TiledTextureRegion			mBusyTextureRegion	= null;
	public TextureRegion				mBodyTextureRegion	= null;
	public TextureRegion				mTitleTextureRegion	= null;

	public Font							mFont;

	private ProxiRectangle				mBodyPlaceholderSprite;
	private ProxiAnimatedSprite			mBusyIndicatorSprite;
	public ProxiSprite					mBodySprite;
	public ProxiSprite					mTitleSprite;

	private DisplayExtra				mDisplayExtra;

	private boolean						mFirstDraw			= true;
	public OnProxiTileSingleTapListener	mSingleTapListener;

	private float						mInitialWidth		= ProxiConstants.PROXI_TILE_WIDTH;
	private float						mInitialHeight		= ProxiConstants.PROXI_TILE_HEIGHT;
	public float						mOldX				= 0;
	private boolean						mGrabbed			= false;
	private boolean						mDragged			= false;
	private float						mTouchStartX		= 0;
	private float						mTouchStartY		= 0;


	public ProxiTile(Context context, float x, float y, ProxiEntity proxiEntity, int index, ProxiUiHandler proxiController) {

		super(x, y);
		this.mProxiEntity = proxiEntity;
		this.mIndex = index;
		this.mContext = context;
		this.mProxiHandler = proxiController;

		mBodyTexture = new Texture(256, 512, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		mTitleTexture = new Texture(256, 64, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		mProxiHandler.getEngine().getTextureManager().loadTexture(mBodyTexture);
		mProxiHandler.getEngine().getTextureManager().loadTexture(mTitleTexture);

		while (!mBodyTexture.isLoadedToHardware())
			Thread.yield();
	}


	public void setBodyBitmap(String key) {

		if (mProxiHandler.getImageManager().hasImage(key)) {

			Bitmap bitmap = mProxiHandler.getImageManager().getImage(key);
			if (bitmap != null) {
				Bitmap bitmapCopy = bitmap.copy(Config.ARGB_8888, false); // Gets recycled once texture is loaded into
				if (bitmapCopy != null) {

					// mBodyTexture.clearTextureSources();
					mBodyTextureRegion = TextureRegionFactory.createFromSource(mBodyTexture, new BitmapTextureSource(bitmapCopy), 0, 0);

					mBodySprite = addBodySprite(false, 1);
					if (mBodySprite != null) {
						this.attachChild(mBodySprite);
						mProxiHandler.getEngine().getScene().registerTouchArea(mBodySprite);
						this.sortChildren();
					}
				}
			}
		}

	}


	public Bitmap getTextBitmap(int width, int height, CharSequence text) {

		// Initialize using a simple Paint object.
		final TextPaint tp = new TextPaint();
		tp.setTextSize(ProxiConstants.PROXI_FONT_SIZE);
		tp.setColor(Color.WHITE);
		tp.setTypeface(Typeface.SANS_SERIF);
		tp.setAntiAlias(true);

		StaticLayout sl = new StaticLayout(text, tp, width, Layout.Alignment.ALIGN_NORMAL, 0.95f, 0.0f, false);
		Bitmap bitmap = Bitmap.createBitmap(width, sl.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		sl.draw(canvas);

		return bitmap;
	}


	public void initialize() {

		this.setScaleCenter(ProxiConstants.PROXI_TILE_WIDTH * 0.5f, ProxiConstants.PROXI_TILE_HEIGHT * 0.5f);
		this.setColor(0, 0, 0, 0);

		// Add sprites
		mTitleSprite = addTitleSprite(false, 0);
		mBodySprite = addBodySprite(false, 1);
		mBodyPlaceholderSprite = addBodyPlaceholderSprite(false, 2);
		mBusyIndicatorSprite = addBusyIndicatorSprite(false, 3);

		this.attachChild(mBodyPlaceholderSprite);
		this.attachChild(mTitleSprite);
		this.attachChild(mBusyIndicatorSprite);
		if (mBodySprite != null)
			this.attachChild(mBodySprite);

		// ZOrder sort
		this.sortChildren();

		mProxiHandler.getEngine().getTextureManager().loadTexture(mTitleTexture);

		// // HACK: Don't continue until we are sure the texture has been completely loaded.
		// // This is to try and fix race conditions that cause textures to not appear properly.
		// while (!mBusyTexture.isLoadedToHardware())
		// Thread.yield();

	}


	protected ProxiAnimatedSprite addBusyIndicatorSprite(boolean visible, int zOrder) {

		ProxiAnimatedSprite sprite = new ProxiAnimatedSprite((mBodyPlaceholderSprite.getWidth() - mBusyTextureRegion.getTileWidth()) * 0.5f,
				((mBodyPlaceholderSprite.getHeight() + ProxiConstants.PROXI_TILE_TITLE_HEIGHT) - mBusyTextureRegion.getTileHeight()) * 0.5f,
				mBusyTextureRegion);

		sprite.setZIndex(zOrder);
		sprite.setVisible(visible);
		return sprite;
	}


	protected ProxiRectangle addBodyPlaceholderSprite(boolean visible, int zOrder) {

		ProxiRectangle sprite = new ProxiRectangle(0, ProxiConstants.PROXI_TILE_TITLE_HEIGHT, ProxiConstants.PROXI_TILE_WIDTH,
				ProxiConstants.PROXI_TILE_BODY_HEIGHT);

		sprite.setColor(1, 1, 1, 0);
		sprite.setZIndex(zOrder);
		sprite.setVisible(visible);
		return sprite;
	}


	protected ProxiSprite addTitleSprite(boolean visible, int zOrder) {

		// Title
		String title = mProxiEntity.entity.label;
		if (mDisplayExtra == DisplayExtra.Level) {
			title += String.valueOf(mProxiEntity.beacon.getAvgBeaconLevel());
		}
		else if (mDisplayExtra == DisplayExtra.Tag) {
			title += String.valueOf(mProxiEntity.beacon.beaconId);
		}
		else if (mDisplayExtra == DisplayExtra.Time) {
			title += String.valueOf(mProxiEntity.beacon.discoveryTime.getTime() / 100);
		}

		// Title sprite
		Bitmap bitmap = getTextBitmap(ProxiConstants.PROXI_TILE_WIDTH, ProxiConstants.PROXI_TILE_TITLE_HEIGHT, title);
		mTitleTextureRegion = TextureRegionFactory.createFromSource(mTitleTexture, new BitmapTextureSource(bitmap), 0, 0);
		ProxiSprite sprite = new ProxiSprite(0, ProxiConstants.PROXI_TILE_TITLE_HEIGHT - (bitmap.getHeight() + 5), mTitleTextureRegion);

		sprite.setBlendFunction(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
		sprite.setZIndex(zOrder);
		sprite.setVisible(visible);
		return sprite;
	}


	protected ProxiSprite addBodySprite(boolean visible, int zOrder) {

		if (mBodyTextureRegion == null)
			return null;

		ProxiSprite sprite = new ProxiSprite(0, ProxiConstants.PROXI_TILE_TITLE_HEIGHT, mBodyTextureRegion) {

			@Override
			public boolean onAreaTouched(final TouchEvent sceneTouchEvent, final float touchAreaLocalX, final float touchAreaLocalY) {

				// If we return true, the event won't make it to the scene touch handler
				if (sceneTouchEvent.isActionDown()) {
					mTouchStartX = sceneTouchEvent.getX();
					mTouchStartY = sceneTouchEvent.getY();
					mGrabbed = true;
					mDragged = false;
				}

				if (sceneTouchEvent.isActionMove()) {
					mDragged = true;
				}

				if (sceneTouchEvent.isActionUp()) {
					if (mGrabbed) {

						final int deltaX = (int) (mTouchStartX - sceneTouchEvent.getX());
						final int deltaY = (int) (mTouchStartY - sceneTouchEvent.getY());
						int distance = (deltaX * deltaX) + (deltaY * deltaY);

						// Treat it as a single tap
						if (distance < mProxiHandler.mTouchSlopSquare) {

							final Integer index = mIndex;
							if (mSingleTapListener != null) {
								ProxiTile proxiTile = (ProxiTile) this.getParent();
								mSingleTapListener.onProxiTileSingleTap(proxiTile);
							}
							mGrabbed = false;
							mDragged = false;
							return true;
						}
					}
					mGrabbed = false;
					mDragged = false;
				}

				return false;
			}

		};

		sprite.setBlendFunction(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
		sprite.setVisible(visible);
		sprite.setZIndex(zOrder);
		return sprite;

	}


	@Override
	protected void applyRotation(final GL10 pGL) {

		/* Disable culling so we can see the backside of this sprite. */
		GLHelper.disableCulling(pGL);

		final float rotation = this.mRotation;

		if (rotation != 0) {
			Utilities.Log(ProxibaseService.APP_NAME, "Tricorder", "Sprite: rotation = " + String.valueOf(rotation));

			final float rotationCenterX = this.mRotationCenterX;
			final float rotationCenterY = this.mRotationCenterY;

			pGL.glTranslatef(rotationCenterX, rotationCenterY, 0);
			// Note we are applying rotation around the y-axis and not the z-axis anymore!
			pGL.glRotatef(rotation, 0, 1, 0);
			pGL.glTranslatef(-rotationCenterX, -rotationCenterY, 0);
		}
	}


	public void showBusy() {

		mProxiHandler.getEngine().runOnUpdateThread(new Runnable() {

			@Override
			public void run() {

				mBusyIndicatorSprite.setVisible(true);
				mBusyIndicatorSprite.animate(150, true);

			}
		});
	}


	public void hideBusy() {

		mProxiHandler.getEngine().runOnUpdateThread(new Runnable() {

			@Override
			public void run() {

				mBusyIndicatorSprite.setVisible(false);
				mBusyIndicatorSprite.stopAnimation();

			}
		});
	}


	public void showBody() {

		mProxiHandler.getEngine().runOnUpdateThread(new Runnable() {

			@Override
			public void run() {

				mBodyPlaceholderSprite.registerEntityModifier(new AlphaModifier(1f, 0.1f, 0.0f));
				mBodySprite.registerEntityModifier(new AlphaModifier(1f, 0.0f, 1.0f));
				mBodySprite.setAlpha(0);
				mBodySprite.setVisible(true);

			}
		});

	}


	public void hideAll() {

		mProxiHandler.getEngine().runOnUpdateThread(new Runnable() {

			@Override
			public void run() {

				mBodySprite.registerEntityModifier(new AlphaModifier(1f, 1.0f, 0.0f));
				mTitleSprite.registerEntityModifier(new AlphaModifier(1f, 1.0f, 0.0f));

			}
		});
	}


	public void firstDraw() {

		mProxiHandler.getEngine().runOnUpdateThread(new Runnable() {

			@Override
			public void run() {

				mTitleSprite.setVisible(true);
				mBodyPlaceholderSprite.registerEntityModifier(new AlphaModifier(0.5f, 0.0f, 0.35f));
				mBodyPlaceholderSprite.setVisible(true);
			}
		});

	}


	public void unloadResources() {

		// Completely remove all resources associated with this sprite.

		mTitleSprite.removeResources();
		mBodySprite.removeResources();
		mBusyIndicatorSprite.removeResources();
		mBodyPlaceholderSprite.removeResources();

		BufferObjectManager.getActiveInstance().unloadBufferObject(mTitleTextureRegion.getTextureBuffer());
		BufferObjectManager.getActiveInstance().unloadBufferObject(mBodyTextureRegion.getTextureBuffer());
		BufferObjectManager.getActiveInstance().unloadBufferObject(mBusyTextureRegion.getTextureBuffer());

		mProxiHandler.getEngine().getTextureManager().unloadTexture(ProxiTile.this.mTitleTexture);
		mProxiHandler.getEngine().getTextureManager().unloadTexture(ProxiTile.this.mBodyTexture);
	}


	public Texture getBodyTexture() {

		return this.mBodyTexture;
	}


	public ProxiEntity getProxiEntity() {

		return this.mProxiEntity;
	}


	public void setSingleTapListener(OnProxiTileSingleTapListener listener) {

		mSingleTapListener = listener;
	}


	public void setBodyTexture(Texture mTexture) {

		this.mBodyTexture = mTexture;
	}


	public interface OnProxiTileSingleTapListener {

		void onProxiTileSingleTap(ProxiTile proxiTile);
	}
}
