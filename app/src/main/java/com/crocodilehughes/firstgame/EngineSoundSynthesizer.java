package com.crocodilehughes.firstgame;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class EngineSoundSynthesizer {

    private static final int SAMPLE_RATE = 44100;
    private static final String TAG = "EngineSoundSynthesizer";
    private boolean isRunning = false;
    private Thread audioThread;
    private AudioTrack audioTrack;

    public void start() {
        if (isRunning)
            return;
        isRunning = true;

        int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
            // Fallback buffer size if min buffer calculation fails
            bufferSize = SAMPLE_RATE * 2;
        }

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM);

        audioTrack.play();

        audioThread = new Thread(new Runnable() {
            @Override
            public void run() {
                generateTone();
            }
        });
        audioThread.start();
    }

    public void stop() {
        isRunning = false;
        if (audioThread != null) {
            try {
                audioThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            audioThread = null;
        }
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }

    private volatile double frequency = 220.0; // Base frequency
    private volatile boolean isMuted = false;

    public void setFrequency(double newFrequency) {
        this.frequency = newFrequency;
    }

    public void setMuted(boolean muted) {
        this.isMuted = muted;
    }

    private void generateTone() {
        double currentFrequency;
        double increment;
        double angle = 0;

        // Asymmetric wave params to make it sound buzzy like an engine
        // We'll add some harmonics or modulation to make it sound "fast"

        // A buffer to write chunks of audio
        int refillSize = 1024;
        short[] buffer = new short[refillSize];

        while (isRunning) {
            if (isMuted) {
                // Determine sleep time based on buffer size and sample rate to avoid busy loop
                // 1024 samples / 44100 Hz = ~23ms
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // Write silence to keep track running smoothly if needed,
                // or just skip writing (but AudioTrack might underrun).
                // Writing silence is safer.
                for (int i = 0; i < refillSize; i++) {
                    buffer[i] = 0;
                }
                if (audioTrack != null) {
                    audioTrack.write(buffer, 0, refillSize);
                }
                continue;
            }

            currentFrequency = frequency;
            increment = 2.0 * Math.PI * currentFrequency / SAMPLE_RATE;

            for (int i = 0; i < refillSize; i++) {
                // Modulate frequency slightly to give it a "running" texture, maybe LFO
                // But for now, a simple buzzy sawtooth/pulse mix

                // Sawtoothish
                double sampleValue = 0;

                // Fundamental
                sampleValue += Math.sin(angle);
                // 2nd Harmonic (Octave)
                sampleValue += 0.5 * Math.sin(angle * 2);
                // 3rd Harmonic (Fifth)
                sampleValue += 0.25 * Math.sin(angle * 3);

                // Make it a bit more like a square/pulse for that 8-bit feel
                if (sampleValue > 0.8)
                    sampleValue = 1.0;
                if (sampleValue < -0.8)
                    sampleValue = -1.0;

                // Scale to 16-bit PCM
                buffer[i] = (short) (sampleValue * Short.MAX_VALUE * 0.3); // 0.3 volume

                angle += increment;
                if (angle > 2.0 * Math.PI) {
                    angle -= 2.0 * Math.PI;
                }
            }

            if (audioTrack != null) {
                audioTrack.write(buffer, 0, refillSize);
            }
        }
    }

    public void playSadMelody() {
        stop(); // Stop the engine sound first

        new Thread(new Runnable() {
            @Override
            public void run() {
                // Frequencies for a descending minor scale (approximate)
                double[] melodyFreqs = { 440.0, 392.0, 349.23, 329.63, 293.66, 261.63, 246.94, 220.0 };
                int durationMs = 300;

                int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);

                AudioTrack melodyTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM);

                melodyTrack.play();

                for (int i = 0; i < melodyFreqs.length; i++) {
                    // Check if muted inside the loop
                    if (isMuted) {
                        // Play valid silence to keep timing or just sleep
                        // Sleeping is easier to keep the "sadness" timing even if silent
                        int currentNoteDuration = (i == melodyFreqs.length - 1) ? durationMs * 2 : durationMs;
                        try {
                            Thread.sleep(currentNoteDuration + 50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }

                    double freq = melodyFreqs[i];
                    double increment = 2.0 * Math.PI * freq / SAMPLE_RATE;
                    double angle = 0;

                    // Double the duration for the last note
                    int currentNoteDuration = (i == melodyFreqs.length - 1) ? durationMs * 2 : durationMs;

                    int numSamples = (int) (SAMPLE_RATE * (currentNoteDuration / 1000.0));
                    short[] buffer = new short[numSamples];

                    for (int j = 0; j < numSamples; j++) {
                        // Simple Sine wave for melody, maybe a bit of square for 8-bit feel
                        double sample = Math.sin(angle);
                        // Clip it slightly for "retro" feel
                        if (sample > 0.8)
                            sample = 0.8;
                        if (sample < -0.8)
                            sample = -0.8;

                        buffer[j] = (short) (sample * Short.MAX_VALUE * 0.5);
                        angle += increment;
                    }

                    melodyTrack.write(buffer, 0, numSamples);

                    // Small pause between notes
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                melodyTrack.stop();
                melodyTrack.release();
            }
        }).start();
    }

    public void playHealingTheme() {
        stop();
        isRunning = true;

        // Similar to start(), we need an audio track
        int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM);

        audioTrack.play();

        audioThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Approximate "Healing" arpeggios
                // Chords: C Major -> G Major -> F Major -> G Major
                // Freqs: C4=261.6, E4=329.6, G4=392.0
                // G3=196.0, B3=246.9, D4=293.7
                // F3=174.6, A3=220.0, C4=261.6

                double[][] chords = {
                        { 261.6, 329.6, 392.0 }, // C Maj
                        { 196.0, 246.9, 293.7 }, // G Maj
                        { 174.6, 220.0, 261.6 }, // F Maj
                        { 196.0, 246.9, 293.7 } // G Maj
                };

                int noteDurationMs = 120; // Fast arpeggio

                while (isRunning) {
                    for (double[] chord : chords) {
                        // Arpeggiate up
                        for (double freq : chord) {
                            if (!isRunning)
                                return;
                            playNote(freq, noteDurationMs);
                        }
                        // And down? Or just repeat high note? Let's minimalize: Up arpeggio x2
                        for (double freq : chord) {
                            if (!isRunning)
                                return;
                            playNote(freq, noteDurationMs);
                        }
                    }
                    // Loop forever
                }
            }

            private void playNote(double freq, int ms) {
                if (isMuted) {
                    try {
                        Thread.sleep(ms);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                double increment = 2.0 * Math.PI * freq / SAMPLE_RATE;
                double angle = 0;
                int numSamples = (int) (SAMPLE_RATE * (ms / 1000.0));
                short[] buffer = new short[numSamples];

                for (int i = 0; i < numSamples; i++) {
                    // Soft sine wave
                    double val = Math.sin(angle);
                    buffer[i] = (short) (val * Short.MAX_VALUE * 0.4);
                    angle += increment;
                }
                if (audioTrack != null) {
                    audioTrack.write(buffer, 0, numSamples);
                }
            }
        });
        audioThread.start();
    }
}
