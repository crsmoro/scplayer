package com.shuffle.scplayer.core.zeroconf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

/* Zeroconf service implementation based on avahi-publish util */
public class ZeroConfServiceAvahiBin implements ZeroConfService{
    private static final transient Log log = LogFactory.getLog(ZeroConfServiceAvahiBin.class);

    public void publishService(String name, String type, int port, String text) {
        Thread avahi = new Thread(new Runnable() {
            @Override
            public void run() {
                // use avahi-publish as workaround - not portable, needs avahi-utils
                try {
                    Runtime.getRuntime().exec("avahi-publish -s "+name+" "+type+" " + String.valueOf(port)+" \"" +text+"\"");
                }catch(Exception e) {
                    log.warn("Avahibin error. Zero conf authentication will not be available", e);
                }
            }
        });
        avahi.setDaemon(true);
        avahi.start();
    }

    @Override
    public void publishService(String name, String type, int port, HashMap<String, String> values) {
        String txt = "";
        for(Map.Entry<String, String> value : values.entrySet()) {
            txt+=value.getKey()+"="+value.getValue()+" ";
        }
        publishService(name, type, port, txt.substring(0, txt.length()-1));
    }
}
