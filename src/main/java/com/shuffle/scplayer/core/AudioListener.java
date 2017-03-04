package com.shuffle.scplayer.core;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 * @author crsmoro
 * @author LeanderK
 * @version 1.0
 */
public interface AudioListener {
    int RATE = 44100;
    int CHANNELS = 2;
    int SAMPLESIZE = 2;
    
    AudioFormat PCM = new AudioFormat(RATE, 16, CHANNELS, true, false);
    
    DataLine.Info DATALINE = new DataLine.Info(SourceDataLine.class, PCM);

    void onActive();

    void onInactive();

    void onPlay();

    void onPause();

    void onTokenLost();

    void onAudioFlush();

    int onAudioData(byte[] data);

    void onVolumeChanged(int volume);

    void close();
}
