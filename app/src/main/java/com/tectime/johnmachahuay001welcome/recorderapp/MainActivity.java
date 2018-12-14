package com.tectime.johnmachahuay001welcome.recorderapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private Button btnRecord, btnStop, btnPlay, btnList;
    private MediaRecorder audioRecorder;
    private MediaPlayer mediaPlayer;
    private String outputFile;
    private int RECORD_AUDIO_REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getPermissionToRecordAudio();
        }

        btnRecord = (Button) findViewById(R.id.btnRecord);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnPlay = (Button) findViewById(R.id.btnPlay);
        btnList = (Button) findViewById(R.id.btnList);

        btnStop.setEnabled(false);
        btnPlay.setEnabled(false);

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initializeMediaRecord();
                try {
                    audioRecorder.prepare();
                    audioRecorder.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                btnRecord.setEnabled(false);
                btnPlay.setEnabled(false);
                btnStop.setEnabled(true);

                Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_SHORT).show();
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (audioRecorder != null) {
                    audioRecorder.stop();
                    audioRecorder.release();
                    audioRecorder = null;
                }
                btnPlay.setEnabled(true);
                btnStop.setEnabled(false);

                Toast.makeText(getApplicationContext(), "Audio recorder successfully", Toast.LENGTH_SHORT).show();
            }
        });
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(outputFile);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    Toast.makeText(getApplicationContext(), "Playing audio", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        btnRecord.setEnabled(true);
                    }
                });
            }
        });

        btnList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File root = Environment.getExternalStorageDirectory();
                String path = root.getAbsolutePath() + "/RecordApp/Audios";
                File directory = new File(path);
                File[] files = directory.listFiles();
                Toast.makeText(getApplicationContext(), "Files: " + files.length, Toast.LENGTH_SHORT).show();
                // Write a message to the database
                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                DatabaseReference namesRef = rootRef.child("audios").push();
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        String fileName = files[i].getName();
                        String fileExtension = files[i].getAbsolutePath();
                        String recordingUri = root.getAbsolutePath() + "/RecordApp/Audios/" + fileName;
                        Map<String, Object> map = new HashMap<>();
                        map.put("Filename", fileName);
                        map.put("FileUri", recordingUri);
                        namesRef.updateChildren(map);
                    }
                }
            }
        });
    }

    private void initializeMediaRecord() {
        audioRecorder = new MediaRecorder();
        audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

        File root = Environment.getExternalStorageDirectory();
        File file = new File(root.getAbsolutePath() + "/RecordApp/Audios");
        if (!file.exists()) {
            file.mkdirs();
        }
        outputFile = root.getAbsolutePath() + "/RecordApp/Audios/" + String.valueOf(System.currentTimeMillis() + ".mp3");
        Log.d("filename", outputFile);

        audioRecorder.setOutputFile(outputFile);
        audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void getPermissionToRecordAudio() {
        // 1) Use the support library version ContextCompat.checkSelfPermission(...) to avoid
        // checking the build version since Context.checkSelfPermission(...) is only available
        // in Marshmallow
        // 2) Always check for permission (even if permission has already been granted)
        // since the user can revoke permissions at any time through Settings
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // The permission is NOT already granted.
            // Check if the user has been asked about this permission already and denied
            // it. If so, we want to give more explanation about why the permission is needed.
            // Fire off an async request to actually get the permission
            // This will show the standard permission request dialog UI
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    RECORD_AUDIO_REQUEST_CODE);

        }
    }

    // Callback with the request from calling requestPermissions(...)
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        // Make sure it's our original READ_CONTACTS request
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if (grantResults.length == 3 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "Record Audio permission granted", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(this, "You must give permissions to use this app. App is exiting.", Toast.LENGTH_SHORT).show();
                finishAffinity();
            }
        }

    }
}
