package com.shuffle.scplayer.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ochafik.lang.jnaerator.runtime.NativeSize;
import static com.shuffle.scplayer.core.AudioEngine.JAVAAUDIO;
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
 * @author Tokazio
 * @version 1.1
 */
public class SpotifyConnectPlayerImpl implements SpotifyConnectPlayer {

    private static final transient Log log = LogFactory.getLog(SpotifyConnectPlayerImpl.class);
    private static SpConfig spConfig;
    private static SpPlaybackCallbacks spPlaybackCallbacks;
    private static SpConnectionCallbacks spConnectionCallbacks;
    private static SpDebugCallbacks spDebugCallbacks;
    private String username;
    private int bitrate = SpBitrate.kSpBitrate320k;
    private String deviceId;
    private List<PlayerListener> playerListeners = new ArrayList<>();
    private List<AuthenticationListener> authenticationListeners = new ArrayList<>();
    private AudioListener audioListener;

    private int pumpEventsDelay = 100;

    private final Lock libLock = new ReentrantLock();
    private final SpotifyLibrary spotifyLib;

    private static SpotifyConnectPlayer instance;
    private String playerName = "SCPlayer";
    private boolean threadPumpEventsStop = false;

    /**
     * Java audio engine by default
     */
    private AudioEngine audioEngine = AudioEngine.JAVAAUDIO;
   
    /**
     *
     */
    @Deprecated
    public SpotifyConnectPlayerImpl() {
	this(new File("./spotify_appkey.key"));
    }

    /**
     *
     * @param appKey
     */
    @Deprecated
    public SpotifyConnectPlayerImpl(File appKey) {
	this(appKey, new File("./libspotify_embedded_shared.so"));
    }

    /**
     *
     * @param appKey
     * @param library
     */
    @Deprecated
    public SpotifyConnectPlayerImpl(File appKey, File library) {
	this(appKey, library, UUID.randomUUID().toString());
    }

    /**
     * For compatibility
     *
     * @param appKey
     * @param deviceId
     * @deprecated use constructor with audioEngine parameter
     */
    @Deprecated
    public SpotifyConnectPlayerImpl(File appKey, String deviceId) {
	this(appKey, new File("./libspotify_embedded_shared.so"), deviceId);
    }

    /**
     *
     * @param appKey
     * @param deviceId
     * @param aAudioEngine
     */
    public SpotifyConnectPlayerImpl(File appKey, String deviceId, AudioEngine aAudioEngine, int aMixerId) {
	this(appKey, new File("./libspotify_embedded_shared.so"), deviceId, aAudioEngine, aMixerId);
    }

    /**
     *
     * @param appKey
     * @param library
     * @param deviceId
     * @param aAudioEngine
     * @throws IllegalArgumentException
     */
    public SpotifyConnectPlayerImpl(File appKey, File library, String deviceId, AudioEngine aAudioEngine, int aMixerId) throws IllegalArgumentException {
	try {
	    if (instance != null) {
		log.warn("Already Initialized");
		throw new IllegalArgumentException("Already Initialized");
	    }
	    spotifyLib = (SpotifyLibrary) Native.loadLibrary(library.getAbsolutePath(), SpotifyLibrary.class);
	} catch (Exception e) {
	    log.error("General error", e);
	    throw new IllegalArgumentException("general error", e);
	}
	this.audioEngine = aAudioEngine;
	init(appKey, library, deviceId, aMixerId);
    }

    /**
     * For compatibility
     *
     * @param appKey
     * @param library
     * @param deviceId
     * @throws IllegalArgumentException
     * @deprecated use constructor with audioEngine parameter
     */
    @Deprecated
    public SpotifyConnectPlayerImpl(File appKey, File library, String deviceId) throws IllegalArgumentException {
	try {
	    if (instance != null) {
		log.warn("Already Initialized");
		throw new IllegalArgumentException("Already Initialized");
	    }
	    spotifyLib = (SpotifyLibrary) Native.loadLibrary(library.getAbsolutePath(), SpotifyLibrary.class);
	} catch (Exception e) {
	    log.error("General error", e);
	    throw new IllegalArgumentException("general error", e);
	}
	init(appKey, library, deviceId, 0);
    }

    /**
     *
     * @param appKey
     * @param library
     * @param deviceId
     */
    private void init(File appKey, File library, String deviceId, int aMixerId) {
	try {

	    log.info("init");

	    byte[] appKeyByte = IOUtils.toByteArray(new FileInputStream(appKey));

	    this.deviceId = deviceId;

	    spConfig = initSPConfig(appKeyByte);

	    registerConnectionCallbacks();
	    registerPlaybackCallbacks();
	    registerDebugCallbackss();

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

	    spotifyLib.SpPlaybackSetBitrate(getBitrate());

	    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
		@Override
		public void run() {
		    close();
		}
	    }));

	    
	    
	    try {
		initAudioEngine(aMixerId);
	    } catch (Exception ex) {
		log.warn("Error initializing audio engine: " + ex.getMessage());
	    }

	    instance = this;

	} catch (Exception e) {
	    log.error("General error", e);
	    throw new IllegalArgumentException("general error", e);
	}
    }

    /**
     *
     * @throws Exception
     */
    private void initAudioEngine(int mixerId) throws Exception {
	System.out.println("Audio engine: " + audioEngine);
	switch (audioEngine) {
	    case JAVAAUDIO:
		audioListener = new JavaAudioPlayer(this, mixerId);
		break;
	    case OPENALAUDIO:
		audioListener = new OpenALAudioPlayer(this, mixerId);
		break;
	    default:
		log.error("Audio engine not known");
		throw new Exception("Audio engine not known");
	}
    }

    private void registerConnectionCallbacks() {
	spConnectionCallbacks = new SpConnectionCallbacks.ByReference();
	spConnectionCallbacks.notify$ = new SpPlaybackCallbacks.notify_callback() {
	    public void apply(int notification, Pointer userdata) {
		if (notification == SpConnectionNotify.kSpConnectionNotifyLoggedIn) {
		    for (AuthenticationListener connectionListener : authenticationListeners) {
			connectionListener.onLoggedIn(username);
		    }
		} else if (notification == SpConnectionNotify.kSpConnectionNotifyLoggedOut) {
		    for (AuthenticationListener connectionListener : authenticationListeners) {
			connectionListener.onLoggedOut(username);
		    }
		}
	    }
	};
	spConnectionCallbacks.new_credentials = new SpConnectionCallbacks.new_credentials_callback() {
	    public void apply(Pointer blob, Pointer userdata) {
		log.trace("new_credentials_callback");
		log.trace("blob : " + blob.getString(0));
		for (AuthenticationListener connectionListener : authenticationListeners) {
		    connectionListener.onNewCredentials(username, blob.getString(0));
		}
	    }
	};
	spotifyLib.SpRegisterConnectionCallbacks(spConnectionCallbacks, null);
    }

    private void registerPlaybackCallbacks() {
	spPlaybackCallbacks = new SpPlaybackCallbacks.ByReference();
	spPlaybackCallbacks.notify$ = new SpPlaybackCallbacks.notify_callback() {
	    public void apply(int notification, Pointer userdata) {
		log.info("playback_notify_callback");
		log.info("notification : " + notification);
		if (notification == SpPlaybackNotify.kSpPlaybackNotifyPlay) {
		    for (PlayerListener playerListener : playerListeners) {
			playerListener.onPlay();
		    }
		    audioListener.onPlay();
		} else if (notification == SpPlaybackNotify.kSpPlaybackNotifyPause) {
		    for (PlayerListener playerListener : playerListeners) {
			playerListener.onPause();
		    }
		    audioListener.onPause();
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
		    int i = audioListener.onAudioData(samples.getByteArray(0, num_samples * AudioListener.SAMPLESIZE), num_samples);
		    return i / AudioListener.SAMPLESIZE;
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
    }

    private void registerDebugCallbackss() {
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
		spotifyLib.SpGetMetadataImageURL(playingTrack.getCoverUri(), SpImageSize.kSpImageSizeSmall, url,
			nativeSize);
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
    public void loginBlob(String username, String blob) {
	this.username = username;
	try {
	    libLock.lock();
	    spotifyLib.SpConnectionLoginBlob(username, blob);
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
    public void addAuthenticationListener(AuthenticationListener authenticationListener) {
	authenticationListeners.add(authenticationListener);
    }

    @Override
    public void removeAuthenticationListener(AuthenticationListener authenticationListener) {
	authenticationListeners.remove(authenticationListener);
    }

    @Override
    public String getDeviceId() {
	return deviceId;
    }

    @Override
    public int getBitrate() {
	return bitrate;
    }

    @Override
    public void setBitrate(int bitrate) {
	this.bitrate = bitrate;
    }

}
