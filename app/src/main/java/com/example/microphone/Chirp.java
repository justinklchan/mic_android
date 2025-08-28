package com.example.microphone;
public class Chirp {
    public static short[] generateChirpSpeaker(double startFreq, double endFreq, double time, double fs, double initialPhase, double gap) {

        int N = (int) (time * fs);
        short[] ans = new short[(int)(N+(gap*fs))];
        double f = startFreq;
        double k = (endFreq - startFreq) / time;
        for (int i = 0; i < N; i++) {
            double t = (double) i / fs;
            double phase = initialPhase + 2*Math.PI*(startFreq * t + 0.5 * k * t * t);
            phase = Normalize(phase);
            ans[i] = (short) (Math.sin(phase) * 32000);
        }

        return ans;
    }

    public static double Normalize(double ang) {

        double angle = ang;
        while (angle < 0) {
            angle += 2 * Math.PI;
        }
        while (angle >= 2 * Math.PI) {
            angle -= 2 * Math.PI;
        }

        return angle;
    }
}