package com.shuffle.scplayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.shuffle.scplayer.core.AudioListener;
import com.shuffle.scplayer.core.AuthenticationListener;
import com.shuffle.scplayer.core.PlayerListener;
import com.shuffle.scplayer.core.SpotifyConnectPlayer;
import com.shuffle.scplayer.core.SpotifyConnectPlayerImpl;
import com.shuffle.scplayer.core.Track;
import com.shuffle.scplayer.jna.SpotifyLibrary.SpBitrate;
import com.shuffle.scplayer.web.PlayerWebServerIntegration;
import com.shuffle.scplayer.core.SpotifyConnectZeroConf;

public class SCPlayerMain {
	private static final transient Log log = LogFactory.getLog(SCPlayerMain.class);
	public static final Map<Integer, Level> logLevel = new HashMap<Integer, Level>();
	private static final Map<Integer, Integer> mapBitrate = new HashMap<>();
	private static final transient Gson gson = new GsonBuilder().create();
	private static final File credentials = new File("./credentials.json");
	private static String username;
	private static String password;
	private static String blob;
	private static String playerName;
	private static String deviceId = UUID.randomUUID().toString();
	private static boolean rememberMe;
	private static JsonObject credentialsJson; 
	static {
		logLevel.put(0, Level.WARN);
		logLevel.put(1, Level.INFO);
		logLevel.put(2, Level.DEBUG);
		logLevel.put(3, Level.TRACE);
		
		mapBitrate.put(90, SpBitrate.kSpBitrate90k);
		mapBitrate.put(160, SpBitrate.kSpBitrate160k);
		mapBitrate.put(320, SpBitrate.kSpBitrate320k);
	}

	public static void main(String[] args) throws Exception {
		initLogger();
		
		Integer debug = Integer.getInteger("debug", 0);
		Logger.getRootLogger().setLevel(logLevel.get(debug));
		
		verifyCredentialFile(credentials);

		Boolean standalone = Boolean.getBoolean("standalone");
		Integer webPort = Integer.getInteger("web.port", 4000);
		String appKeyLocation = System.getProperty("app.key", "./spotify_appkey.key");
		rememberMe = Boolean.parseBoolean(System.getProperty("remember.me", "true"));
		playerName = System.getProperty("player.name", (playerName != null ? playerName : "SCPlayer"));
		username = System.getProperty("username", (username != null ? username:null));
		password = System.getProperty("password", (password != null ? password:null));
		int bitrate = Integer.getInteger("bitrate", SpBitrate.kSpBitrate320k);
		
		File appKey = new File(appKeyLocation);
		
		if (!appKey.exists()) {
			log.error("appkey not found");
			System.exit(-1);
		}

		if (Boolean.getBoolean("list.mixers")) {
			System.out.println("Mixers avaliables");
			System.out.println("Choose your mixer and set -Dmixer=%index%");
			System.out.println("Index - Name - Description - Version");
			Mixer.Info[] mixers = AudioSystem.getMixerInfo();
			for (int i = 0; i < mixers.length; i++) {
				Mixer.Info mixerInfo = mixers[i];
				System.out.println(i + " - " + mixerInfo.getName() + " - " + mixerInfo.getDescription() + " - "
						+ mixerInfo.getVersion()
						+ (AudioSystem.getMixer(mixerInfo).isLineSupported(AudioListener.DATALINE)
								? " - Support PCM Audio" : ""));
			}
			return;
		}

		final SpotifyConnectPlayer player = new SpotifyConnectPlayerImpl(appKey, deviceId);
		

		player.addAuthenticationListener(new AuthenticationListener() {

			@Override
			public void onNewCredentials(String username, String blob) {
				log.trace("remember : " + rememberMe);
				if (rememberMe) {
					JsonObject credentialsJson = new JsonObject();
					credentialsJson.addProperty("username", username);
					credentialsJson.addProperty("blob", blob);
					credentialsJson.addProperty("playerName", player.getPlayerName());
					credentialsJson.addProperty("deviceId", player.getDeviceId());
					try {
						if (!credentials.exists()) {
							log.debug("Credentials file not exists");
							try {
								credentials.createNewFile();
							} catch (SecurityException | IOException e) {
								log.error("Error trying to write credentials file", e);
								return;
							}
						}
						FileWriter fileWriter = new FileWriter(credentials);
						fileWriter.write(gson.toJson(credentialsJson));
						fileWriter.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void onLoggedOut(String username) {
				
			}

			@Override
			public void onLoggedIn(String username) {

			}
		});
		
		player.addPlayerListener(new PlayerListener() {
			
			@Override
			public void onVolumeChanged(short volume) {
				
			}
			
			@Override
			public void onTrackChanged(Track track) {
				
			}
			
			@Override
			public void onTokenLost() {
				
			}
			
			@Override
			public void onShuffle(boolean enabled) {
				
			}
			
			@Override
			public void onSeek(int millis) {
				
			}
			
			@Override
			public void onRepeat(boolean enabled) {
				
			}
			
			@Override
			public void onPreviousTrack(Track track) {
				
			}
			
			@Override
			public void onPlayerNameChanged(String playerName) {
				log.trace("remember : " + rememberMe);
				if (rememberMe) {
					try {
						if (credentialsJson == null) {
							log.debug("Credentials file not exists");
							return;
						}
						credentialsJson.remove("playerName");
						credentialsJson.addProperty("playerName", playerName);
						FileWriter fileWriter = new FileWriter(credentials);
						fileWriter.write(gson.toJson(credentialsJson));
						fileWriter.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			@Override
			public void onPlay() {
				
			}
			
			@Override
			public void onPause() {
				
			}
			
			@Override
			public void onNextTrack(Track track) {
				
			}
			
			@Override
			public void onInactive() {
				
			}
			
			@Override
			public void onActive() {
				
			}
		});

		if (!standalone) {
			PlayerWebServerIntegration webServerIntegration = new PlayerWebServerIntegration(player, webPort);
			player.addPlayerListener(webServerIntegration);
		}
		
		player.setBitrate(mapBitrate.get(bitrate) != null ? mapBitrate.get(bitrate) : SpBitrate.kSpBitrate320k);

		if (username != null && !"".equalsIgnoreCase(username) && password != null && !"".equalsIgnoreCase(password)) {
			player.login(username, password);

		} else if (username != null && !"".equalsIgnoreCase(username) && blob != null && !"".equalsIgnoreCase(blob)) {
			player.loginBlob(username, blob);
		}

		player.setPlayerName(playerName);

		// starting zeroconf service
		SpotifyConnectZeroConf zeroConf = new SpotifyConnectZeroConf(player);
		zeroConf.runServer();

		String mixerString = System.getProperty("mixer", "0");
		if (mixerString != null && !"".equalsIgnoreCase(mixerString)) {
			Mixer.Info[] mixers = AudioSystem.getMixerInfo();
			Mixer.Info mixer = mixers[0];
			try {
				int mixerInt = new Integer(mixerString);
				mixer = mixerInt < mixers.length ? mixers[mixerInt] : mixers[0];
			} catch (NumberFormatException e) {
				for (int i = 0; i < mixers.length; i++) {
					if (mixers[i].getName().equals(mixerString)) {
						mixer = mixers[i];
					}
				}
			}
			player.setMixer(mixer);
		}

	}

	private static void verifyCredentialFile(File credentials) throws IOException, IllegalArgumentException {
		if (!credentials.exists()) {
			log.debug("Credentials file not exists");
			return;
		}
		try {
			log.debug("Reading credentials JSON file");
			JsonElement credentialsContent = new JsonParser().parse(new FileReader(credentials));
			if (credentialsContent != null && credentialsContent.isJsonObject()) {
				log.debug("Valid JSON");
				credentialsJson = credentialsContent.getAsJsonObject();
				if (credentialsJson != null) {
					log.debug("Reading data");
					username = credentialsJson.get("username").getAsString();
					log.trace("Username : " + username);
					blob = credentialsJson.get("blob").getAsString();
					log.trace("Blob : " + blob);
					playerName = credentialsJson.get("playerName").getAsString();
					log.trace("PlayerName : " + playerName);
					deviceId = credentialsJson.get("deviceId").getAsString();
					log.trace("deviceId : " + deviceId);
				}
			}
		} catch (FileNotFoundException e) {
			log.error("Credentials file not found", e);
		} catch (JsonParseException e) {
			log.error("Invalid credentials file", e);
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
