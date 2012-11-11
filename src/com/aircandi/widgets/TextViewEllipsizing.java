package com.aircandi.widgets;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.widget.TextView;

public class TextViewEllipsizing extends TextView {

	private static final String				ELLIPSIS						= "...";

	private final List<EllipsizeListener>	mEllipsizeListeners				= new ArrayList<EllipsizeListener>();
	private boolean							mIsEllipsized;
	private boolean							mIsStale;
	private boolean							mProgrammaticChange;
	private String							mFullText;
	private int								mMaxLines						= -1;
	private float							mLineSpacingMultiplier			= 1.0f;
	private float							mLineAdditionalVerticalPadding	= 0.0f;
	private boolean							mMirrorText						= false;

	public TextViewEllipsizing(Context context) {
		super(context);
	}

	public TextViewEllipsizing(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TextViewEllipsizing(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onTextChanged(CharSequence text, int start, int before, int after) {
		super.onTextChanged(text, start, before, after);
		if (!mProgrammaticChange) {
			mFullText = text.toString();
			mIsStale = true;
		}
	}

	@Override
	public void setText(CharSequence text, BufferType type) {
		super.setText(text, type);
		mFullText = text.toString();
		updateText();
	}

	@Override
	protected void onDraw(Canvas canvas) {

		if (mIsStale) {
			super.setEllipsize(null);
			updateText();
		}

		if (mMirrorText) {
			/* This saves off the matrix that the canvas applies to draws, so it can be restored later. */
			canvas.save();
			canvas.scale(1.0f, -1.0f, super.getWidth() * 0.5f, super.getHeight() * 0.5f);
			super.onDraw(canvas);
			canvas.restore();
		}
		else {
			super.onDraw(canvas);
		}
	}

	private void updateText() {

		int maxLines = mMaxLines;
		String workingText = mFullText;
		boolean ellipsized = false;

		try {
			if (maxLines != -1) {
				Layout layout = createWorkingLayout(workingText);
				if (layout.getLineCount() > maxLines) {
					workingText = workingText.substring(0, layout.getLineEnd(maxLines - 1)).trim();
					while (createWorkingLayout(workingText + ELLIPSIS).getLineCount() > maxLines) {
						int lastSpace = workingText.lastIndexOf(' ');
						if (lastSpace == -1) {
							break;
						}
						workingText = workingText.substring(0, lastSpace);
					}
					workingText = workingText + ELLIPSIS;
					ellipsized = true;
				}
			}

			if (ellipsized != mIsEllipsized) {
				mIsEllipsized = ellipsized;
				for (EllipsizeListener listener : mEllipsizeListeners) {
					listener.ellipsizeStateChanged(ellipsized);
				}
			}
		}
		catch (Exception exception) {
			/*
			 * Most likely happened because of rebuilding/recycling so we eat it
			 * i.e. StringIndexOutOfBoundsException
			 */
		}
	}

	private Layout createWorkingLayout(String workingText) {
		return new StaticLayout(workingText, getPaint(), getWidth() - getPaddingLeft() - getPaddingRight(),
				Alignment.ALIGN_NORMAL, mLineSpacingMultiplier, mLineAdditionalVerticalPadding, false);
	}

	public void addEllipsizeListener(EllipsizeListener listener) {
		if (listener == null) {
			throw new NullPointerException();
		}
		mEllipsizeListeners.add(listener);
	}

	public void removeEllipsizeListener(EllipsizeListener listener) {
		mEllipsizeListeners.remove(listener);
	}

	public boolean isEllipsized() {
		return mIsEllipsized;
	}

	public boolean isMirrorText() {
		return mMirrorText;
	}

	@Override
	public void setMaxLines(int maxLines) {
		super.setMaxLines(maxLines);
		this.mMaxLines = maxLines;
		mIsStale = true;
	}

	public int getMaxLines() {
		return mMaxLines;
	}

	@Override
	public void setLineSpacing(float add, float mult) {
		this.mLineAdditionalVerticalPadding = add;
		this.mLineSpacingMultiplier = mult;
		super.setLineSpacing(add, mult);
	}

	@Override
	public void setEllipsize(TruncateAt where) {
		// Ellipsize settings are not respected
	}

	public void setMirrorText(boolean mirrorText) {
		this.mMirrorText = mirrorText;
	}

	public interface EllipsizeListener {
		void ellipsizeStateChanged(boolean ellipsized);
	}

}
