package com.infinix.tetrispro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class NextPieceView extends View {
    private Tetromino piece;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public NextPieceView(Context context) {
        super(context);
        init();
    }

    public NextPieceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        bgPaint.setColor(Color.parseColor("#12141D"));
        borderPaint.setColor(Color.parseColor("#242838"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.5f);
    }

    public void setPiece(Tetromino piece) {
        this.piece = piece;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();

        // Clean solid box
        rect.set(2, 2, w - 2, h - 2);
        canvas.drawRoundRect(rect, 10, 10, bgPaint);
        canvas.drawRoundRect(rect, 10, 10, borderPaint);

        if (piece == null) return;

        int[][] shape = piece.shape;
        int rows = shape.length;
        int cols = shape[0].length;

        float padX = 6f;
        float padY = 6f;
        float blockSize = Math.min((w - padX * 2) / cols, (h - padY * 2) / rows);
        if (blockSize > 14) blockSize = 14;

        float ox = (w - cols * blockSize) / 2f;
        float oy = (h - rows * blockSize) / 2f;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (shape[r][c] != 0) {
                    float bx = ox + c * blockSize;
                    float by = oy + r * blockSize;

                    // Clean solid block
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(piece.primaryColor);
                    rect.set(bx + 1, by + 1, bx + blockSize - 1, by + blockSize - 1);
                    canvas.drawRoundRect(rect, 3, 3, paint);

                    // Clean crisp bevel highlight
                    paint.setColor(piece.highlightColor);
                    paint.setAlpha(180);
                    canvas.drawLine(bx + 2, by + 2, bx + blockSize - 2, by + 2, paint);
                    canvas.drawLine(bx + 2, by + 2, bx + 2, by + blockSize - 2, paint);
                }
            }
        }
    }
}
