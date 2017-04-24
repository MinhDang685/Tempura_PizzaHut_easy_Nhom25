package com.onedictprojects.soundrecorder;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.ActionBar;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

public class RecordActivity extends AppCompatActivity {

    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    public static final String AUDIO_RECORDER_FOLDER = "SoundRecorder";
    private static final String AUDIO_RECORDER_TEMP_FOLDER = "SoundTemp";
    private static final String AUDIO_RECORDER_TEMP_EXT_FILE = ".raw";
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    private static String tmpPlay = null;

    private TextView hour = null;
    private TextView min = null;
    private TextView sec = null;

    private int hourCounter = 0;
    private int minCounter = 0;
    private int secCounter = 0;

    Handler threadHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        hour = (TextView) findViewById(R.id.txtHour);
        min = (TextView) findViewById(R.id.txtMin);
        sec = (TextView) findViewById(R.id.txtSec);

        setButtonHandlers();
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

        UpdateThreadTimer updateThreadTimer = new UpdateThreadTimer();
        threadHandler.postDelayed(updateThreadTimer,1000);

        //action bar
//        ActionBar actionBar = getActionBar();
//        actionBar.setDisplayHomeAsUpEnabled(true);

        //notification
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals("START")) {
                    Toast.makeText(getApplication(), "START", Toast.LENGTH_LONG).show();
                    processEnableRecording();
                    updateNotificationWhenUserTouchStart();
                    startBackgroundColorTransition();
                }
                else if(intent.getAction().equals("STOP")) {
                    Toast.makeText(getApplication(), "STOP", Toast.LENGTH_LONG).show();
                    processPauseRecording();
                    processStopRecording();
                    updateNotificationWhenUserTouchStop();
                    endBackgroundColorTransition();
                }
                else if(intent.getAction().equals("PAUSE")) {
                    Toast.makeText(getApplication(), "PAUSE", Toast.LENGTH_LONG).show();
                    processPauseRecording();
                    updateNotificationWhenUserTouchPause();
                    pauseBackgroundColorTransition();
                }
                else if(intent.getAction().equals("RESUME")) {
                    Toast.makeText(getApplication(), "RESUME", Toast.LENGTH_LONG).show();
                    processEnableRecording();
                    updateNotificationWhenUserTouchResume();
                    startBackgroundColorTransition();
                }
                else if(intent.getAction().equals("DELETE")) {
                    Toast.makeText(getApplication(), "DELETE", Toast.LENGTH_LONG).show();
                    processPauseRecording();
                    processDeleteRecording();
                    updateNotificationWhenUserTouchStop();
                    endBackgroundColorTransition();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("START");
        filter.addAction("PAUSE");
        filter.addAction("STOP");
        filter.addAction("RESUME");
        filter.addAction("DELETE");
        registerReceiver(receiver, filter);
        addNotification();
        int duration = 10000;
        initializeBackgroundColorTransition(duration);
    }

    final int NOTIFICATION_ID = 0;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id==R.id.mnList) {
            // open recording list activity
            Intent intent = new Intent(RecordActivity.this,RecordingListActivity.class);
            startActivity(intent);
        }

        else if(id==R.id.mnSettings) {

        }

        else if(id==R.id.mnAbout) {

        }

        return super.onOptionsItemSelected(item);
    }


    private void startRecording () {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING,bufferSize);
        int i = recorder.getState();

        if(i==1) {
            recorder.startRecording();
        }

        isRecording=true;
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioToFile();
            }
        },"AudioRecorder Thread");

        recordingThread.start();
    }

    private void writeAudioToFile() {
        byte[] data = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream fileOutputStream =null;

        try {
            fileOutputStream = new FileOutputStream(filename);
        } catch (FileNotFoundException ex) {
            System.out.println("SoundRecorder Error: "+ ex.getMessage());
        }

        int read = 0;

        if(fileOutputStream!=null) {
            while (isRecording) {
                read = recorder.read(data,0,bufferSize);

                if(AudioRecord.ERROR_INVALID_OPERATION != read) {
                    try {
                        fileOutputStream.write(data);
                    } catch (IOException ex) {
                        System.out.println("SoundRecorder Error: "+ ex.getMessage());
                    }
                }
            }

            try {
                fileOutputStream.close();
            } catch (IOException ex) {
                System.out.println("SoundRecorder Error: "+ ex.getMessage());
            }
        }
    }

    private void pauseRecording() {
        if(recorder!=null) {
            isRecording = false;
            int i= recorder.getState();
            if(i==1) {
                recorder.stop();
            }

            recorder.release();
            recorder=null;
            recordingThread = null;
        }
    }

    private void stopRecording() {
        tmpPlay = getFilename();
        copyWaveFile(getAllTempFilename(),tmpPlay);
        deleteTempFile(getAllTempFilename());

        secCounter=0;
        minCounter=0;
        hourCounter=0;

        sec.setText("00");
        min.setText("00");
        hour.setText("00");
    }

    private void copyWaveFile(Vector<String> allTempFile,String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;

        long totalAudioLength = 0;
        long totalDataLength = totalAudioLength + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[bufferSize];

        for(int i=0;i<allTempFile.size();i++) {
            try {
                in = new FileInputStream(allTempFile.elementAt(i));
                totalAudioLength += in.getChannel().size();
                in.close();
            } catch (FileNotFoundException ex) {
                System.out.println("SoundRecorder Error: "+ex.getMessage());
            } catch (IOException ex) {
                System.out.println("SoundRecorder Error: "+ex.getMessage());
            }
        }

        totalDataLength = totalAudioLength+36;

        try {
            out = new FileOutputStream(outFilename);
        } catch (FileNotFoundException ex) {
            System.out.println("SoundRecorder Error: "+ex.getMessage());
        }

        writeWaveFileHeader(out, totalAudioLength, totalDataLength, longSampleRate, channels, byteRate);

        for(int i=0;i<allTempFile.size();i++) {

            try {
                in = new FileInputStream(allTempFile.elementAt(i));
                while (in.read(data) != -1) {
                    out.write(data);
                }

                in.close();
            } catch (FileNotFoundException ex) {
                System.out.println("SoundRecorder Error: "+ex.getMessage());
            } catch (IOException ex) {
                System.out.println("SoundRecorder Error: "+ex.getMessage());
            }
        }

        try {
            out.close();
        } catch (IOException ex) {
            System.out.println("SoundRecorder Error: "+ex.getMessage());
        }

    }

    private void writeWaveFileHeader( FileOutputStream out, long totalAudioLength, long totalDataLength, long longSampleRate, int channels, long byteRate) {
        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLength & 0xff);
        header[5] = (byte) ((totalDataLength >> 8) & 0xff);
        header[6] = (byte) ((totalDataLength >> 16) & 0xff);
        header[7] = (byte) ((totalDataLength >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLength & 0xff);
        header[41] = (byte) ((totalAudioLength >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLength >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLength >> 24) & 0xff);

        try {
            out.write(header, 0, 44);
        } catch (IOException ex) {
            System.out.println("SoundRecorder Error: "+ex.getMessage());
        }

    }

    private void deleteTempFile(Vector<String> tempFilename) {

        for(int i=0;i<tempFilename.size();i++) {
            File file = new File(tempFilename.elementAt(i));
            file.delete();
        }
    }

    private Vector<String> getAllTempFilename() {
        Vector<String> tmp = new Vector<>();

        //String path = getFilesDir().getAbsolutePath();
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();

        File dir = new File(path,AUDIO_RECORDER_TEMP_FOLDER);

        if(!dir.exists())
            dir.mkdir();

        File[] files = dir.listFiles();

        for(int i=0;i<files.length;i++)
            tmp.add(files[i].getAbsolutePath());

        return tmp;
    }

    private String getFilename() {
        //String filePath = getFilesDir().getAbsolutePath();
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File file = new File(filePath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()) {
            file.mkdir();
        }

        return (file.getAbsolutePath()+File.separator+System.currentTimeMillis()+AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private String getTempFilename() { // chua cai dat
        //String filepath = getFilesDir().getAbsolutePath();
        String filepath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File file = new File(filepath,AUDIO_RECORDER_TEMP_FOLDER);

        if(!file.exists()) {
            file.mkdir();
        }

        return (file.getAbsolutePath()+File.separator+System.currentTimeMillis()+AUDIO_RECORDER_TEMP_EXT_FILE);
    }

    private void setButtonHandlers() {
        ((ImageButton) findViewById(R.id.btnStartPause)).setOnClickListener(btnClick);
        ((ImageButton) findViewById(R.id.btnStop)).setOnClickListener(btnClick);
        ((ImageButton) findViewById(R.id.btnDelete)).setOnClickListener(btnClick);
    }

    private View.OnClickListener btnClick  = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStartPause:
                    if(isRecording==false) {
                        processEnableRecording();
                        updateNotificationWhenUserTouchStart();
                        startBackgroundColorTransition();
                    }
                    else {
                        processPauseRecording();
                        updateNotificationWhenUserTouchPause();
                        pauseBackgroundColorTransition();
                    }
                    break;
                case R.id.btnStop:
                    processStopRecording();
                    updateNotificationWhenUserTouchStop();
                    endBackgroundColorTransition();
                    break;
                case R.id.btnDelete:
                    processDeleteRecording();
                    updateNotificationWhenUserTouchStop();
                    endBackgroundColorTransition();
                    break;
            }
        }
    };

    private void processEnableRecording() {
        enableButton(false);
        toggleStartPause(true);
        startRecording();
    }

    private void processPauseRecording() {
        enableButton(true);
        toggleStartPause(false);
        pauseRecording();
    }

    private void processStopRecording() {
        stopRecording();
        enableButton(false);
    }

    private void processDeleteRecording(){
        secCounter=0;
        minCounter=0;
        hourCounter=0;

        sec.setText("00");
        min.setText("00");
        hour.setText("00");
        deleteTempFile(getAllTempFilename());
        enableButton(false);
    }

    private void toggleStartPause(boolean isRecording) {
        ImageButton button = (ImageButton) findViewById(R.id.btnStartPause);

        if(isRecording)
            button.setImageResource(R.drawable.pause50);
        else
            button.setImageResource(R.drawable.microphone48);

    }

    private void enableButton(int id, boolean isEnable) {
        ImageButton button = (ImageButton) findViewById(id);
        button.setEnabled(isEnable);

        if(isEnable)
            button.setVisibility(View.VISIBLE);
        else
            button.setVisibility(View.INVISIBLE);
    }

    private void enableButton(boolean isRecording) {
        enableButton(R.id.btnStop,isRecording);
        enableButton(R.id.btnDelete,isRecording);
    }

    //notification
    NotificationCompat.Builder builder;
    private void addNotification() {
        Intent newIntentStart = new Intent("START");
        Intent newIntentOpen = new Intent(this, RecordActivity.class);
        PendingIntent pendingStart = PendingIntent.getBroadcast(this, 0, newIntentStart, 0);
        PendingIntent pendingOpen = PendingIntent.getActivity(this, 0, newIntentOpen,
                PendingIntent.FLAG_UPDATE_CURRENT);

        builder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.logo)
                        .setContentTitle("easyRecord")
                        .setContentText("...ready...")
                        .setOngoing(true)
                        .setAutoCancel(false)
                        .setPriority(Notification.PRIORITY_MAX)
                        .addAction(R.drawable.mic_idle,"Start",pendingStart);
        builder.setContentIntent(pendingOpen);

        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private void updateNotificationWhenUserTouchStart() {
        //xóa các button cũ
        builder.mActions.clear();
        //cập nhật lại icon, title
        builder.setSmallIcon(R.drawable.mic_recording);
        builder.setContentText("...recording...");
        //thêm các button mới
        Intent newIntentPause = new Intent("PAUSE");
        Intent newIntentStop = new Intent("STOP");
        Intent newIntentCancel = new Intent("DELETE");
        PendingIntent pendingPause = PendingIntent.getBroadcast(this, 0, newIntentPause, 0);
        PendingIntent pendingStop = PendingIntent.getBroadcast(this, 0, newIntentStop, 0);
        PendingIntent pendingCancel = PendingIntent.getBroadcast(this, 0, newIntentCancel, 0);
        builder.addAction(R.drawable.pause50, "Pause", pendingPause);
        builder.addAction(R.drawable.stop48, "Stop", pendingStop);
        builder.addAction(R.drawable.delete64, "Cancel", pendingCancel);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private void updateNotificationWhenUserTouchStop() {
        //xóa các button cũ
        builder.mActions.clear();
        //cập nhật lại icon, title
        builder.setSmallIcon(R.drawable.mic_idle);
        builder.setContentText("...ready...");
        //thêm các button mới
        Intent newIntentStart = new Intent("START");
        PendingIntent pendingStart = PendingIntent.getBroadcast(this, 0, newIntentStart, 0);
        builder.addAction(R.drawable.mic_idle, "Start", pendingStart);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private void updateNotificationWhenUserTouchPause() {
        //xóa các button cũ
        builder.mActions.clear();
        //cập nhật lại icon, title
        builder.setSmallIcon(R.drawable.mic_idle);
        builder.setContentText("...ready...");
        //thêm các button mới
        Intent newIntentResume = new Intent("RESUME");
        Intent newIntentStop = new Intent("STOP");
        Intent newIntentCancel = new Intent("DELETE");
        PendingIntent pendingResume = PendingIntent.getBroadcast(this, 0, newIntentResume, 0);
        PendingIntent pendingStop = PendingIntent.getBroadcast(this, 0, newIntentStop, 0);
        PendingIntent pendingCancel = PendingIntent.getBroadcast(this, 0, newIntentCancel, 0);
        builder.addAction(R.drawable.mic_idle, "Resume", pendingResume);
        builder.addAction(R.drawable.stop48, "Stop", pendingStop);
        builder.addAction(R.drawable.delete64, "Stop", pendingCancel);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private void updateNotificationWhenUserTouchCancel() {
        updateNotificationWhenUserTouchStop();
    }

    private void updateNotificationWhenUserTouchResume() {
        updateNotificationWhenUserTouchStart();
    }

    private void updateNotificationText(android.support.v4.app.NotificationCompat.Builder builder, String content) {
        builder.setContentText(content);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private void clearAllButtons(android.support.v4.app.NotificationCompat.Builder builder) {
        builder.mActions.clear();
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    ValueAnimator colorAnimation;
    void initializeBackgroundColorTransition(int duration)
    {
        int colorFrom = getResources().getColor(R.color.colorDarkGreen);
        int colorTo = getResources().getColor(R.color.colorRed);
        colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(duration); // milliseconds
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                LinearLayout mainScreen = (LinearLayout) findViewById(R.id.activity_record);
                mainScreen.setBackgroundColor((int) animator.getAnimatedValue());
            }
        });
    }

    private void pauseBackgroundColorTransition() {
        colorAnimation.pause();
    }

    private void resumeBackgroundColorTransition() {
        colorAnimation.resume();
    }

    private void startBackgroundColorTransition() {
        if(colorAnimation.isRunning())
            colorAnimation.resume();
        else colorAnimation.start();
    }

    private void endBackgroundColorTransition() {
        colorAnimation.end();
        findViewById(R.id.activity_record).setBackgroundColor(getResources().getColor(R.color.colorDarkGreen));
    }

    void reverseTransitionBackgroundColor() {
        colorAnimation.setDuration(500);
        colorAnimation.reverse();
    }

    class UpdateThreadTimer implements Runnable {
        public void run() {
            if(isRecording) {
                if(secCounter<59) {
                    secCounter++;
                    if(secCounter<10)
                        sec.setText("0"+String.valueOf(secCounter));
                    else
                        sec.setText(String.valueOf(secCounter));
                }
                else {
                    secCounter=0;
                    sec.setText("00");
                    if(minCounter<59) {
                        minCounter++;
                        if(minCounter<10)
                            min.setText("0"+String.valueOf(minCounter));
                        else
                            min.setText(String.valueOf(minCounter));
                    }
                    else {
                        minCounter=0;
                        min.setText("00");
                        hourCounter++;
                        if(hourCounter<10)
                            hour.setText("0"+String.valueOf(hourCounter));
                        else
                            hour.setText(String.valueOf(hourCounter));
                    }
                }
            }

            threadHandler.postDelayed(this,1000);
        }
    }
}
