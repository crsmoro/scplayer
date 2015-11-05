package com.shuffle.scplayer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.shuffle.scplayer.core.Player;
import com.shuffle.scplayer.player.SpotifyConnectStandalonePlayer;
import com.shuffle.scplayer.player.SpotifyConnectWebPlayer;

public class SCPlayer {
	
	public static final Map<Integer, Level> logLevel = new HashMap<Integer, Level>();
	
	static {
		logLevel.put(0, Level.WARN);
		logLevel.put(1, Level.INFO);
		logLevel.put(2, Level.DEBUG);
		logLevel.put(3, Level.TRACE);
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		
		String playerName = System.getProperty("playerName");
		String username = System.getProperty("username");
		String password = System.getProperty("password");
		Boolean standalone = Boolean.getBoolean("standalone");
		Integer debug = Integer.getInteger("debug", 0);
		String appKey = System.getProperty("appKey", "./spotify_appkey.key");
		
		Logger.getRootLogger().setLevel(logLevel.get(debug));

		Player player = null;
		if (standalone) {
			player = new SpotifyConnectStandalonePlayer(new File(appKey));
		}
		else {
			player = new SpotifyConnectWebPlayer(new File(appKey));
		}
		
		if (playerName != null && !"".equalsIgnoreCase(playerName)) {
			player.setPlayerName(playerName);
		}
		if (username != null && !"".equalsIgnoreCase(username) && password != null && !"".equalsIgnoreCase(password))
		{
			player.login(username, password);
		}
	}

}
