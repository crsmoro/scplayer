package com.shuffle.scplayer.core.zeroconf;

import com.shuffle.scplayer.core.SpotifyConnectPlayer;

/* Not supported zeroconf vars and login provider using original spotify library*/
public class SpotifyZeroConfProviderLib implements SpotifyZeroConfProvider {

    SpotifyConnectPlayer player;

    public SpotifyZeroConfProviderLib(SpotifyConnectPlayer player) {
        this.player = player;
    }
    @Override
    public SpotifyZeroConfVars getVars() {
        return player.getZeroConfVars();
    }

    @Override
    public void loginZeroConf(String username, String blob, String clientKey) {
        player.loginZeroconf(username, blob, clientKey);
    }
}
