package com.shuffle.scplayer.core.zeroconf;

public interface SpotifyZeroConfProvider {
    SpotifyZeroConfVars getVars();
    void loginZeroConf(String username, String blob, String clientKey) throws Exception;
}
