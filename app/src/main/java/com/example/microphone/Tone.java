package com.example.microphone;

public class Tone {
    // time in seconds
    public static short[] generateTone(double freq, double time, double fs) {
        int N = (int) (time * fs);
        short[] ans = new short[N];
        for (int i = 0; i < N; i++) {
            double t = (double) i / fs;
            ans[i] = (short) (Math.sin(2 * Math.PI * freq * t) * 32767);
        }

        return ans;
    }
}
