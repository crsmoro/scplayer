package com.shuffle.scplayer.core.zeroconf;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by pwitkows on 1/10/2016.
 */
public class ZeroConfServiceJmdns implements ZeroConfService {
    @Override
    public void publishService(String name, String type, int port, HashMap<String, String> values) {

        try {
            JmDNS jmDNS = JmDNS.create();
            jmDNS.registerService(ServiceInfo.create(type, name, port, 0, 0, values));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
