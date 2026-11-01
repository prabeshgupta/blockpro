package com.infinix.tetrispro;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;

/**
 * ImageButton that dynamically enforces a strict 1:1 square ratio
 * based on its measured width, maximizing the touch area across the row.
 */
public class SquareImageButton extends ImageButton {

    public SquareImageButton(Context context) {
        super(context);
    }

    public SquareImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int w = getMeasuredWidth();
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int h = (hMode != MeasureSpec.UNSPECIFIED) ? MeasureSpec.getSize(heightMeasureSpec) : Integer.MAX_VALUE;

        // Take the width or container height limit if constrained
        int size = (h > 0 && h < w) ? h : w;
        if (size > 0) {
            setMeasuredDimension(size, size);
        }
    }
}
