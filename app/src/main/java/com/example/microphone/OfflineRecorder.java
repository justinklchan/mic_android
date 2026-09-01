package com.example.microphone;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;

public class OfflineRecorder extends Thread {

    AudioRecord rec;
    int minbuffersize;
    boolean recording;
    int count;
    Context context;
    String filename;
    int fs;
    private boolean framedView;
    private long lastChartUpdate;

    /** The spectrum only needs to look live; redrawing per audio buffer swamps the UI thread. */
    private static final long CHART_INTERVAL_MS = 100;

    public OfflineRecorder(int microphone, int fs, int bufferLen, Context context, String filename) {
        this.context = context;
        this.filename = filename;
        this.fs = fs;

        minbuffersize = AudioRecord.getMinBufferSize(
                fs,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        rec = new AudioRecord(
                microphone,
                fs,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minbuffersize);
        Constants.temp = new short[minbuffersize];
        Constants.samples = new short[bufferLen];
    }

    public void run() {
//        Log.e("asdf","run");
        int bytesread;

        rec.startRecording();
        recording=true;
        while(recording) {
            bytesread = rec.read(Constants.temp, 0, minbuffersize);

            process();

//            Log.e("asdf","counter "+count+","+Constants.samples.length+","+minbuffersize);
            for (int i = 0; i < bytesread; i++) {
                if (count >= Constants.samples.length) {
                    recording = false;
                    FileOperations.writeToDisk(context,filename);
                    break;
                } else {
                    Constants.samples[count] = Constants.temp[i];
                    count += 1;
                }
            }
        }
    }

    public void process() {
        // Recording is unaffected by this; only the on-screen refresh rate is capped.
        long now = SystemClock.uptimeMillis();
        if (now - lastChartUpdate < CHART_INTERVAL_MS) {
            return;
        }
        lastChartUpdate = now;

        double[] out = fftnative_short(Constants.temp, Constants.temp.length);

        // Build the series off the UI thread, then hand the finished data over.
        float freqSpacing = (float) fs / out.length;
        List<Entry> lineData = new ArrayList<>(out.length);
        for (int i = 0; i < out.length; i++) {
            lineData.add(new Entry(i * freqSpacing, (float) out[i]));
        }

        if (!(context instanceof Activity)) {
            return;
        }
        ((Activity) context).runOnUiThread(() -> {
            LineChart chart = Constants.lineChart;
            if (chart == null) {
                return;
            }

            LineDataSet series = new LineDataSet(lineData, "");
            series.setDrawCircles(false);
            series.setDrawValues(false);
            series.setMode(LineDataSet.Mode.LINEAR);
            series.setLineWidth(1.4f);
            series.setColor(ContextCompat.getColor(context, R.color.trace));
            series.setDrawFilled(true);
            series.setFillColor(ContextCompat.getColor(context, R.color.trace));
            series.setFillAlpha(40);

            List<ILineDataSet> data = new ArrayList<>();
            data.add(series);
            chart.setData(new LineData(data));
            chart.notifyDataSetChanged();

            // Frame the target once per run; after that the view is the user's to pan.
            if (!framedView) {
                framedView = true;
                ((MainActivity) context).frameChartToTarget();
            }
            chart.invalidate();
        });
    }

    public void halt() {
        if (rec.getState() == AudioRecord.STATE_INITIALIZED||
                rec.getState() == AudioRecord.RECORDSTATE_RECORDING) {
            rec.stop();
        }
        rec.release();
        recording = false;
        FileOperations.writeToDisk(context,filename);
    }

    public static native double[] fftnative_short(short[] data, int N);

}