package com.example.microphone;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

public class Worker  extends AsyncTask<Void, Void, Void> {
    int freq, freq2;
    double vol;
    int signalType;
    Context context;
    int fs;
    double chirpTime,gapTime;
    int length;
    String fname;

    public Worker(Context context, int freq, int freq2, int signalType, double chirpTime,double gapTime, double vol, int length, int fs, String fname) {
        this.freq = freq;
        this.freq2 = freq2;
        this.signalType = signalType;
        this.vol=vol;
        this.chirpTime = chirpTime;
        this.gapTime=gapTime;
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

        OfflineRecorder rec = new OfflineRecorder(MediaRecorder.AudioSource.DEFAULT,fs,fs*length, context, fname, freq);
        rec.start();

        AudioSpeaker speaker=null;

        if (signalType==0) {
            short[] tone = Tone.generateTone(freq,1,fs);
            speaker=new AudioSpeaker(context, tone, fs);
        }
        else if (signalType==1) {
            short[] tone = Chirp.generateChirpSpeaker(freq,freq2,chirpTime,fs,0,gapTime);
            speaker=new AudioSpeaker(context, tone, fs);
        }
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
