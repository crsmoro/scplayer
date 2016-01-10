package com.shuffle.scplayer.core.zeroconf;

public interface ZeroConfService {
    void publishService(String name, String type, int port, String text);
}
