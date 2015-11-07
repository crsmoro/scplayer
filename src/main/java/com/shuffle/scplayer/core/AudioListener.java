package com.shuffle.scplayer.core;

/**
 * @author crsmoro
 * @author LeanderK
 * @version 1.0
 */
public interface AudioListener {
    int RATE = 44100;
    int CHANNELS = 2;
    int SAMPLESIZE = 2;

    void onActive();

    void onInactive();

    void onTokenLost();

    void onAudioFlush();

    void onAudioData(byte[] data);

    void onVolumeChanged(short volume);

    void close();
}
