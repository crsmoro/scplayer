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
    private final SpotifyConnectPlayer player;

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
    private boolean isMuted = false;

    public AudioPlayer(final SpotifyConnectPlayer player) throws LineUnavailableException, IOException {
        this.player = player;

        Thread playing = new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    int framesize = AudioSystem.NOT_SPECIFIED;
                    byte[] buffer = new byte[4096];
                    int numbytes = 0;

                    boolean started = false;
                    while (!stopPlayThread) {
                        try {
                            if (!play.get() || input == null) {
                                playThreadWait();
                                continue;
                            }

                            int bytesread = input.read(buffer, numbytes, buffer.length - numbytes);

                            if (bytesread == -1) {
                                playThreadWait();
                                continue;
                            }

                            numbytes += bytesread;

                            if (!audioLine.isOpen()) {
                                numbytes = 0;
                                if (!player.isActive())
                                    onInactive();
                                continue;
                            }

                            if (!audioLine.isRunning()) {
                                audioLine.start();
                                started = true;
                            }

                            int bytestowrite = numbytes / framesize * framesize;

                            audioLine.write(buffer, 0, bytestowrite);

                            int remaining = numbytes - bytestowrite;
                            if (remaining > 0)
                                System.arraycopy(buffer, bytestowrite, buffer, 0, remaining);
                            numbytes = remaining;
                        } catch (Exception e) {
                            log.error("Playing thread error", e);
                        }
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

    private void playThreadWait() {
        try {
            lock.lock();
            isWaiting = true;
            waiting.await();
            isWaiting = false;
        } catch (InterruptedException ignored) {
        } finally {
            isWaiting = false;
            lock.unlock();
        }
    }

    private void playThreadSignal() {
        try {
            lock.lock();
            waiting.signalAll();
        } finally {
            lock.unlock();
        }
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
        try {
            audioLine = (SourceDataLine) AudioSystem.getLine(info);
            audioLine.open(pcm);
            onVolumeChanged(player.getVolume());
            if (isMuted && audioLine.isControlSupported(BooleanControl.Type.MUTE))
                ((BooleanControl) audioLine.getControl(BooleanControl.Type.MUTE)).setValue(true);
            input = new PipedInputStream(1048576);
            output = new PipedOutputStream(input);
        } catch (LineUnavailableException | IOException e) {
            log.error("onActive error", e);
        }
    }

    @Override
    public void onInactive() {
        audioLine.flush();
        audioLine.close();
        try {
            output.close();
        } catch (IOException e) {
            log.error("unable to close output", e);
        }
        try {
            PipedInputStream input = this.input;
            this.input = null;
            input.close();
        } catch (IOException e) {
            log.error("unable to input output", e);
        }
    }

    @Override
    public void onPlay() {
        if (play.compareAndSet(false, true)) {
            playThreadSignal();
        }
    }

    @Override
    public void onPause() {
        play.compareAndSet(true, false);
    }

    @Override
    public void onAudioFlush() {
        if (audioLine != null)
            audioLine.flush();
        try {
            if (input != null)
                input.skip(input.available());
        } catch (IOException e) {
            log.error("error while trying to clear buffer", e);
        }
    }

    @Override
    public void onTokenLost() {
        onInactive();
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
        stopPlayThread = true;
        if (audioLine != null)
            audioLine.close();
        try {
            if (output != null)
                output.close();
        } catch (IOException e) {
            log.error(e);
        }
        try {
            if (input != null)
                input.close();
        } catch (IOException e) {
            log.error(e);
        }
    }
}
