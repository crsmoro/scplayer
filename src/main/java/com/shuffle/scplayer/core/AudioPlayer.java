package com.shuffle.scplayer.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    private boolean stopPlayThread = false;
    final Lock lock = new ReentrantLock();
    final Condition waiting  = lock.newCondition();
    private boolean isWaiting = false;
    private AtomicBoolean play = new AtomicBoolean(true);

    public AudioPlayer(SpotifyConnectPlayer spotifyConnectPlayer) throws LineUnavailableException, IOException {
        this.spotifyConnectPlayer = spotifyConnectPlayer;
        //audioLine = (SourceDataLine) AudioSystem.getLine(info);
        //audioLine.open(pcm);

        input = new PipedInputStream(4096);
        output = new PipedOutputStream(input);

        Thread playing = new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    int framesize = AudioSystem.NOT_SPECIFIED;
                    byte[] buffer = new byte[4096];
                    int numbytes = 0;

                    boolean started = false;
                    while (!stopPlayThread) {
                        if (!play.get()) {
                            playThreadWait();
                        }

                        int bytesread = input.read(buffer, numbytes, buffer.length - numbytes);

                        if (bytesread == -1) {
                            playThreadWait();
                        }

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
                } catch (Exception e) {
                    log.error("Playing thread error", e);
                }

            }
        });
        playing.start();

    }

    public void playThreadWait() {
        lock.lock();
        try {
            isWaiting = true;
            waiting.await();
            isWaiting = false;
        } catch (InterruptedException ignored) {
        } finally {
            isWaiting = false;
            lock.unlock();
        }
    }

    public void playThreadSignal() {
        lock.lock();
        try {
            waiting.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onActive() {
        try {
            audioLine = (SourceDataLine) AudioSystem.getLine(info);
            audioLine.open(pcm);
        } catch (LineUnavailableException e) {
            log.error("onActive error", e);
        }
        log.error("onActive called");
    }

    @Override
    public void onInactive() {
        audioLine.flush();
        audioLine.close();
        log.error("onInactive called");
    }

    @Override
    public void onPlay() {
        if (play.compareAndSet(false, true)) {
            playThreadSignal();
        }
        log.error("onPlay called");
    }

    @Override
    public void onPause() {
        play.compareAndSet(true, false);
        log.error("onPause called");
    }

    @Override
    public void onAudioFlush() {
        audioLine.flush();
        try {
            input.skip(input.available());
        } catch (IOException e) {
            log.error("error while trying to clear buffer", e);
        }
        log.error("onAudioFlush called");
    }

    @Override
    public void onTokenLost() {
        onInactive();
        log.error("onTokenLost called");
    }

    @Override
    public void onAudioData(byte[] data) {
        if (isWaiting)
            playThreadSignal();
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

        log.error("volume called");
    }

    @Override
    public void close() {
        stopPlayThread = true;
        audioLine.close();
        log.error("close called");
    }
}
