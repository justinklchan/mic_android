package com.example.microphone;

import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import com.github.mikephil.charting.charts.LineChart;

public class Constants {
    static Button startButton, stopButton;
    static RadioButton b1, b2;
    static EditText freqEt,freqEt2,volEt,gapTimeEt,chirpTimeEt,lengthEt;
    static LineChart lineChart;
    static short[] samples;
    static short[] temp;
}
