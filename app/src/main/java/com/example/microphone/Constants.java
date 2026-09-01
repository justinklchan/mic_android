package com.example.microphone;

import android.widget.Button;
import android.widget.EditText;

import com.github.mikephil.charting.charts.LineChart;

public class Constants {
    static Button startButton, stopButton;
    static EditText freqEt, volEt, lengthEt;
    static LineChart lineChart;
    static short[] samples;
    static short[] temp;

    /** These are static View references; clear them when the Activity goes away. */
    static void release() {
        startButton = null;
        stopButton = null;
        freqEt = null;
        volEt = null;
        lengthEt = null;
        lineChart = null;
    }
}
