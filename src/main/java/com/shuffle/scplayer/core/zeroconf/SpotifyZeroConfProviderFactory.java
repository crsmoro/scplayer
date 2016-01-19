package com.shuffle.scplayer.core.zeroconf;

import com.shuffle.scplayer.core.SpotifyConnectPlayer;

public class SpotifyZeroConfProviderFactory {
    public SpotifyZeroConfProvider create(String implementation, SpotifyConnectPlayer player) {
        if (implementation.toLowerCase().equals("library")) {
            return new SpotifyZeroConfProviderLib(player);
        } else if (implementation.toLowerCase().equals("opensource")) {
            return new SpotifyZeroConfProviderOS(player);
        }
        throw new IllegalArgumentException();
    }
}
