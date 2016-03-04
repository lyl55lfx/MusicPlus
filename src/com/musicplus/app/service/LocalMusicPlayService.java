package com.musicplus.app.service;

import java.io.IOException;

import com.musicplus.media.AudioFocusHelper;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

/**
 * Local Music Play Service
 * @author Darcy
 *
 */
public class LocalMusicPlayService extends Service implements MusicPlayInterface , OnPreparedListener{
    
	private AudioFocusHelper mAudioFocusHelper;
	private MediaPlayer mAudioPlayer;
	private Uri mAudioUri;
	
	private boolean mPlaying;
	private boolean mPrepared;
	
	@Override
	public void onCreate() {
		super.onCreate();
		mAudioPlayer = new MediaPlayer();
		mAudioFocusHelper = new AudioFocusHelper(this,mAudioPlayer);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if(mAudioPlayer != null){
			mAudioPlayer.release();
			mAudioPlayer = null;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return new LocalMusicPlayBinder(this);
	}

	public static class LocalMusicPlayBinder extends Binder {
		
		LocalMusicPlayService service;
		
		LocalMusicPlayBinder(LocalMusicPlayService service){
			this.service = service;
		}
		
		public MusicPlayInterface getService() {
            return service;
        }
    }

	@Override
	public void play(Uri audioUri) {
		
		if(mAudioUri != null && mAudioUri.equals(audioUri) && mAudioPlayer.isPlaying()){
			mAudioPlayer.start();// start at the beginning
			return;
		}else{
			mAudioUri = audioUri;
		}
		
		mPlaying = false;
		mPrepared = false;
		mAudioPlayer.reset();
		mAudioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		try {
			mAudioPlayer.setOnPreparedListener(this);
			mAudioPlayer.setDataSource(this, audioUri);
			mAudioPlayer.prepareAsync();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void pause() {
		if(mPlaying){
			mAudioFocusHelper.abandonFocus();
			mAudioPlayer.pause();
			mPlaying = false;
		}
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		mAudioFocusHelper.requestFocus();
		mAudioPlayer.start();
		mPlaying = true;
		mPrepared = true;
	}

	@Override
	public void close() {
		mAudioPlayer.stop();
		mAudioPlayer.release();
		mPlaying = false;
		mPrepared = false;
	}

	@Override
	public void start() {
		if(mPrepared){
			mAudioPlayer.start();
		}
	}
}
