package com.shuffle.scplayer.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sound.sampled.*;
import java.io.IOException;

/**
 * @author crsmoro
 * @author LeanderK
 * @author Tokazio
 * @version 1.1
 */
public class JavaAudioPlayer implements AudioListener {

    /**
     * 
     */
    private static final transient Log log = LogFactory.getLog(JavaAudioPlayer.class);
    
    /**
     * 
     */
    private final SpotifyConnectPlayer player;
    
    /**
     * 
     */
    private SourceDataLine audioLine;
    
    /**
     * 
     */
    private boolean isMuted = false;
    
    /**
     * 
     */
    public static final DataLine.Info DATALINE = new DataLine.Info(SourceDataLine.class, PCM);
    
    /**
     * 
     */
    private boolean active = false;
    
    /**
     * 
     * @param player
     * @throws LineUnavailableException
     * @throws IOException
     * @deprecated
     */
    @Deprecated
    public JavaAudioPlayer(final SpotifyConnectPlayer player) throws LineUnavailableException, IOException {
	this.player = player;
    }
    
    /**
     * 
     * @param player
     * @param aMixerId
     * @throws LineUnavailableException
     * @throws IOException 
     */
    public JavaAudioPlayer(final SpotifyConnectPlayer player, int aMixerId) throws LineUnavailableException, IOException {
	this.player = player;
	open(aMixerId);
    }

    /**
     * 
     * @return 
     */
    public boolean isActive(){
	return this.active;
    }
    
    /**
     * 
     */
    public void mute() {
	if (audioLine == null) {
	    isMuted = true;
	} else {
	    //when requesting the same line you sometimes get different features
	    isMuted = true;
	    if (!audioLine.isControlSupported(BooleanControl.Type.MUTE)) {
		return;
	    }
	    BooleanControl control = (BooleanControl) audioLine.getControl(BooleanControl.Type.MUTE);
	    control.setValue(true);
	}
    }

    /**
     * 
     */
    public void unMute() {
	if (audioLine == null) {
	    isMuted = false;
	} else {
	    isMuted = false;
	    if (!audioLine.isControlSupported(BooleanControl.Type.MUTE)) {
		return;
	    }
	    BooleanControl control = (BooleanControl) audioLine.getControl(BooleanControl.Type.MUTE);
	    control.setValue(false);
	}
    }

    /**
     * 
     */
    @Override
    public void onActive() {
	if(isActive()){
	    return;
	}
	log.info("Activating...");
	/*
	if (audioLine != null && audioLine.isOpen()) {
	    audioLine.close();
	}
	*/
	try {
	    audioLine.open(PCM);
	    onVolumeChanged(player.getVolume());
	    if (isMuted && audioLine.isControlSupported(BooleanControl.Type.MUTE)) {
		((BooleanControl) audioLine.getControl(BooleanControl.Type.MUTE)).setValue(true);
	    }
	    active = true;
	} catch (LineUnavailableException e) {
	    log.error("onActive error", e);
	}
	log.info("Activated.");
    }

    /**
     * 
     */
    @Override
    public void onInactive() {
	if(!isActive()){
	    return;
	}
	log.info("Inactivating...");
	if (audioLine != null) {
	    audioLine.flush();
	    audioLine.close();
	    active = false;
	}
	log.info("Inactivated.");
    }

    /**
     * 
     */
    @Override
    public void onPlay() {
	if(!isActive()){
	    return;
	}
	log.debug("Play");
	if(audioLine!=null && !audioLine.isRunning()){
	    audioLine.start();
	}
	//onActive();
    }

    /**
     * 
     */
    @Override
    public void onPause() {
	if(!isActive()){
	    return;
	}
	log.info("Pause");	
	if(audioLine!=null && audioLine.isRunning()){
	    audioLine.stop();
	}
	//onInactive();
    }

    /**
     * 
     */
    @Override
    public void onAudioFlush() {	
	if (audioLine != null) {
	    audioLine.flush();
	}
    }

    /**
     * 
     */
    @Override
    public void onTokenLost() {
	log.info("token lost");
	//onInactive();
    }

    /**
     * 
     * @param data
     * @param numSamples
     * @return 
     */
    @Override
    public int onAudioData(byte[] data, int numSamples) {
	if(!isActive()){
	    onActive();
	}
	if (audioLine != null && audioLine.isOpen()) {
	    if (!audioLine.isRunning()) {
		audioLine.start();
	    }
	    int toWrite = Math.min(audioLine.available(), data.length);
	    return audioLine.write(data, 0, toWrite);
	} else {
	    log.warn(audioLine + "is null or closed!");	    
	}
	return 0;
    }

    /**
     * 
     * @param volume 
     */
    @Override
    public void onVolumeChanged(int volume) {
	if(!isActive()){
	    return;
	}
	log.info("apply_volume_callback");
	log.debug("volume: " + volume);
	if (audioLine == null) {
	    log.warn("audioLine not ready");
	    return;
	}
	FloatControl volumeControl = (FloatControl) audioLine.getControl(FloatControl.Type.MASTER_GAIN);
	float maxDb = volumeControl.getMaximum();
	log.debug("maxDb : " + maxDb);
	float minDbOrig = volumeControl.getMinimum();
	float minDb = minDbOrig + ((maxDb - minDbOrig) / 3);
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

    /**
     * 
     */
    @Override
    public void close() {
	log.info("Closing...");
	onInactive();
	log.info("Closed.");
    }

    /**
     * 
     * @param aMixerId 
     */
    @Override
    public final void open(int aMixerId) {
	log.info("Opening...");
	Mixer.Info[] mixers = AudioSystem.getMixerInfo();
	if(mixers.length==0){
	    log.error("No sound card!");
	}
	int i = aMixerId < mixers.length ? aMixerId : 0;
	if (!AudioSystem.getMixer(mixers[i]).isLineSupported(DATALINE)) {
	    log.error("Mixer does not support PCM");
	    //throw new IllegalArgumentException("Mixer does not support PCM");
	}
	if (mixers[i] != null) {
	    log.info("Mixer: #"+i+" (" + mixers[i].getName()+")");
	    try {
		audioLine = AudioSystem.getSourceDataLine(PCM, mixers[i]);		
		log.info("Opened: '"+audioLine+"'");
	    } catch (LineUnavailableException ex) {
		log.error("Audio line not created!");
	    }	    
	}else{
	    log.error("No mixer (null)");
	}
    }
    
}
