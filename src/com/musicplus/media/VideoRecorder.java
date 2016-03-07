package com.musicplus.media;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.view.Surface;
import android.view.TextureView;

/**
 * 录制视频
 * 
 * @author Darcy
 */
public class VideoRecorder {

	private Activity mActivity;
	private MediaRecorder mMediaRecorder;
	private TextureView mPreview;
	private int mVidoeWidth,mVideoHeight;
	private Camera mCamera;
	private String mOutputFile;

	private OnVideoRecordListener mListener;
	private boolean isRecording = false;
	private boolean isValidCamera;

	public VideoRecorder(Activity activity , TextureView preview, String outputFile) {
		this.mActivity = activity;
		this.mPreview = preview;
		this.mOutputFile = outputFile;
	}

	public void setOnVideoRecordListener(OnVideoRecordListener l){
		this.mListener = l;
	}
	
	public void startPreview(){
		try {
			prepareCamera();
			mCamera.startPreview();
			isValidCamera = true;
		} catch (IOException e) {
			e.printStackTrace();
			isValidCamera = false;
		}
	}
	
	public void startRecord() {
		if(isValidCamera && !isRecording){
			new MediaPrepareTask().execute();
		}
	}

	class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... voids) {
			if (prepareVideoRecorder()) {
				isRecording = true;
			} else {
				release();
				return false;
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if(result){
				mMediaRecorder.start();
				if(mListener!= null)
					mListener.onStarted();
			}
		}
	}

	private void prepareCamera() throws IOException{
		mCamera = CameraHelper.getDefaultCameraInstance();
		setCameraDisplayOrientation(mActivity, Camera.CameraInfo.CAMERA_FACING_BACK, mCamera);
		
		int previewWidth = mPreview.getWidth();
		int previewHeight = mPreview.getHeight();
		Camera.Parameters parameters = mCamera.getParameters();
		List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
		Camera.Size optimalSize = CameraHelper.getOptimalPreviewSize(supportedPreviewSizes, previewWidth,previewHeight);
		optimalSize.width = 640;
		optimalSize.height = 480;
		parameters.setPreviewSize(optimalSize.width,optimalSize.height);
		//parameters.setRotation(90);
		mCamera.setParameters(parameters);
		mVidoeWidth = optimalSize.width;
		mVideoHeight = optimalSize.height;
		
        mCamera.setPreviewTexture(mPreview.getSurfaceTexture());
	}
	
	private void setCameraDisplayOrientation(Activity activity,
	         int cameraId, android.hardware.Camera camera) {
	     android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
	     android.hardware.Camera.getCameraInfo(cameraId, info);
	     int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
	     int degrees = 0;
	     switch (rotation) {
	         case Surface.ROTATION_0: degrees = 0; break;
	         case Surface.ROTATION_90: degrees = 90; break;
	         case Surface.ROTATION_180: degrees = 180; break;
	         case Surface.ROTATION_270: degrees = 270; break;
	     }

	     int result;
	     if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
	         result = (info.orientation + degrees) % 360;
	         result = (360 - result) % 360;  // compensate the mirror
	     } else {  // back-facing
	         result = (info.orientation - degrees + 360) % 360;
	     }
	     camera.setDisplayOrientation(result);
	 }
	
	private boolean prepareVideoRecorder() {
		
		mMediaRecorder = new MediaRecorder();
		
		mCamera.unlock();
		mMediaRecorder.setCamera(mCamera);
		
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

		mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		mMediaRecorder.setAudioChannels(MediaConstants.AUDIO_CHANNEL);
		mMediaRecorder.setAudioSamplingRate(MediaConstants.AUDIO_SAMPLE_RATE);

		mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		mMediaRecorder.setVideoFrameRate(MediaConstants.VIDEO_FRAME_RATE);
		mMediaRecorder.setVideoSize(mVidoeWidth, mVideoHeight);
		mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
		
		mMediaRecorder.setOutputFile(mOutputFile);

		try {
			mMediaRecorder.prepare();
		} catch (IllegalStateException e) {
			release();
			return false;
		} catch (IOException e) {
			release();
			return false;
		}
		return true;
	}

	public void release() {
		if (mMediaRecorder != null) {
			mMediaRecorder.reset();
			mMediaRecorder.release();
			mMediaRecorder = null;
		}
		
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
		
		isRecording = false;
	}
	
	public interface OnVideoRecordListener{
		void onStarted();
		void onError(int errorCode);
	}
}
