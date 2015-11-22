package com.shuffle.scplayer.core;

import javax.sound.sampled.Mixer;

/**
 * @author crsmoro
 * @author LeanderK
 * @version 1.0
 */
public interface SpotifyConnectPlayer {
    Track getPlayingTrack();

    boolean getShuffle();

    boolean getRepeat();

    short getVolume();

    int getSeek();

    String getAlbumCoverURL();

    boolean isPlaying();

    String getPlayerName();

    void setPlayerName(String playerName);

    void play();

    void pause();

    void prev();

    void next();

    void seek(int millis);

    void shuffle(boolean enabled);

    void repeat(boolean enabled);

    void volume(short volume);

    void login(String username, String password);
    
    void loginBlob(String username, String blob);

    void logout();

    boolean isLoggedIn();

    boolean isActive();

    void close();

    void addPlayerListener(PlayerListener playerListener);

    void removePlayerListener(PlayerListener playerListener);

    AudioListener getAudioListener();

    void setAudioListener(AudioListener audioListener);
    
    Mixer.Info getMixer();
    
    void setMixer(Mixer.Info mixer);
    
    void addAuthenticationListener(AuthenticationListener authenticationListener);

    void removeAuthenticationListener(AuthenticationListener authenticationListener);
    
    String getDeviceId();
    
    void setBitrate(int bitrate);
    
    int getBitrate();

    void setMixerId(int mixerId);

    int getMixerId();
}
