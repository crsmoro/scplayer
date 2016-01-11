package com.shuffle.scplayer.core.zeroconf;

import java.util.HashMap;

public interface ZeroConfService {
    void publishService(String name, String type, int port, HashMap<String, String> values);
}
