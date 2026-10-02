package com.absorbgame.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Lightweight audio manager that synthesizes short sound effects as PCM
 * sine-wave buffers (no external audio files needed). Can be swapped for
 * real asset playback later.
 */
public class GameAudio {
    public static final int SFX_EAT = 0;
    public static final int SFX_ABSORB = 1;
    public static final int SFX_DEATH = 2;
    public static final int SFX_UI = 3;
    public static final int SFX_PAUSE = 4;

    private final short[][] buffers = new short[5][];
    private boolean sfxEnabled = true;
    private boolean musicEnabled = true;
    private AudioTrack bgmTrack;
    private Thread bgmThread;
    private volatile boolean bgmRunning = false;

    public GameAudio() {
        buffers[SFX_EAT]    = genTone(660f, 0.08f, 0.3f);
        buffers[SFX_ABSORB] = genTone(440f, 0.18f, 0.5f);
        buffers[SFX_DEATH]  = genTone(110f, 0.6f, 0.6f);
        buffers[SFX_UI]     = genTone(880f, 0.05f, 0.2f);
        buffers[SFX_PAUSE]  = genTone(330f, 0.12f, 0.3f);
    }

    public void setSfxEnabled(boolean v) { sfxEnabled = v; }
    public void setMusicEnabled(boolean v) {
        musicEnabled = v;
        if (!v) stopBgm();
    }

    public boolean isSfxEnabled() { return sfxEnabled; }
    public boolean isMusicEnabled() { return musicEnabled; }

    public void playSfx(int which) {
        if (!sfxEnabled) return;
        short[] buf = buffers[which];
        if (buf == null) return;
        new Thread(() -> {
            try {
                AudioTrack t = new AudioTrack(AudioManager.STREAM_MUSIC,
                        44100, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        buf.length * 2, AudioTrack.MODE_STATIC);
                t.write(buf, 0, buf.length);
                t.play();
                Thread.sleep((long) (buf.length / 44.1f));
                t.stop();
                t.release();
            } catch (Exception ignored) {}
        }).start();
    }

    /** Starts a simple ambient BGM loop (low volume). */
    public void startBgm() {
        if (!musicEnabled || bgmRunning) return;
        bgmRunning = true;
        bgmThread = new Thread(() -> {
            int sr = 22050;
            int bufSize = AudioTrack.getMinBufferSize(sr,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            bgmTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sr,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    Math.max(bufSize, 4096), AudioTrack.MODE_STREAM);
            bgmTrack.play();
            // simple pentatonic loop
            float[] notes = {261.63f, 329.63f, 392f, 523.25f, 392f, 329.63f};
            int noteIdx = 0;
            short[] chunk = new short[1024];
            double phase = 0;
            while (bgmRunning) {
                float freq = notes[noteIdx % notes.length];
                for (int i = 0; i < chunk.length; i++) {
                    phase += 2 * Math.PI * freq / sr;
                    // soft envelope per note
                    double env = 0.15 * (0.5 + 0.5 * Math.sin(phase * 0.01));
                    chunk[i] = (short) (Math.sin(phase) * Short.MAX_VALUE * env * 0.25);
                }
                bgmTrack.write(chunk, 0, chunk.length);
                if (Math.random() < 0.02) noteIdx++;
            }
            try { bgmTrack.stop(); bgmTrack.release(); } catch (Exception ignored) {}
        });
        bgmThread.start();
    }

    public void stopBgm() {
        bgmRunning = false;
        if (bgmThread != null) {
            try { bgmThread.join(300); } catch (Exception ignored) {}
        }
    }

    static short[] genTone(float freq, float dur, float vol) {
        int sr = 44100;
        int n = (int) (sr * dur);
        short[] buf = new short[n];
        for (int i = 0; i < n; i++) {
            double t = (double) i / sr;
            double env = Math.exp(-t * 6.0) * (1.0 - Math.exp(-t * 80.0));
            double s = Math.sin(2 * Math.PI * freq * t) * env * vol;
            buf[i] = (short) (s * Short.MAX_VALUE);
        }
        return buf;
    }
}
