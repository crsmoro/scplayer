package com.shuffle.scplayer.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sound.sampled.*;
import java.io.IOException;

/**
 * @author crsmoro
 * @author LeanderK
 * @version 1.0
 */
public class AudioPlayer implements AudioListener {
    private static final transient Log log = LogFactory.getLog(AudioPlayer.class);
    private final SpotifyConnectPlayer player;
    private SourceDataLine audioLine;
    private boolean isMuted = false;

    public AudioPlayer(final SpotifyConnectPlayer player) throws LineUnavailableException, IOException {
        this.player = player;
    }

    public void mute() {
        if (audioLine == null) {
            isMuted = true;
        } else {
            //when requesting the same line you sometimes get different features
            isMuted = true;
            if (!audioLine.isControlSupported(BooleanControl.Type.MUTE))
                return;
            BooleanControl control = (BooleanControl) audioLine.getControl(BooleanControl.Type.MUTE);
            control.setValue(true);
        }
    }

    public void unMute() {
        if (audioLine == null) {
            isMuted = false;
        } else {
            isMuted = false;
            if (!audioLine.isControlSupported(BooleanControl.Type.MUTE))
                return;
            BooleanControl control = (BooleanControl) audioLine.getControl(BooleanControl.Type.MUTE);
            control.setValue(false);
        }
    }

    @Override
    public void onActive() {
        if (audioLine != null)
            audioLine.close();

        try {
        	if (player.getMixer() != null) {
        		log.debug("Custom mixer " + player.getMixer().getName());
        		audioLine = AudioSystem.getSourceDataLine(PCM, player.getMixer());
        	}
        	else {
        		audioLine = AudioSystem.getSourceDataLine(PCM);
        	}
            audioLine.open(PCM, 1048576);
            log.debug("player.volume : " + player.getVolume());
            onVolumeChanged(player.getVolume());
            if (isMuted && audioLine.isControlSupported(BooleanControl.Type.MUTE))
                ((BooleanControl) audioLine.getControl(BooleanControl.Type.MUTE)).setValue(true);
        } catch (LineUnavailableException e) {
            log.error("onActive error", e);
        }
    }

    @Override
    public void onInactive() {
        if (audioLine != null) {
            audioLine.flush();
            audioLine.close();
            audioLine = null;
        }
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
    public void onAudioFlush() {
        if (audioLine != null)
            audioLine.flush();
    }

    @Override
    public void onTokenLost() {
        onInactive();
    }

    @Override
    public int onAudioData(byte[] data) {
        if (audioLine != null && audioLine.isOpen()) {
            if (!audioLine.isRunning()) {
                audioLine.start();
            }
            int toWrite = Math.min(audioLine.available(), data.length);
            if (toWrite == audioLine.available())
                log.trace("full! toWrite: " + toWrite + " instead of: " + data.length);
            return audioLine.write(data, 0, toWrite);
        } else {
            return 0;
        }
    }

    @Override
    public void onVolumeChanged(int volume) {
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
        float minDbOrig = volumeControl.getMinimum();
        float minDb = minDbOrig + ((maxDb - minDbOrig)/3);
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
            newVolume = minDbOrig;
        }

        log.debug("newVolume : " + newVolume);
        volumeControl.setValue(newVolume);
    }

    @Override
    public void close() {
        onInactive();
    }
    
}
