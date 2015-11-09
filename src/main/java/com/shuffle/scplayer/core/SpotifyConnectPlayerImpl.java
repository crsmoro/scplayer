package com.shuffle.scplayer.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

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
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * @author crsmoro
 * @author LeanderK
 */
public class SpotifyConnectPlayerImpl implements SpotifyConnectPlayer {

    private static final transient Log log = LogFactory.getLog(SpotifyConnectPlayerImpl.class);
    private static SpConfig spConfig;
    private static SpPlaybackCallbacks spPlaybackCallbacks;
    private static SpConnectionCallbacks spConnectionCallbacks;
    private static SpDebugCallbacks spDebugCallbacks;
    private final transient Gson gson = new GsonBuilder().create();
    private String username;
    private String blob;
    private boolean rememberMe;
    private boolean forcePlayerName = false;
    private int bitrate = SpBitrate.kSpBitrate320k;
    private Map<Integer, Integer> mapBitrate = new HashMap<>();
    private String deviceId = UUID.randomUUID().toString();
    private List<PlayerListener> playerListeners = new ArrayList<>();
    private AudioListener audioListener;
    private Mixer.Info mixer;
    private int pumpEventsDelay = 100;

    private final Lock libLock =  new ReentrantLock();
    private final SpotifyLibrary spotifyLib;

    private static SpotifyConnectPlayer instance;
    private String playerName = "SCPlayer";
    private boolean threadPumpEventsStop = false;
    
    {
    	mapBitrate.put(90, SpBitrate.kSpBitrate90k);
    	mapBitrate.put(160, SpBitrate.kSpBitrate160k);
    	mapBitrate.put(320, SpBitrate.kSpBitrate320k);
    }

    private final File credentials = new File("./credentials.json");

    public SpotifyConnectPlayerImpl() {
        this(new File("./spotify_appkey.key"), "spotify_embedded_shared");
    }

    public SpotifyConnectPlayerImpl(File appKey) {
        this(appKey, "spotify_embedded_shared");
    }

    public SpotifyConnectPlayerImpl(File appKey, Properties properties) {
        this(appKey, "spotify_embedded_shared");
    }

    public SpotifyConnectPlayerImpl(File appKey, String libraryName) {
    	this(appKey, libraryName, System.getProperties());
    }

    public SpotifyConnectPlayerImpl(File appKey, String libraryName, Properties properties) throws IllegalArgumentException {
        try {
            if (instance != null) {
                log.warn("Already Initialized");
                throw new IllegalArgumentException("Already Initialized");
            }

            spotifyLib = (SpotifyLibrary) Native.loadLibrary(libraryName, SpotifyLibrary.class);

            log.info("init");
            if (AudioSystem.getMixerInfo().length <= 0) {
                log.error("No sound cards Avaliables");
                throw new Exception("No sound cards Avaliables");
            }

            byte[] appKeyByte = IOUtils.toByteArray(new FileInputStream(appKey));
            
            if (properties.getProperty("player.name") != null)
            {
            	forcePlayerName = true;
            }
            playerName = properties.getProperty("player.name", "SCPlayer");
    		username = properties.getProperty("username");
    		String password = properties.getProperty("password");
    		rememberMe = Boolean.parseBoolean(properties.getProperty("remember.me", "true"));
    		bitrate = Integer.getInteger("bitrate", SpBitrate.kSpBitrate320k);

    		if (username == null || "".equalsIgnoreCase(username) || password == null || "".equalsIgnoreCase(password))
    		{
    			verifyCredentialFile(credentials);
    		}

            spConfig = initSPConfig(appKeyByte);

            audioListener = new AudioPlayer(this);
            String mixerString = properties.getProperty("mixer", "0");
            if (mixerString != null && !"".equalsIgnoreCase(mixerString))
            {
            	Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            	Mixer.Info mixer = mixers[0];
            	try {
            		int mixerInt = new Integer(mixerString);
            		mixer = mixerInt < mixers.length  ? mixers[mixerInt] : mixers[0];
            	}
            	catch (NumberFormatException e) {
            		for (int i=0; i<mixers.length; i++) {
            			if (mixers[i].getName().equals(mixerString)) {
            				mixer = mixers[i];
            			}
            		}
            	}
            	setMixer(mixer);
            }


            spConnectionCallbacks = new SpConnectionCallbacks.ByReference();
            spConnectionCallbacks.notify$ = new SpPlaybackCallbacks.notify_callback() {
                public void apply(int notification, Pointer userdata) {
                    if (notification == SpConnectionNotify.kSpConnectionNotifyLoggedIn) {
                        for (PlayerListener playerListener : playerListeners) {
                            playerListener.onLoggedIn();
                        }
                    } else if (notification == SpConnectionNotify.kSpConnectionNotifyLoggedOut) {
                        for (PlayerListener playerListener : playerListeners) {
                            playerListener.onLoggedOut();
                        }
                    }
                }
            };
            spConnectionCallbacks.new_credentials = new SpConnectionCallbacks.new_credentials_callback() {
                public void apply(Pointer blob, Pointer userdata) {
                    log.debug("new_credentials_callback");
                    log.trace("blob : " + blob.getString(0));
                    log.trace("remember : " + rememberMe);
                    if (rememberMe)
                    {
                    	JsonObject credentialsJson = new JsonObject();
                        credentialsJson.addProperty("username", username);
                        credentialsJson.addProperty("blob", blob.getString(0));
                        credentialsJson.addProperty("playerName", getPlayerName());
                        credentialsJson.addProperty("deviceId", deviceId);
                        try {
                        	if (!credentials.exists())
                        	{
                        		log.debug("Credentials file not exists");
                                try
                    			{
                    				credentials.createNewFile();
                    			}
                    			catch (SecurityException | IOException e)
                    			{
                    				log.error("Error trying to write credentials file", e);
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
            };
            spotifyLib.SpRegisterConnectionCallbacks(spConnectionCallbacks, null);

            spPlaybackCallbacks = new SpPlaybackCallbacks.ByReference();
            registerCallbacks();

            Thread threadPumpEvents = new Thread(new Runnable() {

                @Override
                public void run() {
                    while (!threadPumpEventsStop) {
                        try {
                            try {
                                libLock.lock();
                                spotifyLib.SpPumpEvents();
                            } finally {
                                libLock.unlock();
                            }
                            try {
                                Thread.sleep(pumpEventsDelay);
                            } catch (InterruptedException ignored) {

                            }
                        } catch (Exception e) {
                            log.error("PumpEvents thread error", e);
                            e.printStackTrace();
                        }
                    }
                }
            });
            threadPumpEvents.start();

            spotifyLib.SpPlaybackSetBitrate(mapBitrate.get(bitrate) != null ? mapBitrate.get(bitrate) : SpBitrate.kSpBitrate320k);

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    close();
                }
            }));
            
            if (username != null && !"".equalsIgnoreCase(username) && password != null && !"".equalsIgnoreCase(password)) {
            	spotifyLib.SpConnectionLoginPassword(username, password);
            }
            else if (username != null && !"".equalsIgnoreCase(username) && blob != null && !"".equalsIgnoreCase(blob)) {
                spotifyLib.SpConnectionLoginBlob(username, blob);
            }

            instance = this;

        } catch (Exception e) {
            log.error("General error", e);
            throw new IllegalArgumentException("general error", e);
        }
    }

    private void registerCallbacks() {
        spPlaybackCallbacks.notify$ = new SpPlaybackCallbacks.notify_callback() {
            public void apply(int notification, Pointer userdata) {
                log.info("playback_notify_callback");
                log.info("notification : " + notification);
                if (notification == SpPlaybackNotify.kSpPlaybackNotifyPlay) {
                    for (PlayerListener playerListener : playerListeners) {
                        playerListener.onPlay();
                    }
                } else if (notification == SpPlaybackNotify.kSpPlaybackNotifyPause) {
                    for (PlayerListener playerListener : playerListeners) {
                        playerListener.onPause();
                    }
                } else if (notification == SpPlaybackNotify.kSpPlaybackNotifyTrackChanged) {
                    for (PlayerListener playerListener : playerListeners) {
                        playerListener.onTrackChanged(getPlayingTrack());
                    }
                } else if (notification == SpPlaybackNotify.kSpPlaybackNotifyNext) {
                    for (PlayerListener playerListener : playerListeners) {
                        playerListener.onNextTrack(getPlayingTrack());
                    }
                } else if (notification == SpPlaybackNotify.kSpPlaybackNotifyPrev) {
                    for (PlayerListener playerListener : playerListeners) {
                        playerListener.onPreviousTrack(getPlayingTrack());
                    }
                } else if (notification == SpPlaybackNotify.kSpPlaybackNotifyShuffleEnabled) {
                    for (PlayerListener playerListener : playerListeners) {
                        playerListener.onShuffle(true);
                    }
                } else if (notification == SpPlaybackNotify.kSpPlaybackNotifyShuffleDisabled) {
                    for (PlayerListener playerListener : playerListeners) {
                        playerListener.onShuffle(false);
                    }
                } else if (notification == SpPlaybackNotify.kSpPlaybackNotifyRepeatEnabled) {
                    for (PlayerListener playerListener : playerListeners) {
                        playerListener.onRepeat(true);
                    }
                } else if (notification == SpPlaybackNotify.kSpPlaybackNotifyRepeatDisabled) {
                    for (PlayerListener playerListener : playerListeners) {
                        playerListener.onRepeat(false);
                    }
                } else if (notification == SpPlaybackNotify.kSpPlaybackNotifyBecameActive) {
                    for (PlayerListener playerListener : playerListeners) {
                        playerListener.onActive();
                    }
                    audioListener.onActive();
                } else if (notification == SpPlaybackNotify.kSpPlaybackNotifyBecameInactive) {
                    for (PlayerListener playerListener : playerListeners) {
                        playerListener.onInactive();
                    }
                    audioListener.onInactive();
                } else if (notification == SpPlaybackNotify.kSpPlaybackNotifyPlayTokenLost) {
                    for (PlayerListener playerListener : playerListeners) {
                        playerListener.onTokenLost();
                    }
                    audioListener.onTokenLost();
                } else if (notification == SpPlaybackNotify.kSpPlaybackEventAudioFlush) {
                    audioListener.onAudioFlush();
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
                    num_samples -= (num_samples % AudioListener.CHANNELS);
                    audioListener.onAudioData(samples.getByteArray(0, num_samples * AudioListener.SAMPLESIZE));
                    return num_samples * AudioListener.SAMPLESIZE;
                } catch (Exception e) {
                    log.error("spotifyLib audio data error", e);
                    return 0;
                }
            }
        };
        spPlaybackCallbacks.seek = new SpPlaybackCallbacks.seek_callback() {
            public void apply(int millis, Pointer userdata) {
                for (PlayerListener playerListener : playerListeners) {
                    playerListener.onSeek(millis);
                }
            }
        };
        spPlaybackCallbacks.apply_volume = new SpPlaybackCallbacks.apply_volume_callback() {
            public void apply(short volume, Pointer userdata) {
                for (PlayerListener playerListener : playerListeners) {
                    playerListener.onVolumeChanged(volume);
                }
                audioListener.onVolumeChanged(volume);
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
    }

    private SpConfig initSPConfig(byte[] appKeyByte) {
        SpConfig.ByReference spConfig = new SpConfig.ByReference();
        spConfig.version = 4;
        
        Pointer pointerBlank = new Memory(1048576L);
        Pointer pointerAppKey = NativeUtils.pointerFrom(appKeyByte);

        spConfig.buffer = pointerBlank;
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
            throw new IllegalArgumentException("Error initializing spotifyLib, returned " + retorno);
        }
        return spConfig;
    }

    private void verifyCredentialFile(File credentials) throws IOException, IllegalArgumentException {
        if (!credentials.exists()) {
            log.debug("Credentials file not exists");
            try
			{
				credentials.createNewFile();
			}
			catch (SecurityException | IOException e)
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
                    if (!forcePlayerName)
                    {
                    	this.playerName = credentialsJson.get("playerName").getAsString();
                        log.trace("PlayerName : " + playerName);
                    }
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

    @Override
    public Track getPlayingTrack() {
        SpMetadata spMetadata = new SpMetadata.ByReference();
        int retMetadata;
        try {
            libLock.lock();
            retMetadata = spotifyLib.SpGetMetadata(spMetadata, 0);
        } finally {
            libLock.unlock();
        }
        if (retMetadata == SpError.kSpErrorOk) {
            Track track = new Track();
            try {
                track.setName(new String(spMetadata.track_name, "UTF-8").trim());
                track.setUri(new String(spMetadata.track_uri, "UTF-8").trim());
                track.setArtist(new String(spMetadata.artist_name, "UTF-8").trim());
                track.setArtistUri(new String(spMetadata.artist_uri, "UTF-8").trim());
                track.setAlbum(new String(spMetadata.album_name, "UTF-8").trim());
                track.setAlbumUri(new String(spMetadata.album_uri, "UTF-8").trim());
                track.setDuration(spMetadata.duration);
                track.setCoverUri(new String(spMetadata.cover_uri, "UTF-8").trim());
            } catch (UnsupportedEncodingException e) {
                log.error("Metadata error", e);
            }
            return track;
        }
        return null;
    }

    @Override
    public boolean getShuffle() {
        try {
            libLock.lock();
            return NativeUtils.toBoolean(spotifyLib.SpPlaybackIsShuffled());
        } finally {
            libLock.unlock();
        }
    }

    @Override
    public boolean getRepeat() {
        try {
            libLock.lock();
            return NativeUtils.toBoolean(spotifyLib.SpPlaybackIsRepeated());
        } finally {
            libLock.unlock();
        }
    }

    @Override
    public short getVolume() {
        try {
            libLock.lock();
            return spotifyLib.SpPlaybackGetVolume();
        } finally {
            libLock.unlock();
        }
    }

    @Override
    public int getSeek() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String getAlbumCoverURL() {
        Track playingTrack = getPlayingTrack();
        if (playingTrack != null) {
            ByteBuffer url = ByteBuffer.allocate(512);
            NativeSize nativeSize = new NativeSize(url.array().length);
            try {
                libLock.lock();
                spotifyLib.SpGetMetadataImageURL(playingTrack.getCoverUri(),
                        SpImageSize.kSpImageSizeSmall, url, nativeSize);
            } finally {
                libLock.unlock();
            }
            return new String(url.array()).trim();
        } else {
            return "";
        }
    }

    @Override
    public boolean isPlaying() {
        try {
            libLock.lock();
            return NativeUtils.toBoolean(spotifyLib.SpPlaybackIsPlaying());
        } finally {
            libLock.unlock();
        }
    }

    @Override
    public String getPlayerName() {
        return spConfig.remoteName.getString(0);
    }

    @Override
    public void setPlayerName(String playerName) {
        spConfig.remoteName = NativeUtils.pointerFrom(playerName);
        try {
            libLock.lock();
            spotifyLib.SpSetDisplayName(playerName);
        } finally {
            libLock.unlock();
        }
    }

    @Override
    public void play() {
        try {
            libLock.lock();
            spotifyLib.SpPlaybackPlay();
        } finally {
            libLock.unlock();
        }
    }

    @Override
    public void pause() {
        try {
            libLock.lock();
            spotifyLib.SpPlaybackPause();
        } finally {
            libLock.unlock();
        }
    }

    @Override
    public void prev() {
        try {
            libLock.lock();
            spotifyLib.SpPlaybackSkipToPrev();
        } finally {
            libLock.unlock();
        }
    }

    @Override
    public void next() {
        try {
            libLock.lock();
            spotifyLib.SpPlaybackSkipToNext();
        } finally {
            libLock.unlock();
        }
    }

    @Override
    public void seek(int millis) {
        try {
            libLock.lock();
            spotifyLib.SpPlaybackSeek(millis);
        } finally {
            libLock.unlock();
        }
    }

    @Override
    public void shuffle(boolean enabled) {
        try {
            libLock.lock();
            spotifyLib.SpPlaybackEnableShuffle(NativeUtils.fromBoolean(enabled));
        } finally {
            libLock.unlock();
        }
    }

    @Override
    public void repeat(boolean enabled) {
        try {
            libLock.lock();
            spotifyLib.SpPlaybackEnableRepeat(NativeUtils.fromBoolean(enabled));
        } finally {
            libLock.unlock();
        }
    }

    @Override
    public void volume(short volume) {
        try {
            libLock.lock();
            spotifyLib.SpPlaybackUpdateVolume(volume);
        } finally {
            libLock.unlock();
        }
    }

    @Override
    public void login(String username, String password) {
        this.username = username;
        try {
            libLock.lock();
            spotifyLib.SpConnectionLoginPassword(username, password);
        } finally {
            libLock.unlock();
        }
    }

    @Override
    public void logout() {
        try {
            libLock.lock();
            spotifyLib.SpConnectionLogout();
        } finally {
            libLock.unlock();
        }
    }

    @Override
    public boolean isLoggedIn() {
        try {
            libLock.lock();
            return NativeUtils.toBoolean(spotifyLib.SpConnectionIsLoggedIn());
        } finally {
            libLock.unlock();
        }
    }

    @Override
    public boolean isActive() {
        try {
            libLock.lock();
            return NativeUtils.toBoolean(spotifyLib.SpPlaybackIsActiveDevice());
        } finally {
            libLock.unlock();
        }
    }

    @Override
    public void close() {
        threadPumpEventsStop = true;
        try {
            libLock.lock();
            if (NativeUtils.toBoolean(spotifyLib.SpPlaybackIsPlaying())) {
                spotifyLib.SpPlaybackPause();
            }
            spotifyLib.SpConnectionLogout();
            spotifyLib.SpFree();
        } finally {
            libLock.unlock();
        }
        audioListener.close();
    }

    @Override
    public void addPlayerListener(PlayerListener playerListener) {
        playerListeners.add(playerListener);
    }

    @Override
    public void removePlayerListener(PlayerListener playerListener) {
        playerListeners.remove(playerListener);
    }

    @Override
    public AudioListener getAudioListener() {
        return audioListener;
    }

    @Override
    public void setAudioListener(AudioListener audioListener) {
        this.audioListener = audioListener;
    }

    @Override
    public Mixer.Info getMixer() {
    	return mixer;
    }

    @Override
    public void setMixer(Mixer.Info mixer) {
    	if (!AudioSystem.getMixer(mixer).isLineSupported(AudioListener.DATALINE)) {
    		throw new IllegalArgumentException("Mixer does not support PCM");
    	}
    	this.mixer = mixer;
    }
}
