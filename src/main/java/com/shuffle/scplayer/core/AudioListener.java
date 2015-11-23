package com.shuffle.scplayer.core;

import javax.sound.sampled.AudioFormat;

/**
 * @author crsmoro
 * @author LeanderK
 * @author Tokazio
 * @version 1.1
 */
public interface AudioListener {
    int RATE = 44100;
    int CHANNELS = 2;
    int SAMPLESIZE = 16/8;//16bit
    
    AudioFormat PCM = new AudioFormat(RATE, 16, CHANNELS, true, false);

    void onActive();

    void onInactive();

    void onPlay();

    void onPause();

    void onTokenLost();

    void onAudioFlush();

    int onAudioData(byte[] data, int numSamples);

    void onVolumeChanged(int volume);

    void close();
    
    void open(int aMixerId);
}
