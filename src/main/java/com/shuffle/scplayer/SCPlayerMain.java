package com.shuffle.scplayer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import com.shuffle.scplayer.core.AudioListener;
import com.shuffle.scplayer.core.SpotifyConnectPlayer;
import com.shuffle.scplayer.core.SpotifyConnectPlayerImpl;
import com.shuffle.scplayer.web.PlayerWebServerIntegration;

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

		Boolean standalone = Boolean.getBoolean("standalone");
		Integer debug = Integer.getInteger("debug", 0);
		Integer webPort = Integer.getInteger("web.port", 4000);
		String appKeyLocation = System.getProperty("app.key", "spotify_appkey.key");
		File appKey = new File(appKeyLocation);
		Logger.getRootLogger().setLevel(logLevel.get(debug));

		if (!appKey.exists()) {
			log.error("appkey not found");
			System.exit(-1);
		}
		
		if (Boolean.getBoolean("list.mixers")) {
        	System.out.println("Mixers avaliables");
        	System.out.println("Choose your mixer and set -Dmixer=%index%");
        	System.out.println("Index - Name - Description - Version");
        	Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        	for (int i = 0; i< mixers.length; i++) {
        		Mixer.Info mixerInfo = mixers[i];
        		System.out.println(i + " - " + mixerInfo.getName() + " - " + mixerInfo.getDescription() + " - " + mixerInfo.getVersion() + (AudioSystem.getMixer(mixerInfo).isLineSupported(AudioListener.DATALINE)?" - Support PCM Audio":""));
        	}
        	return;
        }

		SpotifyConnectPlayer player = new SpotifyConnectPlayerImpl(appKey);

		if (!standalone) {
			PlayerWebServerIntegration webServerIntegration = new PlayerWebServerIntegration(player, webPort);
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
