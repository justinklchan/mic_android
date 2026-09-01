package com.example.microphone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    static {
        System.loadLibrary("native-lib");
    }

    /** Audible range; the tone is also capped below Nyquist for the 48 kHz rate. */
    private static final int MIN_FREQ = 20;
    private static final int MAX_FREQ = 20000;
    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 600;
    private static final int SAMPLE_RATE = 48000;
    private static final int REQUEST_MIC = 1;

    private int freq = 200;
    private int freq2 = 500;
    private boolean secondTone;
    private double vol = 0.1;
    private int length = 30;

    private Worker task;
    private CountDownTimer countdown;

    private TextView recordingIdText, statusText, labelFrequency;
    private MaterialSwitch secondToneSwitch;
    private View rowFrequency2;
    private EditText freq2Et;
    private View settingsRows;
    private TextView settingsSummary;
    private ViewGroup root;
    private boolean uiReady;
    private ImageView statusDot;
    private SharedPreferences prefs;

    /** Single-value callback so the three fields share one TextWatcher shape. */
    private interface OnValue {
        void onValue(String text);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recordingIdText = findViewById(R.id.textView1);
        statusText = findViewById(R.id.statusText);
        statusDot = findViewById(R.id.statusDot);
        labelFrequency = findViewById(R.id.labelFrequency);
        secondToneSwitch = findViewById(R.id.switchSecondTone);
        rowFrequency2 = findViewById(R.id.rowFrequency2);
        freq2Et = findViewById(R.id.editTextNumber4);
        settingsRows = findViewById(R.id.settingsRows);
        settingsSummary = findViewById(R.id.settingsSummary);
        root = findViewById(R.id.root);

        Constants.lineChart = findViewById(R.id.linechart);
        Constants.startButton = findViewById(R.id.button);
        Constants.stopButton = findViewById(R.id.button2);
        Constants.freqEt = findViewById(R.id.editTextNumber);
        Constants.volEt = findViewById(R.id.editTextNumber2);
        Constants.lengthEt = findViewById(R.id.editTextNumber3);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        loadParameters();
        styleChart();
        updateTargetMarker();
        setIdle();

        Constants.startButton.setOnClickListener(v -> startRun());
        Constants.stopButton.setOnClickListener(v -> stopRun());
        findViewById(R.id.resetView).setOnClickListener(v -> frameChartToTarget());

        requestMicrophone();
        uiReady = true;
    }

    // ---------------------------------------------------------------- params

    private void loadParameters() {
        freq = clamp(prefs.getInt("freq", 200), MIN_FREQ, MAX_FREQ);
        vol = Math.max(0, Math.min(1, prefs.getFloat("vol", 0.1f)));
        length = clamp(prefs.getInt("length", 30), MIN_LENGTH, MAX_LENGTH);

        Constants.freqEt.setText(String.valueOf(freq));
        Constants.volEt.setText(formatDecimal(vol));
        Constants.lengthEt.setText(String.valueOf(length));

        watch(Constants.freqEt, text -> {
            if (Utils.isInteger(text)) {
                int value = Integer.parseInt(text);
                if (value >= MIN_FREQ && value <= MAX_FREQ) {
                    freq = value;
                    prefs.edit().putInt("freq", freq).apply();
                    Constants.freqEt.setError(null);
                                updateTargetMarker();
                    return;
                }
            }
            Constants.freqEt.setError(getString(R.string.error_frequency, MIN_FREQ, MAX_FREQ));
        });

        freq2 = clamp(prefs.getInt("freq2", 500), MIN_FREQ, MAX_FREQ);
        freq2Et.setText(String.valueOf(freq2));
        watch(freq2Et, text -> {
            if (Utils.isInteger(text)) {
                int value = Integer.parseInt(text);
                if (value >= MIN_FREQ && value <= MAX_FREQ) {
                    freq2 = value;
                    prefs.edit().putInt("freq2", freq2).apply();
                    freq2Et.setError(null);
                    updateTargetMarker();
                    return;
                }
            }
            freq2Et.setError(getString(R.string.error_frequency, MIN_FREQ, MAX_FREQ));
        });

        secondTone = prefs.getBoolean("secondTone", false);
        secondToneSwitch.setChecked(secondTone);
        applySecondToneVisibility();
        secondToneSwitch.setOnCheckedChangeListener((button, checked) -> {
            secondTone = checked;
            prefs.edit().putBoolean("secondTone", secondTone).apply();
            applySecondToneVisibility();
            updateTargetMarker();
        });

        watch(Constants.volEt, text -> {
            if (Utils.isDouble(text)) {
                double value = Double.parseDouble(text);
                if (value >= 0 && value <= 1) {
                    vol = value;
                    prefs.edit().putFloat("vol", (float) vol).apply();
                    Constants.volEt.setError(null);
                    return;
                }
            }
            Constants.volEt.setError(getString(R.string.error_volume));
        });

        watch(Constants.lengthEt, text -> {
            if (Utils.isInteger(text)) {
                int value = Integer.parseInt(text);
                if (value >= MIN_LENGTH && value <= MAX_LENGTH) {
                    length = value;
                    prefs.edit().putInt("length", length).apply();
                    Constants.lengthEt.setError(null);
                    return;
                }
            }
            Constants.lengthEt.setError(getString(R.string.error_duration, MIN_LENGTH, MAX_LENGTH));
        });
    }

    /** The second frequency is only shown when it is actually in play. */
    private void applySecondToneVisibility() {
        rowFrequency2.setVisibility(secondTone ? View.VISIBLE : View.GONE);
        labelFrequency.setText(secondTone ? R.string.label_frequency_1 : R.string.label_frequency);
    }

    /** Frequencies to emit: one tone, or both when the second is switched on. */
    private double[] tones() {
        return secondTone ? new double[]{freq, freq2} : new double[]{freq};
    }

    private void watch(EditText field, OnValue callback) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                callback.onValue(s.toString());
            }
        });
    }

    /** Shortest exact-looking form: 0.1 stays "0.1", not "0.100" or a truncated "0.0". */
    private static String formatDecimal(double value) {
        String text = String.format(Locale.US, "%.3f", value);
        if (text.contains(".")) {
            text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return text.isEmpty() ? "0" : text;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // ------------------------------------------------------------- transport

    private void startRun() {
        if (!hasMicrophone()) {
            requestMicrophone();
            return;
        }
        closeKeyboard();

        String recordingId = String.valueOf(System.currentTimeMillis());
        recordingIdText.setText(getString(R.string.recording_id, recordingId));

        task = new Worker(this, tones(), vol, length, SAMPLE_RATE, recordingId);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        setRecording();
    }

    private void stopRun() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }
        setIdle();
    }

    /** Called by {@link Worker} when a run reaches its full duration on its own. */
    void onRunFinished() {
        task = null;
        setIdle();
    }

    private void setRecording() {
        showSettings(false);
        Constants.startButton.setEnabled(false);
        Constants.stopButton.setEnabled(true);
        setLamp(R.color.trace, getString(R.string.status_recording));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (countdown != null) {
            countdown.cancel();
        }
        countdown = new CountDownTimer(length * 1000L, 250) {
            @Override
            public void onTick(long remainingMs) {
                int seconds = (int) Math.ceil(remainingMs / 1000.0);
                statusText.setText(getString(R.string.countdown_remaining, seconds));
            }

            @Override
            public void onFinish() {
                statusText.setText(R.string.status_recording);
            }
        }.start();
    }

    private void setIdle() {
        showSettings(true);
        Constants.startButton.setEnabled(hasMicrophone());
        Constants.stopButton.setEnabled(false);
        setLamp(hasMicrophone() ? R.color.mark : R.color.mute,
                getString(hasMicrophone() ? R.string.status_ready : R.string.status_no_mic));
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (countdown != null) {
            countdown.cancel();
            countdown = null;
        }
    }

    /**
     * Hands the settings' height to the chart during a run. The values stay on
     * screen as a single summary line so you can still see what is playing.
     */
    private void showSettings(boolean expanded) {
        if (!expanded) {
            settingsSummary.setText(secondTone
                    ? getString(R.string.summary_dual, freq, freq2, formatDecimal(vol), length)
                    : getString(R.string.summary_single, freq, formatDecimal(vol), length));
        }
        if (settingsRows.getVisibility() == (expanded ? View.VISIBLE : View.GONE)) {
            return;
        }
        if (uiReady) {
            TransitionManager.beginDelayedTransition(root, new AutoTransition());
        }
        settingsRows.setVisibility(expanded ? View.VISIBLE : View.GONE);
        settingsSummary.setVisibility(expanded ? View.GONE : View.VISIBLE);
    }

    private void setLamp(int colorRes, String label) {
        statusDot.setImageTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, colorRes)));
        statusText.setText(label);
    }

    // ----------------------------------------------------------- permissions

    private boolean hasMicrophone() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMicrophone() {
        if (!hasMicrophone()) {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MIC) {
            setIdle();
            if (!hasMicrophone()) {
                Constants.lineChart.setNoDataText(getString(R.string.permission_needed));
                Constants.lineChart.invalidate();
            }
        }
    }

    // ----------------------------------------------------------------- chart

    /** One-time chart appearance. Data styling lives in {@link OfflineRecorder}. */
    private void styleChart() {
        LineChart chart = Constants.lineChart;
        int mute = ContextCompat.getColor(this, R.color.mute);
        int rule = ContextCompat.getColor(this, R.color.rule);

        chart.setNoDataText(getString(R.string.chart_empty));
        chart.setNoDataTextColor(mute);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setDrawGridBackground(false);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDoubleTapToZoomEnabled(true);
        chart.setHighlightPerTapEnabled(false);
        chart.setHighlightPerDragEnabled(false);
        chart.setExtraBottomOffset(4f);
        chart.setMinOffset(12f);

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawAxisLine(false);
        x.setGridColor(rule);
        x.enableGridDashedLine(3f, 5f, 0f);
        x.setTextColor(mute);
        x.setTypeface(Typeface.MONOSPACE);
        x.setTextSize(9f);
        x.setLabelCount(5, false);
        x.setDrawLimitLinesBehindData(true);
        x.setAxisMinimum(0f);
        x.setAxisMaximum(SAMPLE_RATE / 2f);
        x.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf(Math.round(value));
            }
        });

        YAxis left = chart.getAxisLeft();
        left.setDrawAxisLine(false);
        left.setGridColor(rule);
        left.enableGridDashedLine(3f, 5f, 0f);
        left.setTextColor(mute);
        left.setTypeface(Typeface.MONOSPACE);
        left.setTextSize(9f);
        left.setLabelCount(5, false);
        left.setAxisMinimum(0f);
        left.setAxisMaximum(160f);
        chart.getAxisRight().setEnabled(false);

        chart.invalidate();
    }

    /** A blue marker per emitted tone, so you can see where each peak should land. */
    private void updateTargetMarker() {
        XAxis x = Constants.lineChart.getXAxis();
        x.removeAllLimitLines();

        if (secondTone) {
            // Stagger the labels vertically; close-together tones would otherwise collide.
            x.addLimitLine(marker(freq, getString(R.string.chart_marker_f1),
                    LimitLine.LimitLabelPosition.RIGHT_TOP));
            x.addLimitLine(marker(freq2, getString(R.string.chart_marker_f2),
                    LimitLine.LimitLabelPosition.RIGHT_BOTTOM));
        } else {
            x.addLimitLine(marker(freq, getString(R.string.chart_marker_target),
                    LimitLine.LimitLabelPosition.RIGHT_TOP));
        }
        Constants.lineChart.invalidate();
    }

    private LimitLine marker(float atHz, String label, LimitLine.LimitLabelPosition at) {
        int mark = ContextCompat.getColor(this, R.color.mark);
        LimitLine line = new LimitLine(atHz, label);
        line.setLineColor(mark);
        line.setLineWidth(1.2f);
        line.enableDashedLine(5f, 4f, 0f);
        line.setTextColor(mark);
        line.setTypeface(Typeface.MONOSPACE);
        line.setTextSize(9f);
        line.setLabelPosition(at);
        return line;
    }

    /**
     * Zooms to a window around the target frequency. The zoom cap is lifted again
     * straight after, so this frames the view without limiting how far you can
     * zoom back out by hand.
     */
    void frameChartToTarget() {
        LineChart chart = Constants.lineChart;
        if (chart == null || chart.getData() == null) {
            return;
        }
        float low = secondTone ? Math.min(freq, freq2) : freq;
        float high = secondTone ? Math.max(freq, freq2) : freq;
        float span = Math.max(4000f, (high - low) * 1.6f);

        chart.fitScreen();
        chart.setVisibleXRangeMaximum(span);
        chart.moveViewToX(Math.max((low + high) / 2f - span / 2f, 0f));
        chart.setVisibleXRangeMaximum(SAMPLE_RATE / 2f);
    }

    // ----------------------------------------------------------- lifecycle

    public void closeKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countdown != null) {
            countdown.cancel();
            countdown = null;
        }
        if (task != null) {
            task.cancel(true);
            task = null;
        }
        // Constants holds these statically; drop them so the Activity can be collected.
        Constants.release();
    }
}
