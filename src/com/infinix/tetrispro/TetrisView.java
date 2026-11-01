package com.infinix.tetrispro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TetrisView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    public interface GameListener {
        void onScoreChanged(int score);
        void onNextPiece(Tetromino next);
        void onGameOver(int finalScore);
    }

    private static final int COLS = 10;
    private static final int ROWS = 20;

    private SurfaceHolder holder;
    private Thread gameThread;
    private volatile boolean running = false;
    private boolean paused = false;

    // Clean, crisp paints (NO gradients, NO blurry shadows)
    private final Paint blockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ghostPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint();
    private final RectF blockRect = new RectF();

    private final Tetromino[][] grid = new Tetromino[ROWS][COLS];
    private final List<Tetromino> bag = new ArrayList<>();
    private final List<Tetromino> nextQueue = new ArrayList<>();
    private final Random random = new Random();

    private Tetromino currentPiece;
    private int currentX;
    private int currentY;

    private int score = 0;
    private int level = 1;
    private long dropInterval = 1000;
    private long lastDropTime = 0;

    private GameListener listener;
    private NativeSound sound;
    private Vibrator vibrator;

    private float blockSize = 30f;
    private float blockWidth = 30f;
    private float blockHeight = 30f;
    private float boardOffsetX = 0f;
    private float boardOffsetY = 0f;

    // Clean line clear animation flash
    private int clearingRow = -1;
    private long clearStartTime = 0;

    // Touch controls on board
    private float touchStartX, touchStartY;
    private long touchStartTime;

    public TetrisView(Context context) {
        super(context);
        init();
    }

    public TetrisView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        holder = getHolder();
        holder.addCallback(this);

        // Crisp, eye-pleasing subtle grid
        gridPaint.setColor(Color.parseColor("#151822"));
        gridPaint.setStrokeWidth(1f);

        shadowPaint.setColor(Color.argb(80, 0, 0, 0));
        shadowPaint.setStrokeWidth(1.2f);
        shadowPaint.setStyle(Paint.Style.STROKE);

        highlightPaint.setStrokeWidth(1.2f);
        highlightPaint.setStyle(Paint.Style.STROKE);

        ghostPaint.setStyle(Paint.Style.STROKE);
        ghostPaint.setStrokeWidth(1.2f);
    }

    public void setGameListener(GameListener listener) {
        this.listener = listener;
    }

    public void setSound(NativeSound sound) {
        this.sound = sound;
    }

    public void setVibrator(Vibrator vibrator) {
        this.vibrator = vibrator;
    }

    private void vibrate(long ms) {
        if (vibrator != null && vibrator.hasVibrator()) {
            try { vibrator.vibrate(ms); } catch (Throwable ignored) {}
        }
    }

    public void startNewGame(int startLevel) {
        synchronized (grid) {
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    grid[r][c] = null;
                }
            }
            bag.clear();
            nextQueue.clear();
            score = 0;
            this.level = startLevel;
            updateDropSpeed();

            if (listener != null) {
                listener.onScoreChanged(score);
            }

            refillBag();
            spawnPiece();
            paused = false;
        }
    }

    public void togglePause() {
        paused = !paused;
    }

    public boolean isPaused() {
        return paused;
    }

    private void updateDropSpeed() {
        long[] speeds = new long[]{1000, 850, 720, 600, 490, 400, 320, 250, 190, 140};
        dropInterval = speeds[Math.min(level - 1, speeds.length - 1)];
    }

    private void refillBag() {
        if (bag.isEmpty()) {
            String[] types = new String[]{"I", "O", "T", "S", "Z", "J", "L"};
            List<String> list = new ArrayList<>();
            Collections.addAll(list, types);
            Collections.shuffle(list, random);
            for (String t : list) {
                bag.add(Tetromino.create(t));
            }
        }
    }

    private Tetromino getNextFromBag() {
        refillBag();
        return bag.remove(0);
    }

    private void spawnPiece() {
        while (nextQueue.size() < 3) {
            nextQueue.add(getNextFromBag());
        }
        currentPiece = nextQueue.remove(0);
        currentX = (COLS - currentPiece.shape[0].length) / 2;
        currentY = 0;

        if (listener != null && !nextQueue.isEmpty()) {
            listener.onNextPiece(nextQueue.get(0));
        }

        if (checkCollision(currentPiece.shape, currentX, currentY)) {
            // Game Over
            if (sound != null) sound.playGameOver();
            vibrate(80);
            if (listener != null) {
                listener.onGameOver(score);
            }
            currentPiece = null;
        }
        lastDropTime = SystemClock.uptimeMillis();
    }

    private boolean checkCollision(int[][] shape, int ox, int oy) {
        int rows = shape.length;
        int cols = shape[0].length;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (shape[r][c] != 0) {
                    int x = ox + c;
                    int y = oy + r;
                    if (x < 0 || x >= COLS || y >= ROWS) return true;
                    if (y >= 0 && grid[y][x] != null) return true;
                }
            }
        }
        return false;
    }

    public void moveLeft() {
        synchronized (grid) {
            if (currentPiece == null || paused) return;
            if (!checkCollision(currentPiece.shape, currentX - 1, currentY)) {
                currentX--;
                if (sound != null) sound.playMove();
                vibrate(8);
            }
        }
    }

    public void moveRight() {
        synchronized (grid) {
            if (currentPiece == null || paused) return;
            if (!checkCollision(currentPiece.shape, currentX + 1, currentY)) {
                currentX++;
                if (sound != null) sound.playMove();
                vibrate(8);
            }
        }
    }

    public void softDrop() {
        synchronized (grid) {
            if (currentPiece == null || paused) return;
            if (!checkCollision(currentPiece.shape, currentX, currentY + 1)) {
                currentY++;
                score += 1;
                if (listener != null) listener.onScoreChanged(score);
                if (sound != null) sound.playMove();
                vibrate(6);
                lastDropTime = SystemClock.uptimeMillis();
            } else {
                lockPiece();
            }
        }
    }

    public void rotatePiece() {
        synchronized (grid) {
            if (currentPiece == null || paused) return;
            int n = currentPiece.shape.length;
            int[][] rotated = new int[n][n];
            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) {
                    rotated[c][n - 1 - r] = currentPiece.shape[r][c];
                }
            }
            int[] kicks = new int[]{0, -1, 1, -2, 2};
            for (int k : kicks) {
                if (!checkCollision(rotated, currentX + k, currentY)) {
                    currentPiece.shape = rotated;
                    currentX += k;
                    if (sound != null) sound.playRotate();
                    vibrate(10);
                    return;
                }
            }
        }
    }

    public void hardDrop() {
        synchronized (grid) {
            if (currentPiece == null || paused) return;
            int dropCount = 0;
            while (!checkCollision(currentPiece.shape, currentX, currentY + 1)) {
                currentY++;
                dropCount++;
            }
            score += dropCount * 2;
            if (listener != null) listener.onScoreChanged(score);
            if (sound != null) sound.playDrop();
            vibrate(20);
            lockPiece();
        }
    }

    private void lockPiece() {
        int rows = currentPiece.shape.length;
        int cols = currentPiece.shape[0].length;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (currentPiece.shape[r][c] != 0) {
                    int x = currentX + c;
                    int y = currentY + r;
                    if (y >= 0 && y < ROWS && x >= 0 && x < COLS) {
                        grid[y][x] = currentPiece;
                    }
                }
            }
        }
        if (sound != null) sound.playLand();
        vibrate(15);
        checkLines();
        spawnPiece();
    }

    private void checkLines() {
        int cleared = 0;
        for (int r = 0; r < ROWS; r++) {
            boolean full = true;
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] == null) {
                    full = false;
                    break;
                }
            }
            if (full) {
                cleared++;
                clearingRow = r;
                clearStartTime = SystemClock.uptimeMillis();

                // Shift rows down
                for (int y = r; y > 0; y--) {
                    System.arraycopy(grid[y - 1], 0, grid[y], 0, COLS);
                }
                for (int c = 0; c < COLS; c++) {
                    grid[0][c] = null;
                }
            }
        }

        if (cleared > 0) {
            int[] pts = new int[]{0, 100, 300, 500, 800};
            score += pts[Math.min(cleared, 4)] * level;
            if (listener != null) listener.onScoreChanged(score);
            if (sound != null) sound.playClear(cleared);
            vibrate(cleared >= 4 ? 40 : 20);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // 100% full width edge-to-edge! Zero vacant margin on left or right!
        blockWidth = (float) width / COLS;
        blockHeight = (float) height / ROWS;
        blockSize = blockWidth;
        boardOffsetX = 0f;
        boardOffsetY = 0f;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        try {
            if (gameThread != null) gameThread.join();
        } catch (InterruptedException ignored) {}
    }

    @Override
    public void run() {
        while (running) {
            long now = SystemClock.uptimeMillis();
            if (!paused && currentPiece != null) {
                if (now - lastDropTime > dropInterval) {
                    lastDropTime = now;
                    synchronized (grid) {
                        if (!checkCollision(currentPiece.shape, currentX, currentY + 1)) {
                            currentY++;
                        } else {
                            lockPiece();
                        }
                    }
                }
            }

            // High precision smooth rendering loop
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    drawCleanGame(canvas);
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }

            try {
                Thread.sleep(8); // ~120 FPS capable smooth loop
            } catch (InterruptedException ignored) {}
        }
    }

    /* Clean, Eye-Pleasing, Solid Matte Rendering (100% Width Edge-to-Edge) */
    private void drawCleanGame(Canvas canvas) {
        // Solid matte deep slate background (NO gradients, NO ai slop)
        canvas.drawColor(Color.parseColor("#090A0E"));

        // Crisp, subtle grid lines spanning edge to edge
        for (int c = 0; c <= COLS; c++) {
            float x = c * blockWidth;
            canvas.drawLine(x, 0, x, ROWS * blockHeight, gridPaint);
        }
        for (int r = 0; r <= ROWS; r++) {
            float y = r * blockHeight;
            canvas.drawLine(0, y, COLS * blockWidth, y, gridPaint);
        }

        // Locked Matrix Blocks
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] != null) {
                    drawCleanBlock(canvas, c * blockWidth, r * blockHeight, blockWidth, blockHeight, grid[r][c], false);
                }
            }
        }

        // Ghost Piece & Falling Piece
        if (currentPiece != null) {
            int ghostY = currentY;
            while (!checkCollision(currentPiece.shape, currentX, ghostY + 1)) {
                ghostY++;
            }

            int rows = currentPiece.shape.length;
            int cols = currentPiece.shape[0].length;

            // Draw Minimalist Ghost Piece (clean crisp outline)
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (currentPiece.shape[r][c] != 0) {
                        drawCleanBlock(canvas, (currentX + c) * blockWidth, (ghostY + r) * blockHeight, blockWidth, blockHeight, currentPiece, true);
                    }
                }
            }

            // Draw Active Piece
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (currentPiece.shape[r][c] != 0) {
                        drawCleanBlock(canvas, (currentX + c) * blockWidth, (currentY + r) * blockHeight, blockWidth, blockHeight, currentPiece, false);
                    }
                }
            }
        }

        // Subtle clean line clear flash
        if (clearingRow >= 0) {
            long elapsed = SystemClock.uptimeMillis() - clearStartTime;
            if (elapsed < 140) {
                Paint flashPaint = new Paint();
                flashPaint.setColor(Color.WHITE);
                flashPaint.setAlpha((int) ((1.0f - elapsed / 140f) * 200));
                canvas.drawRect(0, clearingRow * blockHeight, COLS * blockWidth, (clearingRow + 1) * blockHeight, flashPaint);
            } else {
                clearingRow = -1;
            }
        }
    }

    private void drawCleanBlock(Canvas canvas, float x, float y, float w, float h, Tetromino p, boolean isGhost) {
        float r = 3.5f;
        blockRect.set(x + 1f, y + 1f, x + w - 1f, y + h - 1f);

        if (isGhost) {
            ghostPaint.setColor(p.primaryColor);
            ghostPaint.setAlpha(160);
            canvas.drawRoundRect(blockRect, r, r, ghostPaint);
            return;
        }

        // Solid, clean, vibrant body
        blockPaint.setStyle(Paint.Style.FILL);
        blockPaint.setColor(p.primaryColor);
        canvas.drawRoundRect(blockRect, r, r, blockPaint);

        // Crisp 1px inner top/left bevel highlight
        highlightPaint.setColor(p.highlightColor);
        highlightPaint.setAlpha(170);
        canvas.drawLine(x + 2f, y + 2f, x + w - 2f, y + 2f, highlightPaint);
        canvas.drawLine(x + 2f, y + 2f, x + 2f, y + h - 2f, highlightPaint);

        // Crisp 1px inner bottom/right shade line
        canvas.drawLine(x + 2f, y + h - 2f, x + w - 2f, y + h - 2f, shadowPaint);
        canvas.drawLine(x + w - 2f, y + 2f, x + w - 2f, y + h - 2f, shadowPaint);

        // Clean modern center square accent (subtle flat detail)
        float cw = Math.min(w, h) * 0.26f;
        blockPaint.setColor(Color.argb(35, 255, 255, 255));
        canvas.drawRect(x + (w - cw) / 2f, y + (h - cw) / 2f, x + (w + cw) / 2f, y + (h + cw) / 2f, blockPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                touchStartTime = SystemClock.uptimeMillis();
                return true;
            case MotionEvent.ACTION_UP:
                float dx = event.getX() - touchStartX;
                float dy = event.getY() - touchStartY;
                long dur = SystemClock.uptimeMillis() - touchStartTime;

                if (dy > 60 && dur < 250 && Math.abs(dx) < 50) {
                    hardDrop();
                } else if (Math.hypot(dx, dy) < 20 && dur < 250) {
                    rotatePiece();
                } else if (Math.abs(dx) > 35) {
                    if (dx > 0) moveRight();
                    else moveLeft();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }
}
