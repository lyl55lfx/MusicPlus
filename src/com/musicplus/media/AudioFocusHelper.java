package com.musicplus.media;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;

/**
 * Audio Focus 
 * @author Darcy
 * @version android.os.Build.VERSION.SDK_INT >= 8
 */
public class AudioFocusHelper implements AudioManager.OnAudioFocusChangeListener {
   
	private AudioManager mAudioManager;
	private MediaPlayer mMediaPlayer;
	private Context mContext;


    public AudioFocusHelper(Context ctx , MediaPlayer player) {
    	this.mContext = ctx;
        this.mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        this.mMediaPlayer = player;
    }

    public boolean requestFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
            mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN);
    }

    public boolean abandonFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.abandonAudioFocus(this);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
    	switch (focusChange) {
        case AudioManager.AUDIOFOCUS_GAIN:
            if (mMediaPlayer == null){
            	return;
            }else if (!mMediaPlayer.isPlaying()) {
            	mMediaPlayer.start();
            }
            mMediaPlayer.setVolume(0.5f, 0.5f);
            break;

        case AudioManager.AUDIOFOCUS_LOSS:
            if (mMediaPlayer.isPlaying()) 
            	mMediaPlayer.stop();
            mMediaPlayer.release();
            break;
        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            if (mMediaPlayer.isPlaying()) 
            	mMediaPlayer.pause();
            break;

        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
            if (mMediaPlayer.isPlaying()) 
            	mMediaPlayer.setVolume(0.1f, 0.1f);
            break;
    }

    }
}
