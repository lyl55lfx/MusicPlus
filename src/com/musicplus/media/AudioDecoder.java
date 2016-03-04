package com.musicplus.media;

import java.io.IOException;

/**
 * 音频解码器
 * @author Darcy
 *
 */
public abstract class AudioDecoder {
	 
	 String mEncodeFile;
	 
	 AudioDecoder(String encodefile){
		 this.mEncodeFile = encodefile;
	 }
	 
	 public static AudioDecoder createDefualtDecoder(String encodefile){
		 return new AndroidAudioDecoder(encodefile);
	 } 
	 
	 /**
	  * 解码
	  * @return
	  * @throws IOException
	  */
	 public abstract RawAudioInfo decodeToFile(String outFile) throws IOException;
	 
	 public static class RawAudioInfo{
		 public String tempRawFile;
		 public int size;
		 public long sampleRate;
		 public int channel;
	 }
}
