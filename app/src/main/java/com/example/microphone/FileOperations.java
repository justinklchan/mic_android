package com.example.microphone;

import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class FileOperations {
    private static final String TAG = "FileOperations";

    /**
     * Writes the captured samples as one value per line. Runs on the caller's
     * thread and is only ever called from the recording thread, never the UI one.
     */
    public static void writeToDisk(Context cxt, String fname) {
        File dir = cxt.getExternalFilesDir(null);
        if (dir == null) {
            Log.e(TAG, "no external files directory available");
            return;
        }
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e(TAG, "could not create " + dir);
            return;
        }

        File file = new File(dir, fname + ".txt");
        try (BufferedWriter outfile = new BufferedWriter(new FileWriter(file, false))) {
            for (short s : Constants.samples) {
                outfile.append(String.valueOf(s));
                outfile.newLine();
            }
            outfile.flush();
        } catch (Exception e) {
            Log.e(TAG, "writing " + file, e);
        }
    }
}
