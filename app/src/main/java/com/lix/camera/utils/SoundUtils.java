
package com.lix.camera.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.lix.camera.R;

/**
 * Manages sounds played by the app. Since we have a limited set of sounds that can be played, we pre-load them and we
 * use hardcoded values to play them quickly.
 */
@SuppressWarnings("deprecation")
public class SoundUtils {

    public final static int SOUND_SHUTTER = 0;

    public final static int SOUND_FOCUS = 1;
    
    public final static int SOUND_FOCUS_FAIL = 2;
    
    public final static int SOUND_TIMER = 3;
    
    public final static int SOUND_CALIBRATION_OK = 4;

    private final static int SOUND_MAX = 5;

    private static SoundUtils sSingleton;

    public static SoundUtils getSingleton() {
        if (sSingleton == null) {
            sSingleton = new SoundUtils();
        }

        return sSingleton;
    }

    private SoundPool mSoundPool;

    private int[] mSoundsFD = new int[SOUND_MAX];

    /**
     * Default constructor Creates the sound pool to play the audio files. Make sure to call preload() before doing
     * anything so the sounds are loaded!
     */
    private SoundUtils() {
        mSoundPool = new SoundPool(SOUND_MAX, AudioManager.STREAM_NOTIFICATION, 0);
    }

    public void preload(Context ctx) {
        mSoundsFD[SOUND_SHUTTER] = mSoundPool.load(ctx, R.raw.snd_capture, 1);
        mSoundsFD[SOUND_FOCUS] = mSoundPool.load(ctx, R.raw.camera_focus, 1);
        mSoundsFD[SOUND_FOCUS_FAIL] = mSoundPool.load(ctx, R.raw.focus_failure, 1);
        mSoundsFD[SOUND_TIMER] = mSoundPool.load(ctx, R.raw.camera_timer, 1);
        mSoundsFD[SOUND_CALIBRATION_OK] = mSoundPool.load(ctx, R.raw.calibration_ok, 1);
    }

    /**
     * Immediately play the specified sound, Make sure preload() was called before doing play!
     * 
     * @param sound The sound to play, see SoundUtils.SOUND_*
     *
     */
    public void play(int sound) {
        mSoundPool.play(mSoundsFD[sound], 1.0f, 1.0f, 0, 0, 1.0f);
    }
}
