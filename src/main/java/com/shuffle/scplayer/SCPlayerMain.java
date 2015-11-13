package com.shuffle.scplayer;

import com.shuffle.scplayer.core.SpotifyConnectPlayer;
import com.shuffle.scplayer.core.SpotifyConnectPlayerImpl;
import com.shuffle.scplayer.web.PlayerWebServerIntegration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.*;

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
		initLogger();

		String playerName = System.getProperty("playerName", "SCPlayer");
		String mixer = System.getProperty("mixer");
		String username = System.getProperty("username");
		String password = System.getProperty("password");
		Boolean standalone = Boolean.getBoolean("standalone");
		Integer debug = Integer.getInteger("debug", 0);
		String appKeyLocation = System.getProperty("appKey", "spotify_appkey.key");
		File appKey = new File(appKeyLocation);
		Logger.getRootLogger().setLevel(logLevel.get(debug));

		if (!appKey.exists()) {
			log.error("appkey is not existing");
			System.exit(-1);
		}

		SpotifyConnectPlayer player = new SpotifyConnectPlayerImpl(appKey);

		if (playerName != null && !"".equalsIgnoreCase(playerName)) {
			player.setPlayerName(playerName);
		}
		if (mixer != null && !"".equalsIgnoreCase(mixer)) {
			player.setMixer(mixer);
		}
		if (username != null && !"".equalsIgnoreCase(username) && password != null && !"".equalsIgnoreCase(password)) {
			player.login(username, password);
		} else if (standalone) {
			log.error("unable to create Spotify-Connect");
			System.exit(-1);
		}
		if (!standalone) {
			PlayerWebServerIntegration webServerIntegration = new PlayerWebServerIntegration(player);
			player.addPlayerListener(webServerIntegration);
		}

	}

	private static void initLogger() {
		// This is the root logger provided by log4j
		Logger rootLogger = Logger.getRootLogger();

		// Define log pattern layout
		PatternLayout layout = new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n");

		try {
			// Define file appender with layout and output log file name
			RollingFileAppender fileAppender = new RollingFileAppender(layout, "./scplayer.log");
			fileAppender.setImmediateFlush(true);
			fileAppender.setThreshold(Level.DEBUG);
			fileAppender.setAppend(true);
			fileAppender.setMaxFileSize("5MB");
			fileAppender.setMaxBackupIndex(2);

			// Add the appender to root logger
			rootLogger.addAppender(fileAppender);
		} catch (IOException e) {
			System.out.println("Failed to add appender !!");
			System.exit(-1);
		}
	}

}
