package com.shuffle.scplayer.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * @author crsmoro
 * @author LeanderK
 * @version 1.0
 */
public class AudioPlayer implements AudioListener {
    private static final transient Log log = LogFactory.getLog(AudioPlayer.class);
    private final SpotifyConnectPlayer spotifyConnectPlayer;

    private AudioFormat pcm = new AudioFormat(RATE, 16, CHANNELS, true, false);
    private DataLine.Info info = new DataLine.Info(SourceDataLine.class, pcm);
    private SourceDataLine audioLine;
    private PipedInputStream input;
    private PipedOutputStream output;

    public AudioPlayer(SpotifyConnectPlayer spotifyConnectPlayer) throws LineUnavailableException, IOException {
        this.spotifyConnectPlayer = spotifyConnectPlayer;
        audioLine = (SourceDataLine) AudioSystem.getLine(info);
        audioLine.open(pcm);

        input = new PipedInputStream(4096);
        output = new PipedOutputStream(input);

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

    }

    @Override
    public void onActive() {
        try {
            audioLine = (SourceDataLine) AudioSystem.getLine(info);
            audioLine.open(pcm);
        } catch (LineUnavailableException e) {
            log.error("onActive error", e);
        }
    }

    @Override
    public void onInactive() {
        audioLine.flush();
        audioLine.close();
    }

    @Override
    public void onAudioFlush() {
        //audioLine.flush();
    }

    @Override
    public void onTokenLost() {
        onInactive();
    }

    @Override
    public void onAudioData(byte[] data) {
        try {
            output.write(data);
        } catch (IOException e) {
            log.error("onAudioData error", e);
        }
    }

    @Override
    public void onVolumeChanged(short volume) {
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
    }

    @Override
    public void close() {
        audioLine.close();
    }
}
