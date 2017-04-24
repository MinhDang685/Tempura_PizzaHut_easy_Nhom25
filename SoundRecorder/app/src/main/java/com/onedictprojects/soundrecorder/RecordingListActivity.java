package com.onedictprojects.soundrecorder;

import android.app.Dialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.v7.widget.PopupMenu;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



public class RecordingListActivity extends AppCompatActivity {

    List<AudioItem> adapter = null;

    ListView listView = null;
    MediaPlayer player = null;
    AudioItem selectedAudioItem = new AudioItem("","","");

    ImageButton imageButtonPlayPause;
    SeekBar seekBar;
    TextView textViewFileNamePlaying = null;
    Handler threadHandler = new Handler();

    boolean isPlaying = false;
    boolean isNewAudio = false;
    boolean isPlayed = false;
    String currentAudioFilePath = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording_list);

        imageButtonPlayPause = (ImageButton) findViewById(R.id.imageButton_playPause);
        textViewFileNamePlaying= (TextView) findViewById(R.id.textView_filenamePlaying);
        imageButtonPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPlaying) {
                    pauseAudio();
                }
                else playAudio(currentAudioFilePath);
            }
        });
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setClickable(false);

        adapter = getAllFileName();
        listView= (ListView) findViewById(R.id.lvRecording);
        listView.setAdapter(new CustomListAdapter(adapter,this));

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                Object o = listView.getItemAtPosition(position);
                AudioItem item = (AudioItem) o;
                currentAudioFilePath = item.getPath();
                isNewAudio = true;
                textViewFileNamePlaying.setText(item.getFilename());
                playAudio(currentAudioFilePath);
            }
        });

        registerForContextMenu(listView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu,View v,ContextMenu.ContextMenuInfo menuInfo) {
        if(v.getId()==R.id.lvRecording) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            selectedAudioItem = adapter.get(info.position);

            super.onCreateContextMenu(menu,v,menuInfo);
            getMenuInflater().inflate(R.menu.popup,menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.share:
                Toast.makeText(getApplicationContext(),"Shared" + selectedAudioItem.getFilename(), Toast.LENGTH_SHORT).show();
                break;
            case R.id.delete:
                break;
            case R.id.rename:
                break;
            case R.id.sync:
                break;
            case R.id.detail:
                break;
        }

        return super.onContextItemSelected(item);
    }

    //dialog
    final Context context = this;
    private void ShowFileDetailDialog()
    {
        // custom dialog
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.file_detail);
        dialog.setTitle("File detail");
        // set the custom dialog components - text, image and button
        TextView fileName = (TextView) findViewById(R.id.textview_filename);
        TextView fileType = (TextView) findViewById(R.id.textview_filetype);
        TextView fileSize = (TextView) findViewById(R.id.textview_filesize);
        TextView fileLength = (TextView) findViewById(R.id.textview_filelength);
        TextView fileDateModified = (TextView) findViewById(R.id.textview_datemodified);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       // getMenuInflater().inflate(R.menu.popup,menu);
        return  true;
    }

    private List<AudioItem> getAllFileName() {
        //String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        List<AudioItem> list= new ArrayList<AudioItem>();

        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        File dir = new File(path,RecordActivity.AUDIO_RECORDER_FOLDER);

        if(!dir.exists())
            dir.mkdir();

        File[] files = dir.listFiles();

        for(int i=0;i<files.length;i++) {
            AudioItem audioItem = new AudioItem("wav_file",files[i].getName(),files[i].getAbsolutePath());
            list.add(audioItem);
        }

        return list;
    }

    private void playAudio(String filePath) {

        if(isNewAudio || isPlayed) {

            if(player!=null) {
                player.stop();
                //player.reset();
                player.release();
            }
            player = new MediaPlayer();
            isPlayed=false;
            try {
                player.setDataSource(filePath);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            try {
                player.prepare();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        int duration = this.player.getDuration();
        int currentPosition = player.getCurrentPosition();
        if(currentPosition == 0) {
            this.seekBar.setMax(duration);
        }

        player.start();
        imageButtonPlayPause.setImageResource(R.drawable.pause50);
        isPlaying=true;
        isNewAudio=false;


        //tao thread update seekbar
        UpdateSeekbarThread updateSeekbarThread = new UpdateSeekbarThread();
        threadHandler.postDelayed(updateSeekbarThread,50);
    }

    class UpdateSeekbarThread implements Runnable {
        public void run() {
            int currentPosition = player.getCurrentPosition();
            int duration = player.getDuration();
            seekBar.setProgress(currentPosition);
            threadHandler.postDelayed(this,50);

            // chuyen trang thai nut pause sang play khi phat xong
            if(duration-currentPosition<=50) {
                player.reset();
                imageButtonPlayPause.setImageResource(R.drawable.play50);

                seekBar.setProgress(0);

                isPlayed=true;

                isPlaying=false;
                isNewAudio=false;
            }
        }
    }

    private void pauseAudio() {
        if(isPlaying) {
            player.pause();
            isPlaying=false;
            imageButtonPlayPause.setImageResource(R.drawable.play50);
        }
    }
}
