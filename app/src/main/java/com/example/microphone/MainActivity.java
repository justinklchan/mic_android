package com.example.microphone;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class MainActivity extends AppCompatActivity {
    static {
        System.loadLibrary("native-lib");
    }

    LineChart lineChart;
    private int grantResults[];
    int freq=0;
    double vol=0;
    int length=0;
    Worker task;
    Activity av;
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO},1);
        onRequestPermissionsResult(1,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO},grantResults);

        av = this;
        tv = (TextView)findViewById(R.id.textView1);

        Constants.lineChart = (LineChart)findViewById(R.id.linechart);
        Constants.startButton = (Button)findViewById(R.id.button);
        Constants.stopButton = (Button)findViewById(R.id.button2);
        Constants.startButton.setEnabled(true);
        Constants.stopButton.setEnabled(false);
        Constants.startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String fname=System.currentTimeMillis()+"";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv.setText(fname);
                    }
                });

                closeKeyboard();
                task = new Worker(av,freq,vol,length, 48000,fname);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                Constants.startButton.setEnabled(false);
                Constants.stopButton.setEnabled(true);
            }
        });
        Constants.stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.cancel(true);
                Constants.startButton.setEnabled(true);
                Constants.stopButton.setEnabled(false);
            }
        });

        Constants.freqEt = (EditText)findViewById(R.id.editTextNumber);
        Constants.volEt = (EditText)findViewById(R.id.editTextNumber2);
        Constants.lengthEt = (EditText)findViewById(R.id.editTextNumber3);

        Context c= this;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        freq=prefs.getInt("freq",200);
        Constants.freqEt.setText(freq+"");
        Constants.freqEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence cs, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(c).edit();
                String s = Constants.freqEt.getText().toString();
                if (Utils.isInteger(s)) {
                    freq=Integer.parseInt(s);
                    editor.putInt("freq", freq);
                    editor.commit();
                }
            }
        });
        vol=prefs.getFloat("vol", 0.1f);
        Constants.volEt.setText(vol+"");
        Constants.volEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence cs, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(c).edit();
                String s = Constants.volEt.getText().toString();
                if (Utils.isDouble(s)) {
                    vol=Double.parseDouble(s);
                    editor.putFloat("vol", (float)vol);
                    editor.commit();
                }
            }
        });
        length=prefs.getInt("length",30);
        Constants.lengthEt.setText(length+"");
        Constants.lengthEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence cs, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(c).edit();
                String s = Constants.lengthEt.getText().toString();
                if (Utils.isInteger(s)) {
                    length=Integer.parseInt(s);
                    editor.putInt("length", length);
                    editor.commit();
                }
            }
        });

    }

    public void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}