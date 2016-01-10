package com.shuffle.scplayer.core.zeroconf;

/* Not supported zeroconf vars and login provider using original spotify library*/
public class SpotifyZeroConfProviderLib implements SpotifyZeroConfProvider {
    @Override
    public SpotifyZeroConfVars getVars() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loginZeroConf(String username, String blob, String clientKey) {
        throw new UnsupportedOperationException();
    }
}
