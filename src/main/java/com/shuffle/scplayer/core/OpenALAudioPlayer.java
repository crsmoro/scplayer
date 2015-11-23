package com.shuffle.scplayer.core;

import java.io.IOException;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.urish.openal.ALException;
import org.urish.openal.Device;
import org.urish.openal.OpenAL;
import org.urish.openal.Source;
import org.urish.openal.SourceBufferedOutputStream;
import org.urish.openal.jna.ALFactory;

/**
 *
 * @author Romain PETIT <tokazio@esyo.net>
 * @version 1.0
 */
public class OpenALAudioPlayer implements AudioListener {

    /**
     *
     */
    private static final transient Log log = LogFactory.getLog(OpenALAudioPlayer.class);

    /**
     *
     */
    private final SpotifyConnectPlayer player;

    /**
     *
     */
    private boolean isMuted = false;

    /**
     *
     */
    private OpenAL openal;

    /**
     *
     */
    private Source source;

    /**
     *
     */
    private SourceBufferedOutputStream stream;

    /**
     *
     */
    private float gain;

    /**
     *
     */
    private boolean active = false;

    /**
     *
     * @param player
     */
    @Deprecated
    public OpenALAudioPlayer(final SpotifyConnectPlayer player) {
	this.player = player;
    }

    /**
     *
     * @param player
     * @param aMixerId
     */
    public OpenALAudioPlayer(final SpotifyConnectPlayer player, int aMixerId) {
	this.player = player;
	open(aMixerId);
    }

    /**
     *
     */
    public void mute() {
	if (!isActive()) {
	    return;
	}
	System.out.println("Mute");
	try {
	    gain = source.getGain();
	    source.setGain(0);
	    isMuted = true;
	} catch (ALException ex) {
	    log.warn("OpenALPlayer::mute() => " + ex.getClass().getName() + ": " + ex.getMessage());
	}
    }

    /**
     *
     */
    public void unmute() {
	if (!isActive()) {
	    return;
	}
	System.out.println("Unmute");
	try {
	    source.setGain(gain);
	    isMuted = false;
	} catch (ALException ex) {
	    log.warn("OpenALPlayer::unmute() => " + ex.getClass().getName() + ": " + ex.getMessage());
	}
    }

    /**
     *
     */
    @Override
    public void onActive() {
	if (isActive()) {
	    return;
	}
	log.info("Activating...");
	if (source != null) {
	    source.close();
	}
	if (stream != null) {
	    try {
		stream.close();
	    } catch (IOException ex) {
		log.error("OpenALPlayer::onActive()-stream close => " + ex.getClass().getName() + ": " + ex.getMessage());
	    }
	}
	//Crée la source
	try {
	    source = openal.createSource();
	    //Stream
	    try {
		stream = source.createOutputStream(PCM, 32, 8192);
		this.active = true;
		onVolumeChanged(player.getVolume());
	    } catch (ALException ex) {
		log.error("OpenALPlayer::onActive()-stream create => " + ex.getClass().getName() + ": " + ex.getMessage());
	    }
	} catch (ALException ex) {
	    log.error("OpenALPlayer::onActive()-source => " + ex.getClass().getName() + ": " + ex.getMessage());
	}
	log.info("Activated.");
    }

    /**
     *
     */
    @Override
    public void onInactive() {
	if (!isActive()) {
	    return;
	}
	log.info("Inactivating...");
	if (source != null) {
	    try {
		source.stop();
	    } catch (ALException ex) {
		log.error("OpenALPlayer::onInactive() => " + ex.getClass().getName() + ": " + ex.getMessage());
	    }
	    source.close();
	}
	this.active = false;
	log.info("Inactivated.");
    }

    /**
     *
     */
    @Override
    public void onPlay() {
	if (!isActive()) {
	    return;
	}
	log.info("Play");
	/*
	if (source != null) {
	    try {
		source.play();
	    } catch (ALException ex) {
		log.error("OpenALPlayer::onPlay() => " + ex.getClass().getName() + ": " + ex.getMessage());
	    }
	}
	*/
    }

    /**
     *
     */
    @Override
    public void onPause() {
	if (!isActive()) {
	    return;
	}
	log.info("Pause");
	/*
	if (source != null) {
	    try {
		source.pause();
	    } catch (ALException ex) {
		log.error("OpenALPplayer::onPause() => " + ex.getClass().getName() + ": " + ex.getMessage());
	    }
	}
	*/
    }

    /**
     *
     */
    @Override
    public void onTokenLost() {
	if (!isActive()) {
	    return;
	}
	log.info("Tokenlost");

    }

    /**
     *
     */
    @Override
    public void onAudioFlush() {
	if (!isActive()) {
	    return;
	}
	log.info("Flush");
	/*
	if (source != null && stream != null) {
	    try {
		stream.flush();
	    } catch (IOException ex) {
		log.error("OpenALPlayer::onAudioFlush() => " + ex.getClass().getName() + ": " + ex.getMessage());
	    }
	}
	*/
    }

    /**
     *
     * @param data
     * @param numSamples
     * @return
     */
    @Override
    public int onAudioData(byte[] data, int numSamples) {
	if (!isActive()) {
	    return 0;
	}
	if (source != null && stream != null) {
	    try {
		stream.write(data);
		return data.length;
	    } catch (IOException ex) {
		log.error("OpenALPlayer::onAudioData() => " + ex.getClass().getName() + ": " + ex.getMessage());
	    }
	}
	return 0;
    }

    /**
     *
     * @param volume
     */
    @Override
    public void onVolumeChanged(int volume) {
	if (!isActive()) {
	    return;
	}
	if (source == null) {
	    log.error("No source!");
	    return;
	}
	float v = ((float) Math.abs(volume)) / 32768f;
	log.info("Volume changed: " + volume + "(" + v + ")");
	try {
	    source.setGain(v);
	} catch (ALException ex) {
	    log.error("OpenALPlayer::onVolumeChanged() => " + ex.getClass().getName() + ": " + ex.getMessage());
	}
    }

    /**
     *
     */
    @Override
    public void close() {
	log.info("Closing...");
	onInactive();
	if (openal != null) {
	    openal.close();
	}
	log.info("Closed.");
    }

    /**
     *
     * @return
     */
    private boolean isActive() {
	return active;
    }

    /**
     *
     * @param aMixerId
     */
    @Override
    public final void open(int aMixerId) {
	log.info("Opening...");
	try {
	    ALFactory alf = new ALFactory();
	    List<String> devices = Device.availableDevices(alf);
	    int i = aMixerId < devices.size() ? aMixerId : 0;
	    String n = devices.get(i);
	    log.info("Using mixer #" + i + " (" + n + ")");
	    openal = new OpenAL(alf, n);
	} catch (ALException ex) {
	    log.warn("OpenALPlayer::open() => " + ex.getClass().getName() + ": " + ex.getMessage());
	}
	log.info("Opened.");
    }

}
