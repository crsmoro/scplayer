package com.shuffle.scplayer;

import com.shuffle.scplayer.core.SpotifyConnectPlayer;
import com.shuffle.scplayer.core.SpotifyConnectPlayerImpl;
import com.shuffle.scplayer.web.PlayerWebServerIntegration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SCPlayerMain {
    private static final transient Log log = LogFactory.getLog(SCPlayerMain.class);
	public static final Map<Integer, Level> logLevel = new HashMap<Integer, Level>();
	
	static {
		logLevel.put(0, Level.WARN);
		logLevel.put(1, Level.INFO);
		logLevel.put(2, Level.DEBUG);
		logLevel.put(3, Level.TRACE);
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		
		String playerName = System.getProperty("playerName", "SCPlayer");
		String username = System.getProperty("username");
		String password = System.getProperty("password");
		Boolean standalone = Boolean.getBoolean("standalone");
		Integer debug = Integer.getInteger("debug", 0);
		String appKeyLocation = System.getProperty("appKey", "spotify_appkey.key");
        File appKey = new File(appKeyLocation);
        if (!appKey.exists()) {
            log.error("appkey is not existing");
            System.exit(-1);
        }
		
		Logger.getRootLogger().setLevel(logLevel.get(debug));
        File credentials = new File("/storage/credentials.json");

        SpotifyConnectPlayer player = null;

		if (playerName != null && !"".equalsIgnoreCase(playerName)) {
            player = new SpotifyConnectPlayerImpl(appKey, playerName, credentials);
		}
		if (username != null && !"".equalsIgnoreCase(username) && password != null && !"".equalsIgnoreCase(password))
		{
            player = new SpotifyConnectPlayerImpl(appKey, playerName, username, password, credentials);
		} else {
            log.error("unable to create Spotify-Connect");
            System.exit(-1);
        }
		if (!standalone) {
            PlayerWebServerIntegration webServerIntegration = new PlayerWebServerIntegration(player);
            player.addPlayerListener(webServerIntegration);
		}
		

	}

}
