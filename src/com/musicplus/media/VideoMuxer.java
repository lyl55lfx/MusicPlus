package com.musicplus.media;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import com.musicplus.app.MainApplication;
import com.musicplus.utils.DLog;

/**
 * 视频混合音频
 * @author Darcy
 */
public abstract class VideoMuxer {
	
	private static final String TAG = "VideoMuxer";
	
	String mOutputVideo;
	
	private VideoMuxer(String outputVideo){
		this.mOutputVideo = outputVideo;
	}
	
	public final static  VideoMuxer createVideoMuxer(String outputVideo){
		return new Mp4Muxer(outputVideo);
	}
	
	public abstract void mixRawAudio(File videoFile,File rawAudioFile);
	
	/**
	 * use android sdk MediaMuxer
	 * @author Darcy
	 * @version API >= 18
	 */
	private static class Mp4Muxer extends VideoMuxer{

		private static final String AUDIO_MIME = "audio/mp4a-latm";
		private final static long audioBytesPerSample = 44100*16/8;

		private long rawAudioSize;
		
		public Mp4Muxer(String outputVideo) {
			super(outputVideo);
		}

		@Override
		public void mixRawAudio(File videoFile, File rawAudioFile) {
			final String videoFilePath = videoFile.getAbsolutePath();

			MediaMuxer videoMuxer = null;
			try {
				
				
				final String outputVideo = mOutputVideo;
				videoMuxer = new MediaMuxer(outputVideo,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
				
				MediaFormat videoFormat = null;
				MediaExtractor videoExtractor = new MediaExtractor();
				videoExtractor.setDataSource(videoFilePath);
				
				for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
					MediaFormat format = videoExtractor.getTrackFormat(i);
					String mime = format.getString(MediaFormat.KEY_MIME);
					if (mime.startsWith("video/")) {
						videoExtractor.selectTrack(i);
						videoFormat = format;
						break;
					}
				}
				
				int videoTrackIndex = videoMuxer.addTrack(videoFormat);
				
				int audioTrackIndex = 0;
				
				//decode 
				AndroidAudioDecoder audioDecoder = new AndroidAudioDecoder(videoFilePath);
				String rawVidoeAudioFile = MainApplication.RECORD_AUDIO_PATH + "/" + System.currentTimeMillis();
				audioDecoder.decodeToFile(rawVidoeAudioFile);
				
				final FileInputStream fisAudio1 = new FileInputStream(rawVidoeAudioFile);
				final FileInputStream fisAudio2 = new FileInputStream(rawAudioFile);
				
				boolean readAudio1EOS = false;
				boolean readAudio2EOS = false;
				byte[] audio1Buffer = new byte[4096];
				byte[] audio2Buffer = new byte[4096];
				int audio1ReadCount = 0;
				int audio2ReadCount = 0;
				
				final MultiAudioMixer audioMixer = MultiAudioMixer.createAudioMixer();
				final byte[][] twoAudioBytes = new byte[2][];
				
				final MediaCodec audioEncoder = createACCAudioDecoder();
				audioEncoder.start();
				
				ByteBuffer[] audioInputBuffers = audioEncoder.getInputBuffers();
				ByteBuffer[] audioOutputBuffers = audioEncoder.getOutputBuffers();
				boolean sawInputEOS = false;
		        boolean sawOutputEOS = false;
		        long audioTimeUs = 0 ;
				BufferInfo outBufferInfo = new BufferInfo();
		        
				int inputBufIndex, outputBufIndex;
		        while(!sawOutputEOS){
		        	if (!sawInputEOS) {
		        		 inputBufIndex = audioEncoder.dequeueInputBuffer(10000);
					     if (inputBufIndex >= 0) {
					           ByteBuffer inputBuffer = audioInputBuffers[inputBufIndex];
					           inputBuffer.clear();
					           
					           int bufferSize = inputBuffer.remaining();
					           if(bufferSize != audio1Buffer.length){
					        	   audio1Buffer = new byte[bufferSize];
						           audio2Buffer = new byte[bufferSize];
					           }
					           
					           if(!readAudio1EOS){
					        	   audio1ReadCount = fisAudio1.read(audio1Buffer);
					        	   if(audio1ReadCount == -1){
					        		   readAudio1EOS = true;
					        	   }
					           }
					           
					           if(!readAudio2EOS){
					        	   audio2ReadCount = fisAudio2.read(audio2Buffer);
					        	   if(audio2ReadCount == -1){
					        		   readAudio2EOS = true;
					        	   }
					           }
					           
					           if(readAudio1EOS && readAudio2EOS){
									audioEncoder.queueInputBuffer(inputBufIndex,0 , 0 , 0 ,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
									sawInputEOS = true;
					           }else{
						           
						           byte[] mixAudioBytes;
						           if(!readAudio1EOS && !readAudio2EOS){
						        	   if(audio1ReadCount  == audio2ReadCount){
						        		   twoAudioBytes[0] = audio1Buffer;
								           twoAudioBytes[1] = audio2Buffer;
						        	   }else if(audio1ReadCount > audio2ReadCount){
						        		   twoAudioBytes[0] = audio1Buffer;
						        		   Arrays.fill(audio2Buffer, audio2ReadCount -1, bufferSize, (byte)0);
						        	   }else{
						        		   Arrays.fill(audio1Buffer, audio1ReadCount -1, bufferSize, (byte)0);
						        	   }
						        	   mixAudioBytes = audioMixer.mixRawAudioBytes(twoAudioBytes);
						        	   if(mixAudioBytes == null){
						        		   DLog.e(TAG, "mix audio : null");
						        	   }
						        	   inputBuffer.put(mixAudioBytes);
							           rawAudioSize += mixAudioBytes.length;
							           audioEncoder.queueInputBuffer(inputBufIndex, 0, mixAudioBytes.length, audioTimeUs, 0);
						           }else if(!readAudio1EOS && readAudio2EOS){
						        	   inputBuffer.put(audio1Buffer, 0, audio1ReadCount);
							           rawAudioSize += audio1ReadCount;
							           audioEncoder.queueInputBuffer(inputBufIndex, 0, audio1ReadCount, audioTimeUs, 0);
						           }else{
						        	   inputBuffer.put(audio2Buffer, 0, audio2ReadCount);
							           rawAudioSize += audio2ReadCount;
							           audioEncoder.queueInputBuffer(inputBufIndex, 0, audio2ReadCount, audioTimeUs, 0);
						           }
						           
						           audioTimeUs = (long) (1000000 * (rawAudioSize / 2.0) / audioBytesPerSample);
					           }
					     }
		        	}
		        	
		        	outputBufIndex = audioEncoder.dequeueOutputBuffer(outBufferInfo, 10000);
		        	if(outputBufIndex >= 0){
		        		
		        		// Simply ignore codec config buffers.
		        		if ((outBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!= 0) {
		                     DLog.i(TAG, "audio encoder: codec config buffer");
		                     audioEncoder.releaseOutputBuffer(outputBufIndex, false);
		                     continue;
		                 }
		        		 
		        		if(outBufferInfo.size != 0){
			        		 ByteBuffer outBuffer = audioOutputBuffers[outputBufIndex];
			        		 outBuffer.position(outBufferInfo.offset);
			        		 outBuffer.limit(outBufferInfo.offset + outBufferInfo.size);
			        		 DLog.i(TAG, String.format(" writing audio sample : size=%s , presentationTimeUs=%s", outBufferInfo.size, outBufferInfo.presentationTimeUs));
			        		 videoMuxer.writeSampleData(audioTrackIndex, outBuffer, outBufferInfo);
		        		}
		                
		        		audioEncoder.releaseOutputBuffer(outputBufIndex, false);
		                 
		                 if ((outBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					           sawOutputEOS = true;
					     }
		        	}else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
		        		audioOutputBuffers = audioEncoder.getOutputBuffers();
				    } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				        MediaFormat audioFormat = audioEncoder.getOutputFormat();
				        audioTrackIndex = videoMuxer.addTrack(audioFormat);
				        videoMuxer.start(); //start muxer
				    }
		        }
				
		        fisAudio1.close();
		        fisAudio2.close();
		        audioEncoder.stop();
			    audioEncoder.release();
		        
				//mix video
				boolean videoMuxDone = false;
				// 压缩帧大小 < 原始图片大小
				int videoWidth = videoFormat.getInteger(MediaFormat.KEY_WIDTH);
				int videoHeight = videoFormat.getInteger(MediaFormat.KEY_HEIGHT);
				ByteBuffer videoSampleBuffer = ByteBuffer.allocateDirect(videoWidth * videoHeight); 
				BufferInfo videoBufferInfo = new BufferInfo();
				int sampleSize;
				while (!videoMuxDone) {
					videoSampleBuffer.clear();
					sampleSize = videoExtractor.readSampleData(videoSampleBuffer, 0);
					if (sampleSize < 0) {
						videoMuxDone = true;
					} else {
						videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
						videoBufferInfo.flags = videoExtractor.getSampleFlags();
						videoBufferInfo.size = sampleSize;
						videoSampleBuffer.limit(sampleSize);
						videoMuxer.writeSampleData(videoTrackIndex, videoSampleBuffer,videoBufferInfo);
						videoExtractor.advance();
					}
				}
				
				videoExtractor.release();
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				if(videoMuxer != null){
					videoMuxer.stop();
					videoMuxer.release();
					DLog.i(TAG, "video mix complete.");
				}
			}
		}
		
		private MediaCodec createACCAudioDecoder() throws IOException {
			MediaCodec	codec = MediaCodec.createEncoderByType(AUDIO_MIME);
			MediaFormat format = new MediaFormat();
			format.setString(MediaFormat.KEY_MIME, AUDIO_MIME);
			format.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
			format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
			format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100);
			format.setInteger(MediaFormat.KEY_AAC_PROFILE,
					MediaCodecInfo.CodecProfileLevel.AACObjectLC);
			codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
			return codec;
		}

	}
}
