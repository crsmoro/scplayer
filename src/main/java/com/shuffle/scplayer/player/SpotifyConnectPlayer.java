package com.shuffle.scplayer.player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.UUID;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.shuffle.scplayer.core.Player;
import com.shuffle.scplayer.core.Track;
import com.shuffle.scplayer.jna.SpConfig;
import com.shuffle.scplayer.jna.SpConnectionCallbacks;
import com.shuffle.scplayer.jna.SpDebugCallbacks;
import com.shuffle.scplayer.jna.SpMetadata;
import com.shuffle.scplayer.jna.SpPlaybackCallbacks;
import com.shuffle.scplayer.jna.SpSampleFormat;
import com.shuffle.scplayer.jna.SpotifyLibrary;
import com.shuffle.scplayer.jna.SpotifyLibrary.SpBitrate;
import com.shuffle.scplayer.jna.SpotifyLibrary.SpConnectionNotify;
import com.shuffle.scplayer.jna.SpotifyLibrary.SpDeviceType;
import com.shuffle.scplayer.jna.SpotifyLibrary.SpError;
import com.shuffle.scplayer.jna.SpotifyLibrary.SpImageSize;
import com.shuffle.scplayer.jna.SpotifyLibrary.SpPlaybackNotify;
import com.shuffle.scplayer.utils.NativeUtils;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public abstract class SpotifyConnectPlayer extends Player {

	private static final transient Log log = LogFactory.getLog(SpotifyConnectPlayer.class);
	private static SpConfig spConfig;
	private static int RATE = 44100;
	private static int CHANNELS = 2;
	private static int SAMPLESIZE = 2;
	private static PipedInputStream input;
	private static PipedOutputStream output;
	private static SourceDataLine audioLine;
	private static SpPlaybackCallbacks spPlaybackCallbacks;
	private static SpDebugCallbacks spDebugCallbacks;
	private static SpConnectionCallbacks spConnectionCallbacks;
	private AudioFormat pcm = new AudioFormat(RATE, 16, CHANNELS, true, false);
	private DataLine.Info info = new DataLine.Info(SourceDataLine.class, pcm);
	private final transient Gson gson = new GsonBuilder().create();
	private String username;
	private String deviceId = UUID.randomUUID().toString();

	private final SpotifyLibrary spotifyLib = SpotifyLibrary.INSTANCE;

	private static SpotifyConnectPlayer instance;
	
	public SpotifyConnectPlayer() {
		this(new File("./spotify_appkey.key"));
	}

	public SpotifyConnectPlayer(File appKey) {
		try {
			if (instance != null) {
				log.warn("Already Initialized");
				return;
			}

			log.info("init");
			if (AudioSystem.getMixerInfo().length <= 0) {
				log.error("No sound cards Avaliables");
				throw new Exception("No sound cards Avaliables");
			}
			
			String playerName = "SCPlayer";
			String blob = null;
			
			final File credentials = new File("/storage/credentials.json");
			if (!credentials.exists()) {
				log.debug("Credentials file not exists, creating new");
				try
				{
					credentials.createNewFile();
				}
				catch (SecurityException e)
				{
					log.error("Error trying to write credentials file", e);
				}
			}
			try {
				log.debug("Reading credentials JSON file");
				JsonElement credentialsContent = new JsonParser().parse(new FileReader(credentials));
				if (credentialsContent != null && credentialsContent.isJsonObject()) {
					log.debug("Valid JSON");
					JsonObject credentialsJson = credentialsContent.getAsJsonObject();
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
			}
			catch (FileNotFoundException e)
			{
				log.error("Credentials file not found", e);
			}
			catch (JsonParseException e)
			{
				log.error("Invalid credentials file", e);
			}

			byte[] appKeyByte = IOUtils.toByteArray(new FileInputStream(appKey));

			Pointer pointerVazio = new Memory(1048576L);
			Pointer pointerAppKey = NativeUtils.pointerFrom(appKeyByte);

			spConfig = new SpConfig.ByReference();
			spConfig.version = 4;

			spConfig.buffer = pointerVazio;
			spConfig.buffer_size = new NativeLong(1048576, true);
			spConfig.app_key = pointerAppKey;
			spConfig.app_key_size = new NativeLong(appKeyByte.length, true);
			spConfig.deviceId = NativeUtils.pointerFrom(deviceId);
			spConfig.remoteName = NativeUtils.pointerFrom(playerName);
			spConfig.brandName = NativeUtils.pointerFrom("DummyBrand");
			spConfig.modelName = NativeUtils.pointerFrom("DummyModel");
			spConfig.deviceType = SpDeviceType.kSpDeviceTypeAudioDongle;

			int retorno = spotifyLib.SpInit(spConfig);
			if (retorno != SpError.kSpErrorOk) {
				log.error("spInit : " + retorno);
				System.exit(1);
			}

			spConnectionCallbacks = new SpConnectionCallbacks.ByReference();
			spConnectionCallbacks.notify$ = new SpPlaybackCallbacks.notify_callback() {
				public void apply(int notification, Pointer userdata) {
					if (notification == SpConnectionNotify.kSpConnectionNotifyLoggedIn) {
						onLoggedIn();
					}
					else if (notification == SpConnectionNotify.kSpConnectionNotifyLoggedOut) {
						onLoggedOut();
					}
				}
			};
			spConnectionCallbacks.new_credentials = new SpConnectionCallbacks.new_credentials_callback() {
				public void apply(Pointer blob, Pointer userdata) {
					log.info("new_credentials_callback");
					log.debug("blob : " + blob.getString(0));
					JsonObject credentialsJson = new JsonObject();
					credentialsJson.addProperty("username", username);
					credentialsJson.addProperty("blob", blob.getString(0));
					credentialsJson.addProperty("playerName", getPlayerName());
					credentialsJson.addProperty("deviceId", deviceId);
					try {
						FileWriter fileWriter = new FileWriter(credentials);
						fileWriter.write(gson.toJson(credentialsJson));
						fileWriter.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
				}
			};
			spotifyLib.SpRegisterConnectionCallbacks(spConnectionCallbacks, null);

			spPlaybackCallbacks = new SpPlaybackCallbacks.ByReference();
			spPlaybackCallbacks.notify$ = new SpPlaybackCallbacks.notify_callback() {
				public void apply(int notification, Pointer userdata) {
					log.info("playback_notify_callback");
					log.info("notification : " + notification);
					if (notification == SpPlaybackNotify.kSpPlaybackNotifyPlay) {
						onPlay();
					} else if (notification == SpPlaybackNotify.kSpPlaybackNotifyPause) {
						onPause();
					} else if (notification == SpPlaybackNotify.kSpPlaybackNotifyTrackChanged) {
						onTrackChanged(getPlayingTrack());
					} else if (notification == SpPlaybackNotify.kSpPlaybackNotifyNext) {
						onNextTrack(getPlayingTrack());
					} else if (notification == SpPlaybackNotify.kSpPlaybackNotifyPrev) {
						onPreviousTrack(getPlayingTrack());
					} else if (notification == SpPlaybackNotify.kSpPlaybackNotifyShuffleEnabled) {
						onShuffle(true);
					} else if (notification == SpPlaybackNotify.kSpPlaybackNotifyShuffleDisabled) {
						onShuffle(false);
					} else if (notification == SpPlaybackNotify.kSpPlaybackNotifyRepeatEnabled) {
						onRepeat(true);
					} else if (notification == SpPlaybackNotify.kSpPlaybackNotifyRepeatDisabled) {
						onRepeat(false);
					} else if (notification == SpPlaybackNotify.kSpPlaybackNotifyBecameActive) {
						onActive();
					} else if (notification == SpPlaybackNotify.kSpPlaybackNotifyBecameInactive) {
						onInactive();
					} else if (notification == SpPlaybackNotify.kSpPlaybackNotifyPlayTokenLost) {
						onTokenLost();
					} else if (notification == SpPlaybackNotify.kSpPlaybackEventAudioFlush) {
						onAudioFlush();
					}
				}
			};
			spPlaybackCallbacks.audio_data = new SpPlaybackCallbacks.audio_data_callback() {
				public int apply(Pointer samples, int num_samples, SpSampleFormat format, IntByReference pending,
						Pointer userdata) {
					try {
						if (num_samples == 0) {
							return 0;
						}
						num_samples -= (num_samples % CHANNELS);
						onAudioData(samples.getByteArray(0, num_samples * SAMPLESIZE));
						return num_samples * SAMPLESIZE;
					} catch (Exception e) {
						log.error("spotifyLib audio data error", e);
						return 0;
					}
				}
			};
			spPlaybackCallbacks.seek = new SpPlaybackCallbacks.seek_callback() {
				public void apply(int millis, Pointer userdata) {
					onSeek(millis);
				}
			};
			spPlaybackCallbacks.apply_volume = new SpPlaybackCallbacks.apply_volume_callback() {
				public void apply(short volume, Pointer userdata) {
					log.info("apply_volume_callback");
					log.debug("volume: " + volume);
					if (audioLine == null)
					{
						log.warn("audioLine not ready");
						return;
					}
					FloatControl volumeControl = (FloatControl) audioLine.getControl(FloatControl.Type.MASTER_GAIN);
					float maxDb = volumeControl.getMaximum();
					log.debug("maxDb : " + maxDb);
					float minDb = volumeControl.getMinimum();
					log.debug("minDb : " + minDb);
					float newVolume = 0;

					float volumePercent = (float) (volume / 655.35);
					if (volumePercent < 0) {
						volumePercent = 100 + volumePercent;
					}
					log.debug("volume percent : " + volumePercent);
					newVolume = (volumePercent * minDb / 100);
					newVolume = (newVolume - minDb) * -1;

					if (volume == 0) {
						log.debug("volume 0, setting max");
						newVolume = -0.01f;
					}
					
					log.debug("newVolume : " + newVolume);
					volumeControl.setValue(newVolume);
					

					onVolumeChanged((short) newVolume);
				}
			};
			spotifyLib.SpRegisterPlaybackCallbacks(spPlaybackCallbacks, null);

			spDebugCallbacks = new SpDebugCallbacks.ByReference();
			spDebugCallbacks.message = new SpDebugCallbacks.message_callback() {
				public void apply(Pointer msg, Pointer userdata) {
					log.debug(msg.getString(0L));
				}
			};
			spotifyLib.SpRegisterDebugCallbacks(spDebugCallbacks, null);
			
			if (username != null && !"".equalsIgnoreCase(username) && blob != null && !"".equalsIgnoreCase(blob)) {
				spotifyLib.SpConnectionLoginBlob(username, blob);
			}

			Thread threadPumpEvents = new Thread(new Runnable() {
				@Override
				public void run() {
					try
					{
						while (true) {
							spotifyLib.SpPumpEvents();
						}
					}
					catch (Exception e)
					{
						log.error("PumpEvents thread error", e);
					}
				}
			});
			threadPumpEvents.start();

			spotifyLib.SpPlaybackSetBitrate(SpBitrate.kSpBitrate320k);
			

			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

				@Override
				public void run() {
					if (NativeUtils.toBoolean(spotifyLib.SpPlaybackIsPlaying())) {
						spotifyLib.SpPlaybackPause();
					}
					spotifyLib.SpConnectionLogout();
					spotifyLib.SpFree();
				}
			}));

			input = new PipedInputStream(4096);
			output = new PipedOutputStream(input);

			audioLine = (SourceDataLine) AudioSystem.getLine(info);
			audioLine.open(pcm);

			Thread playing = new Thread(new Runnable() {

				@Override
				public void run() {

					try {
						while (true) {
							int framesize = AudioSystem.NOT_SPECIFIED;
							byte[] buffer = new byte[4096];
							int numbytes = 0;

							boolean started = false;
							while (true) {
								int bytesread = input.read(buffer, numbytes, buffer.length - numbytes);

								if (bytesread == -1)
									break;
								numbytes += bytesread;
								
								if (!audioLine.isOpen())
									break;

								if (!started) {
									audioLine.start();
									started = true;
								}

								int bytestowrite = numbytes / framesize * framesize;

								audioLine.write(buffer, 0, bytestowrite);

								int remaining = numbytes - bytestowrite;
								if (remaining > 0)
									System.arraycopy(buffer, bytestowrite, buffer, 0, remaining);
								numbytes = remaining;
							}
							if (started) {
								audioLine.stop();
							}
						}
					} catch (Exception e) {
						log.error("Playing thread error", e);
					}

				}
			});
			playing.start();

			instance = this;

		} catch (Exception e) {
			log.error("General error", e);
		}
	}
	
	public SpotifyConnectPlayer(String username, String password) {
		this();
		login(username, password);
	}
	
	public SpotifyConnectPlayer(String username, String password, String playerName) {
		this(username, password);
		setPlayerName(playerName);
	}
	
	public SpotifyConnectPlayer(String playerName) {
		this();
		setPlayerName(playerName);
	}

	@Override
	protected void onActive() {
		try {
			audioLine = (SourceDataLine) AudioSystem.getLine(info);
			audioLine.open(pcm);
		} catch (LineUnavailableException e) {
			log.error("onActive error", e);
		}
	}

	@Override
	protected void onInactive() {
		audioLine.flush();
		audioLine.close();
	}

	@Override
	protected void onAudioFlush() {
		//audioLine.flush();
	}

	@Override
	protected void onAudioData(byte[] data) {
		try {
			output.write(data);
		} catch (IOException e) {
			log.error("onAudioData error", e);
		}
	}

	@Override
	protected void onTokenLost() {
		log.debug("Token lost");
	}

	public Track getPlayingTrack() {
		SpMetadata spMetadata = new SpMetadata.ByReference();
		int retMetadata = spotifyLib.SpGetMetadata(spMetadata, 0);
		if (retMetadata == SpError.kSpErrorOk) {
			Track track = new Track();
			try
			{
				track.setName(new String(spMetadata.track_name, "UTF-8").trim());
				track.setUri(new String(spMetadata.track_uri, "UTF-8").trim());
				track.setArtist(new String(spMetadata.artist_name, "UTF-8").trim());
				track.setArtistUri(new String(spMetadata.artist_uri, "UTF-8").trim());
				track.setAlbum(new String(spMetadata.album_name, "UTF-8").trim());
				track.setAlbumUri(new String(spMetadata.album_uri, "UTF-8").trim());
				track.setDuration(spMetadata.duration);
				track.setCoverUri(new String(spMetadata.cover_uri, "UTF-8").trim());
			}
			catch (UnsupportedEncodingException e)
			{
				log.error("Metadata error", e);
			}
			return track;
		}
		return null;
	}

	@Override
	public boolean getShuffle() {
		return NativeUtils.toBoolean(spotifyLib.SpPlaybackIsShuffled());
	}

	@Override
	public boolean getRepeat() {
		return NativeUtils.toBoolean(spotifyLib.SpPlaybackIsRepeated());
	}

	@Override
	public short getVolume() {
		return spotifyLib.SpPlaybackGetVolume();
	}

	@Override
	public int getSeek() {
		return 0;
	}

	@Override
	public String getAlbumCoverURL() {
		Track playingTrack = getPlayingTrack();
		if (playingTrack != null)
		{
			ByteBuffer url = ByteBuffer.allocate(512);
			NativeSize nativeSize = new NativeSize(url.array().length);
			spotifyLib.SpGetMetadataImageURL(playingTrack.getCoverUri(), SpImageSize.kSpImageSizeSmall, url, nativeSize);
			return new String(url.array());
		}
		else
		{
			return "";
		}
	}

	@Override
	public boolean isPlaying() {
		return NativeUtils.toBoolean(spotifyLib.SpPlaybackIsPlaying());
	}

	@Override
	public String getPlayerName() {
		return spConfig.remoteName.getString(0);
	}

	@Override
	public void setPlayerName(String playerName) {
		spConfig.remoteName = NativeUtils.pointerFrom(playerName);
		spotifyLib.SpSetDisplayName(playerName);
	}

	@Override
	public void play() {
		spotifyLib.SpPlaybackPlay();
	}

	@Override
	public void pause() {
		spotifyLib.SpPlaybackPause();
	}

	@Override
	public void prev() {
		spotifyLib.SpPlaybackSkipToPrev();
	}

	@Override
	public void next() {
		spotifyLib.SpPlaybackSkipToNext();
	}

	@Override
	public void seek(int millis) {
		spotifyLib.SpPlaybackSeek(millis);
	}

	@Override
	public void shuffle(boolean enabled) {
		spotifyLib.SpPlaybackEnableShuffle(NativeUtils.fromBoolean(enabled));
	}

	@Override
	public void repeat(boolean enabled) {
		spotifyLib.SpPlaybackEnableRepeat(NativeUtils.fromBoolean(enabled));
	}

	@Override
	public void volume(short volume) {
		spotifyLib.SpPlaybackUpdateVolume(volume);
	}

	@Override
	public void login(String username, String password) {
		this.username = username;
		spotifyLib.SpConnectionLoginPassword(username, password);
	}

	@Override
	public void logout() {
		spotifyLib.SpConnectionLogout();
	}

	@Override
	public boolean isLoggedIn() {
		return NativeUtils.toBoolean(spotifyLib.SpConnectionIsLoggedIn());
	}
}
