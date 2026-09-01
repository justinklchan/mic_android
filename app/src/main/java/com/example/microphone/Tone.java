package com.example.microphone;

public class Tone {

    /**
     * Sums one or more sine tones into a single buffer.
     *
     * <p>Amplitude is divided across the tones so a mix can never clip. At whole-Hz
     * frequencies a one-second buffer holds a whole number of cycles of every tone,
     * so it also loops seamlessly.
     *
     * @param freqs frequencies in Hz
     * @param time  buffer length in seconds
     * @param fs    sample rate in Hz
     */
    public static short[] generateTone(double[] freqs, double time, double fs) {
        int n = (int) (time * fs);
        short[] ans = new short[n];
        double scale = 32767.0 / freqs.length;

        for (int i = 0; i < n; i++) {
            double t = i / fs;
            double sum = 0;
            for (double freq : freqs) {
                sum += Math.sin(2 * Math.PI * freq * t);
            }
            ans[i] = (short) (sum * scale);
        }
        return ans;
    }
}
