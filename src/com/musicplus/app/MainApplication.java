package com.musicplus.app;

import java.io.File;

import android.app.Application;
import android.os.Environment;

public class MainApplication extends Application{
	
	private static final String SD_ROOT = Environment.getExternalStorageDirectory().getPath();
	public static final String APP_EXTERNAL_ROOT_PATH = SD_ROOT + "/MusicPlus";
	public static final String TEMP_FILE_PATH = APP_EXTERNAL_ROOT_PATH + "/temp";
	public static final String TEMP_AUDIO_PATH = TEMP_FILE_PATH + "/audio";
	
	@Override
	public void onCreate() {
		super.onCreate();
		createStoreDirs();
	}
	
	private void createStoreDirs(){
		File tempAudioDir = new File(TEMP_AUDIO_PATH);
		if(!tempAudioDir.exists()){
			tempAudioDir.mkdirs();
		}
	}
}
