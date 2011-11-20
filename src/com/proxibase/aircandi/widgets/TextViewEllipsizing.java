package com.proxibase.aircandi.widgets;

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

	private static final String	ELLIPSIS	= "...";

	public interface EllipsizeListener {

		void ellipsizeStateChanged(boolean ellipsized);
	}

	private final List<EllipsizeListener>	ellipsizeListeners				= new ArrayList<EllipsizeListener>();
	private boolean							isEllipsized;
	private boolean							isStale;
	private boolean							programmaticChange;
	private String							fullText;
	private int								maxLines						= -1;
	private float							lineSpacingMultiplier			= 1.0f;
	private float							lineAdditionalVerticalPadding	= 0.0f;
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

	public void addEllipsizeListener(EllipsizeListener listener) {
		if (listener == null) {
			throw new NullPointerException();
		}
		ellipsizeListeners.add(listener);
	}

	public void removeEllipsizeListener(EllipsizeListener listener) {
		ellipsizeListeners.remove(listener);
	}

	public boolean isEllipsized() {
		return isEllipsized;
	}

	@Override
	public void setMaxLines(int maxLines) {
		super.setMaxLines(maxLines);
		this.maxLines = maxLines;
		isStale = true;
	}

	public int getMaxLines() {
		return maxLines;
	}

	@Override
	public void setLineSpacing(float add, float mult) {
		this.lineAdditionalVerticalPadding = add;
		this.lineSpacingMultiplier = mult;
		super.setLineSpacing(add, mult);
	}

	@Override
	protected void onTextChanged(CharSequence text, int start, int before, int after) {
		super.onTextChanged(text, start, before, after);
		if (!programmaticChange) {
			fullText = text.toString();
			isStale = true;
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (isStale) {
			super.setEllipsize(null);
			resetText();
		}

		if (mMirrorText) {
			/* This saves off the matrix that the canvas applies to draws, so it can be restored later. */
			canvas.save();

			canvas.scale(1.0f, -1.0f, super.getWidth() * 0.5f, super.getHeight() * 0.5f);

			/* draw the text with the matrix applied. */ 
			super.onDraw(canvas);

			/* restore the old matrix. */ 
			canvas.restore();
		}
		else {
			super.onDraw(canvas);
		}
	}

	private void resetText() {
		int maxLines = getMaxLines();
		String workingText = fullText;
		boolean ellipsized = false;
		if (maxLines != -1) {
			Layout layout = createWorkingLayout(workingText);
			if (layout.getLineCount() > maxLines) {
				workingText = fullText.substring(0, layout.getLineEnd(maxLines - 1)).trim();
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
		if (!workingText.equals(getText())) {
			programmaticChange = true;
			try {
				setText(workingText);
			}
			finally {
				programmaticChange = false;
			}
		}
		isStale = false;
		if (ellipsized != isEllipsized) {
			isEllipsized = ellipsized;
			for (EllipsizeListener listener : ellipsizeListeners) {
				listener.ellipsizeStateChanged(ellipsized);
			}
		}
	}

	private Layout createWorkingLayout(String workingText) {
		return new StaticLayout(workingText, getPaint(), getWidth() - getPaddingLeft() - getPaddingRight(),
				Alignment.ALIGN_NORMAL, lineSpacingMultiplier, lineAdditionalVerticalPadding, false);
	}

	@Override
	public void setEllipsize(TruncateAt where) {
	// Ellipsize settings are not respected
	}

	public void setMirrorText(boolean mirrorText) {
		this.mMirrorText = mirrorText;
	}

	public boolean isMirrorText() {
		return mMirrorText;
	}
}
