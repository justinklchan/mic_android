package com.example.microphone;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class FileOperations {
    public static void writeToDisk(Context cxt, String fname) {
        Log.e("asdf","file io");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String dir = cxt.getExternalFilesDir(null).toString();
                    File path = new File(dir);
                    if (!path.exists()) {
                        path.mkdirs();
                    }
                    File file = new File(dir, fname+".txt");
                    BufferedWriter outfile = new BufferedWriter(new FileWriter(file,false));
                    for (Short s : Constants.samples) {
                        outfile.append(s+"");
                        outfile.newLine();
                    }
                    outfile.flush();
                    outfile.close();
                } catch(Exception e) {
                    Log.e("ex", "writeRecToDisk");
                    Log.e("ex", e.getMessage());
                }
            }
        }).run();
    }
}
