package com.shuffle.scplayer.core;

import java.io.IOException;
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
 */
public class OpenALPlayer implements AudioListener {

    /**
     * 
     */
    private static final transient Log log = LogFactory.getLog(OpenALPlayer.class);
    
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
     * @param player
     */
    public OpenALPlayer(final SpotifyConnectPlayer player){
	this.player = player;
    }

    /**
     * 
     */
    public void mute() {
	try {
	    gain = source.getGain();
	    source.setGain(0);
	    isMuted = true;
	} catch (ALException ex) {
	    log.warn("OpenALPlayer::mute() => "+ex.getClass().getName() + ": " + ex.getMessage());
	}
    }
    
    /**
     * 
     */
    public void unmute() {
	try {
	    source.setGain(gain);
	    isMuted = false;
	} catch (ALException ex) {
	    log.warn("OpenALPlayer::unmute() => "+ex.getClass().getName() + ": " + ex.getMessage());
	}
    }
    
    /**
     * 
     */
    @Override
    public void onActive() {
	//OpenAL init
	if (openal != null) {
	    openal.close();
	}
	try {
	    ALFactory alf = new ALFactory();
	    openal = new OpenAL(alf,Device.availableDevices(alf).get(player.getMixerId()));
	} catch (ALException ex) {
	    log.warn("OpenALPlayer::onActive()-openal => "+ex.getClass().getName() + ": " + ex.getMessage());
	}
	//Crée la source
	if (source != null) {
	    source.close();
	}
	try {
	    source = openal.createSource();
	} catch (ALException ex) {
	    log.warn("OpenALPlayer::onActive()-source => "+ex.getClass().getName() + ": " + ex.getMessage());
	}
	//Stream
	if (stream != null) {
	    try {
		stream.close();
	    } catch (IOException ex) {
		log.warn("OpenALPlayer::onActive()-stream close => "+ex.getClass().getName() + ": " + ex.getMessage());
	    }
	}
	try {
	    stream = source.createOutputStream(PCM,8,8192);//new AudioFormat(44100, 16, 2, false, false));	    
	} catch (ALException ex) {
	    log.warn("OpenALPlayer::onActive()-stream create => "+ex.getClass().getName() + ": " + ex.getMessage());
	}
    }

    @Override
    public void onInactive() {
	try {
	    source.stop();
	} catch (ALException ex) {
	    log.warn("OpenALPlayer::onInactive() => "+ex.getClass().getName() + ": " + ex.getMessage());
	}
	source.close();
	openal.close();
    }

    @Override
    public void onPlay() {
	onActive();
    }

    @Override
    public void onPause() {
	onInactive();
    }

    @Override
    public void onTokenLost() {
	onInactive();
    }

    @Override
    public void onAudioFlush() {

    }

    @Override
    public int onAudioData(byte[] data) {
	if (stream != null) {
	    try {
		stream.write(data);
		return data.length;
	    } catch (IOException ex) {
		log.warn("OpenALPlayer::onAudioData() => "+ex.getClass().getName() + ": " + ex.getMessage());
	    }
	}
	return 0;
    }

    @Override
    public void onVolumeChanged(short volume) {
	log.info("apply_volume_callback");
        log.debug("volume: " + volume);
        if (source == null)
        {
            log.warn("openAL source not ready");
            return;
        }
	
	try {
	    source.setGain(volume);
	} catch (ALException ex) {
	    log.warn("OpenALPlayer::onVolumeChanged() => "+ex.getClass().getName() + ": " + ex.getMessage());
	}
    }

    @Override
    public void	close() {
	onInactive();	
    }

}
