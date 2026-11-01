package com.infinix.tetrispro;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity implements TetrisView.GameListener {
    private TetrisView tetrisView;
    private NextPieceView nextPieceView;
    private TextView txtScore;
    private ImageButton btnPause;
    private NativeSound sound;
    private MediaPlayer mediaPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int selectedLevel = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } catch (Throwable ignored) {}

        // Keep screen on & use notch edge-to-edge
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        setContentView(R.layout.activity_main);
        hideSystemUI();

        sound = new NativeSound(this);
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        tetrisView = findViewById(R.id.tetris_view);
        nextPieceView = findViewById(R.id.next_piece_view);
        txtScore = findViewById(R.id.txt_score);
        btnPause = findViewById(R.id.btn_pause);

        tetrisView.setGameListener(this);
        tetrisView.setSound(sound);
        tetrisView.setVibrator(vibrator);

        // Setup Controls (Left, Right, Down, Hard Drop, Rotate)
        setupRepeatButton(findViewById(R.id.btn_left), new Runnable() {
            @Override
            public void run() { tetrisView.moveLeft(); }
        }, 55, 150);

        setupRepeatButton(findViewById(R.id.btn_right), new Runnable() {
            @Override
            public void run() { tetrisView.moveRight(); }
        }, 55, 150);

        // Soft Down button with rapid repeat
        setupRepeatButton(findViewById(R.id.btn_down), new Runnable() {
            @Override
            public void run() { tetrisView.softDrop(); }
        }, 40, 100);

        setupInstantButton(findViewById(R.id.btn_drop), new Runnable() {
            @Override
            public void run() { tetrisView.hardDrop(); }
        });

        setupInstantButton(findViewById(R.id.btn_rotate), new Runnable() {
            @Override
            public void run() { tetrisView.rotatePiece(); }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tetrisView.togglePause();
                boolean isPaused = tetrisView.isPaused();
                btnPause.setImageResource(isPaused ? 
                    android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause);
                if (isPaused) {
                    pauseMusic();
                } else {
                    startMusic();
                }
            }
        });

        // Initialize background music
        initMusic();

        // Prompt native Level Slider on launch
        showLevelSliderDialog();
    }

    private void initMusic() {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.tetris_theme);
                if (mediaPlayer != null) {
                    mediaPlayer.setLooping(true);
                    mediaPlayer.setVolume(0.55f, 0.55f);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void startMusic() {
        try {
            initMusic();
            if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        } catch (Throwable ignored) {}
    }

    private void pauseMusic() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
        } catch (Throwable ignored) {}
    }

    private void setupRepeatButton(final View btn, final Runnable action, final long repeatInterval, final long initialDelay) {
        btn.setOnTouchListener(new View.OnTouchListener() {
            private Runnable repeatRunnable;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        try {
                            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                        } catch (Throwable ignored) {}
                        action.run();
                        repeatRunnable = new Runnable() {
                            @Override
                            public void run() {
                                action.run();
                                handler.postDelayed(this, repeatInterval);
                            }
                        };
                        handler.postDelayed(repeatRunnable, initialDelay);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setPressed(false);
                        if (repeatRunnable != null) {
                            handler.removeCallbacks(repeatRunnable);
                            repeatRunnable = null;
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void setupInstantButton(final View btn, final Runnable action) {
        btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        try {
                            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                        } catch (Throwable ignored) {}
                        action.run();
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setPressed(false);
                        return true;
                }
                return false;
            }
        });
    }

    private void showLevelSliderDialog() {
        pauseMusic();
        final Dialog dialog = new Dialog(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_level_slider);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        final TextView txtLevel = dialog.findViewById(R.id.txt_dialog_level);
        final TextView txtSpeed = dialog.findViewById(R.id.txt_dialog_speed);
        final SeekBar seekLevel = dialog.findViewById(R.id.seek_level);
        final Button btnStart = dialog.findViewById(R.id.btn_start_game);

        final String[] speedNames = new String[]{
            "Starter Speed (1000ms)",
            "Casual Speed (850ms)",
            "Easy Speed (720ms)",
            "Moderate Speed (600ms)",
            "Intermediate Speed (490ms)",
            "Brisk Speed (400ms)",
            "Fast Speed (320ms)",
            "Intense Speed (250ms)",
            "Expert Speed (190ms)",
            "Hyper Speed (140ms)"
        };

        seekLevel.setProgress(selectedLevel - 1);
        txtLevel.setText("LEVEL " + selectedLevel);
        txtSpeed.setText(speedNames[selectedLevel - 1]);

        seekLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedLevel = progress + 1;
                txtLevel.setText("LEVEL " + selectedLevel);
                txtSpeed.setText(speedNames[progress]);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                tetrisView.startNewGame(selectedLevel);
                startMusic();
                hideSystemUI();
            }
        });

        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    public void onScoreChanged(final int score) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (txtScore != null) {
                    txtScore.setText(String.format("%,d", score));
                }
            }
        });
    }

    @Override
    public void onNextPiece(final Tetromino next) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (nextPieceView != null) {
                    nextPieceView.setPiece(next);
                }
            }
        });
    }

    @Override
    public void onGameOver(final int finalScore) {
        pauseMusic();
        handler.post(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
                builder.setTitle("GAME OVER");
                builder.setMessage("Final Score: " + String.format("%,d", finalScore));
                builder.setPositiveButton("PLAY AGAIN", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showLevelSliderDialog();
                    }
                });
                builder.setCancelable(false);
                builder.show();
            }
        });
    }

    private void hideSystemUI() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                View decorView = getWindow().getDecorView();
                if (decorView != null) {
                    decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                    );
                }
            }
        } catch (Throwable ignored) {}
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseMusic();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tetrisView != null && !tetrisView.isPaused()) {
            startMusic();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Throwable ignored) {}
            mediaPlayer = null;
        }
        if (sound != null) {
            sound.release();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }
}
