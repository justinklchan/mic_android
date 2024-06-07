package com.example.microphone;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

public class Worker  extends AsyncTask<Void, Void, Void> {
    int freq;
    double vol;
    Context context;
    int fs;
    int length;
    String fname;

    public Worker(Context context, int freq, double vol, int length, int fs, String fname) {
        this.freq = freq;
        this.vol=vol;
        this.length = length;
        this.context = context;
        this.fs = fs;
        this.fname = fname;
    }

    @Override
    protected void onPostExecute(Void unused) {
        super.onPostExecute(unused);
        Constants.startButton.setEnabled(true);
        Constants.stopButton.setEnabled(false);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        short[] tone = Tone.generateTone(freq,1,fs);

        OfflineRecorder rec = new OfflineRecorder(MediaRecorder.AudioSource.DEFAULT,fs,fs*length, context, fname, freq);
        rec.start();

        AudioSpeaker speaker = new AudioSpeaker(context, tone, fs);
        speaker.play(vol,-1);

        Log.e("asdf","start");
        try {
            Thread.sleep(length*1000);
        }
        catch(Exception e){
            Log.e("asdf","Asdf");
        }
        speaker.track1.stop();
        rec.halt();
        Log.e("asdf","stop");
        return null;
    }
}
