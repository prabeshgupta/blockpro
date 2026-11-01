package com.infinix.tetrispro;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

public class NativeSound {
    private SoundPool soundPool;
    private int soundLandId;
    private int soundClearId;
    private int soundTetrisId;
    private int soundMoveId;
    private int soundRotateId;
    private int soundDropId;
    private int soundGameOverId;
    private boolean loaded = false;
    private boolean muted = false;

    public NativeSound(Context context) {
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

            soundPool = new SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(attrs)
                .build();

            soundLandId = soundPool.load(context, R.raw.sound_land, 1);
            soundClearId = soundPool.load(context, R.raw.sound_clear, 1);
            soundTetrisId = soundPool.load(context, R.raw.sound_tetris, 1);
            soundMoveId = soundPool.load(context, R.raw.sound_move, 1);
            soundRotateId = soundPool.load(context, R.raw.sound_rotate, 1);
            soundDropId = soundPool.load(context, R.raw.sound_drop, 1);
            soundGameOverId = soundPool.load(context, R.raw.sound_gameover, 1);
            loaded = true;
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void playLand() {
        if (!muted && loaded && soundPool != null) {
            soundPool.play(soundLandId, 0.75f, 0.75f, 1, 0, 1.0f);
        }
    }

    public void playClear(int lines) {
        if (!muted && loaded && soundPool != null) {
            if (lines >= 4) {
                soundPool.play(soundTetrisId, 0.9f, 0.9f, 2, 0, 1.0f);
            } else {
                soundPool.play(soundClearId, 0.8f, 0.8f, 2, 0, 1.0f);
            }
        }
    }

    public void playMove() {
        if (!muted && loaded && soundPool != null) {
            soundPool.play(soundMoveId, 0.4f, 0.4f, 1, 0, 1.0f);
        }
    }

    public void playRotate() {
        if (!muted && loaded && soundPool != null) {
            soundPool.play(soundRotateId, 0.55f, 0.55f, 1, 0, 1.0f);
        }
    }

    public void playDrop() {
        if (!muted && loaded && soundPool != null) {
            soundPool.play(soundDropId, 0.85f, 0.85f, 1, 0, 1.0f);
        }
    }

    public void playGameOver() {
        if (!muted && loaded && soundPool != null) {
            soundPool.play(soundGameOverId, 0.8f, 0.8f, 2, 0, 1.0f);
        }
    }

    public void toggleMute() {
        muted = !muted;
    }

    public boolean isMuted() {
        return muted;
    }

    public void release() {
        if (soundPool != null) {
            try {
                soundPool.release();
            } catch (Throwable ignored) {}
            soundPool = null;
        }
    }
}
