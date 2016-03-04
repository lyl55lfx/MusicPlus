package com.musicplus.media;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.musicplus.utils.DLog;

/**
 * Android SDK 支持的编码器
 * @author Darcy
 * @version  android.os.Build.VERSION.SDK_INT >= 16
 */
public class AndroidAudioDecoder extends AudioDecoder{
	
	private static final String TAG = "AndroidAudioDecoder";
	
	AndroidAudioDecoder(String encodefile) {
		super(encodefile);
	}

	@Override
	public RawAudioInfo decodeToFile(String outFile) throws IOException{
		
		long beginTime = System.currentTimeMillis();
		
		final String encodeFile = mEncodeFile;
		MediaExtractor extractor = new MediaExtractor();
		extractor.setDataSource(encodeFile);
		MediaFormat format = extractor.getTrackFormat(0);
        String mime = format.getString(MediaFormat.KEY_MIME);
        if(!mime.startsWith("audio/")){
        	throw new IOException("not an valid audio file");
        }
        
        RawAudioInfo rawAudioInfo = new RawAudioInfo();
        rawAudioInfo.channel = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        rawAudioInfo.sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        rawAudioInfo.tempRawFile = outFile;
        FileOutputStream fosDecoder = new FileOutputStream(rawAudioInfo.tempRawFile);
        
        MediaCodec codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, null, null, 0);
        codec.start();
        
        ByteBuffer[] codecInputBuffers = codec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();
        
        extractor.selectTrack(0);
        
        final long kTimeOutUs = 5000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int totalRawSize = 0;
        try{
			while (!sawOutputEOS) {
			    if (!sawInputEOS) {
			        int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
			        if (inputBufIndex >= 0) {
			            ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
			            int sampleSize = extractor.readSampleData(dstBuf, 0);
			            long presentationTimeUs = 0;
			            if (sampleSize < 0) {
			                DLog.i(TAG, "saw input EOS.");
			                sawInputEOS = true;
			                codec.queueInputBuffer(inputBufIndex,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM );
			            } else {
			                presentationTimeUs = extractor.getSampleTime();
			                codec.queueInputBuffer(inputBufIndex,0,sampleSize,presentationTimeUs,0);
			                extractor.advance();
			            }
			        }
			    }
			    int res = codec.dequeueOutputBuffer(info, kTimeOutUs);
			    if (res >= 0) {

			        int outputBufIndex = res;
			        ByteBuffer outBuf = codecOutputBuffers[outputBufIndex];
			        
			        outBuf.position(info.offset);
			        outBuf.limit(info.offset + info.size);
			        byte[] data = new byte[outBuf.remaining()];
			        outBuf.get(data);
			        totalRawSize += data.length;
			        fosDecoder.write(data);
			        
			        codec.releaseOutputBuffer(outputBufIndex, false);
			        
			        DLog.i(TAG, mEncodeFile + "presentationTimeUs : " + info.presentationTimeUs);
			        
			        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
			            DLog.i(TAG, "saw output EOS.");
			            sawOutputEOS = true;
			        }
			        
			    } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
			        codecOutputBuffers = codec.getOutputBuffers();
			        DLog.i(TAG, "output buffers have changed.");
			    } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
			        MediaFormat oformat = codec.getOutputFormat();
			        DLog.i(TAG, "output format has changed to " + oformat);
			    }
			}
			rawAudioInfo.size = totalRawSize;
			
            DLog.i(TAG, "decode "+outFile+" cost " + (System.currentTimeMillis() - beginTime) +" milliseconds !");

			return rawAudioInfo;
        }finally{
        	fosDecoder.close();
        	codec.stop();
            codec.release();
            extractor.release();
        }
        
	}
}
