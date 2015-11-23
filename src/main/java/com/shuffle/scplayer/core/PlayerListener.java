package com.shuffle.scplayer.core;

/**
 * @author LeanderK
 * @version 1.0
 */
public interface PlayerListener {
    void onPlay();

    void onPause();

    void onSeek(int millis);

    void onTrackChanged(Track track);

    void onNextTrack(Track track);

    void onPreviousTrack(Track track);

    void onShuffle(boolean enabled);

    void onRepeat(boolean enabled);

    void onActive();

    void onInactive();

    void onTokenLost();

    void onVolumeChanged(int volume);
}
