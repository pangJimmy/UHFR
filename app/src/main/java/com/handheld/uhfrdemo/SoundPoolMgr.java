package com.handheld.uhfrdemo;

import java.lang.ref.WeakReference;

import com.handheld.uhfr.R;


import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import android.util.SparseIntArray;


/**
 * Audio player, pls set your application's targetSdkVersion to 21 or higher
 *
 * @author 79442
 * @date 2018/2/2
 */
@SuppressLint("NewApi")
public class SoundPoolMgr {

    /**
     * A weak reference with a Context, to avoid leak memory
     */
    private static WeakReference<Context> sWeakReference;
    private SoundPool mSoundPool;
    /**
     * Create a SparseIntArray object and manage various audios using SparseIntArray
     */
    private SparseIntArray mSoundArray = new SparseIntArray();

    /**
     * Inner class, Singleton mode
     */
    public static SoundPoolMgr getInstance(Context context) {
        if (sWeakReference == null) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Service.AUDIO_SERVICE);
            if (audioManager == null){
                Log.e("Huang,SoundPoolMgr", "initialize SoundPoolMgr fail with get AudioManager returns null");
            } else {
                /*
                 * Set media volume to maximum volume
                 */
                int maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
                int curMusic = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
                Log.e("Huang,SoundPoolMgr", "music_max:" + maxMusic + ", cur:" + curMusic);
                audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, maxMusic, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            }
            sWeakReference = new WeakReference<Context>(context);
        }
        return SoundPoolMgrHolder.sSoundPoolMgr;
    }

    private static class SoundPoolMgrHolder {
        private static SoundPoolMgr sSoundPoolMgr = new SoundPoolMgr(sWeakReference.get());
    }

    private SoundPoolMgr(Context context) {
        if (context == null) {
            Log.e("Huang,SoundPoolMgr", "initialize SoundPoolMgr fail with context == null");
        }
        SoundPool.Builder builder = new SoundPool.Builder();
        // Builder of AudioAttributes
        AudioAttributes.Builder attrBuild = new AudioAttributes.Builder();
        // Set streamType to system
        attrBuild.setLegacyStreamType(AudioManager.STREAM_SYSTEM);
        // Set AudioAttributes to SoundPool
        builder.setAudioAttributes(attrBuild.build());
        // Set the number of audio that can be played simultaneously
        builder.setMaxStreams(4);
        // Get SoundPool object
        mSoundPool = builder.build();
        // Load sound resource
        load(context);
    }

    /**
     * Load audio files stored in array
     */
    private void load(Context context) {
        // Put your sound resource in res/raw, the following parameter 1 is the soundID
        mSoundArray.put(1, mSoundPool.load(context, R.raw.msg, 1));
    }

    /**
     * Play a sound from a sound ID.
     *
     * @param soundId The position of the sound resource in the mSoundArray, specified in the method load().(Here, the soundID is 1)
     */
    public void play(int soundId) {
        int streamID = mSoundPool.play(mSoundArray.get(soundId), 1, 1, 0, 0, 1);
        android.util.Log.i("Huang,SoundPoolMgr", "play() return streamID: " + streamID);
    }

    /**
     * Release audio resources
     */
    public void release() {
//        mSoundPool.unload(mSoundArray.get(1));
//        mSoundPool.release();
//        mSoundPool = null;
        sWeakReference.clear();
        sWeakReference = null;
    }
}
