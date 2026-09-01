package com.example.microphone;

import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

public class Worker extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "Worker";

    private final MainActivity activity;
    private final double[] freqs;
    private final double vol;
    private final int length;
    private final int fs;
    private final String fname;

    public Worker(MainActivity activity, double[] freqs, double vol, int length, int fs, String fname) {
        this.activity = activity;
        this.freqs = freqs;
        this.vol = vol;
        this.length = length;
        this.fs = fs;
        this.fname = fname;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        short[] tone = Tone.generateTone(freqs, 1, fs);

        OfflineRecorder rec = new OfflineRecorder(
                MediaRecorder.AudioSource.DEFAULT, fs, fs * length, activity, fname);
        rec.start();

        AudioSpeaker speaker = new AudioSpeaker(activity, tone, fs);
        speaker.play(vol, -1);

        try {
            Thread.sleep(length * 1000L);
        } catch (InterruptedException e) {
            Log.i(TAG, "run cancelled before its full duration");
        }

        speaker.halt();
        rec.halt();
        return null;
    }

    @Override
    protected void onPostExecute(Void unused) {
        super.onPostExecute(unused);
        activity.onRunFinished();
    }

    @Override
    protected void onCancelled(Void unused) {
        super.onCancelled(unused);
        activity.onRunFinished();
    }
}
