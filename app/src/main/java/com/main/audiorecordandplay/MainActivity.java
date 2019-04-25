package com.main.audiorecordandplay;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private MediaRecorder recorder;
    private MediaPlayer player;
    private Button startRecord,stopRecord,startPlay,stopPlay,startRecordPlay,stopRecordPlay;
    private File path;
    /**
     * rbtn 采样精度 pcm16
     */

    private int recBufSize = 0;

    private int playBufSize = 0;

    /**
     * 采样率（默认44100，每秒44100个点）
     */
    private int sampleRateInHz = 44100;

    /**
     * 声道（默认单声道）
     */
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;

    /**
     * 编码率（默认ENCODING_PCM_16BIT）
     */
    private int encodingBitrate = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord;

    private AudioTrack audioTrack;
    /**
     * 即时播放
     */
    private boolean blnInstantPlay = false;
    private ThreadInstantPlay  threadInstantPlay;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startRecord = findViewById(R.id.startRecord);
        startPlay = findViewById(R.id.startPlay);
        stopRecord = findViewById(R.id.stopRecord);
        stopPlay = findViewById(R.id.stopPlay);
        startRecordPlay = findViewById(R.id.startRecordPlay);
        stopRecordPlay = findViewById(R.id.stopRecordPlay);

        startRecord.setOnClickListener(this);
        startPlay.setOnClickListener(this);
        stopRecord.setOnClickListener(this);
        stopPlay.setOnClickListener(this);
        startRecordPlay.setOnClickListener(this);
        stopRecordPlay.setOnClickListener(this);
        startRecordPlay.setEnabled(false);
        stopRecordPlay.setEnabled(false);
        path = new File(Environment.getExternalStorageDirectory(),"testRecording.mp4");

    }

    private void resetAndStartRecorder(){
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        recorder.setOutputFile(path.getAbsolutePath());
        try {
            recorder.prepare();
        }catch (Exception e){
            e.printStackTrace();
        }
        recorder.start();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.startRecord:
                try {
                    resetAndStartRecorder();
                    startRecord.setEnabled(false);
                    stopRecord.setEnabled(true);
                    startPlay.setEnabled(false);
                    stopPlay.setEnabled(false);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.stopRecord:
                recorder.stop();
                recorder.release();
                recorder = null;
                startRecord.setEnabled(true);
                stopRecord.setEnabled(false);
                startPlay.setEnabled(true);
                stopPlay.setEnabled(false);
                break;
            case R.id.startPlay:
                try {
                    player = MediaPlayer.create(this,Uri.parse(path.getAbsolutePath()));
                    player.start();
                    startRecord.setEnabled(false);
                    stopRecord.setEnabled(false);
                    startPlay.setEnabled(false);
                    stopPlay.setEnabled(true);
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            case R.id.stopPlay:
                player.stop();
                player.release();
                player = null;
                startRecord.setEnabled(true);
                stopRecord.setEnabled(false);
                startPlay.setEnabled(true);
                stopPlay.setEnabled(false);
                break;
            case R.id.startRecordPlay:
                startRecordPlay.setEnabled(false);
                stopRecordPlay.setEnabled(true);

                recBufSize = AudioRecord.getMinBufferSize(sampleRateInHz,
                        channelConfig, encodingBitrate);
                playBufSize = AudioTrack.getMinBufferSize(sampleRateInHz,
                        channelConfig, encodingBitrate);

                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        sampleRateInHz, channelConfig, encodingBitrate, recBufSize);
                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz,
                        channelConfig, encodingBitrate, playBufSize, AudioTrack.MODE_STREAM);
                blnInstantPlay = true;
                threadInstantPlay =  new ThreadInstantPlay();
                threadInstantPlay.start();
                break;
            case R.id.stopRecordPlay:
                blnInstantPlay = false;
                stopRecordPlay.setEnabled(false);
                startRecordPlay.setEnabled(true);
                break;
        }
    }
    /**
     *
     * 即时播放线程
     *
     */
    class ThreadInstantPlay extends Thread
    {
        @Override
        public void run()
        {
            byte[] bsBuffer = new byte[recBufSize];
            audioRecord.startRecording();
            audioTrack.play();
            while(blnInstantPlay)
            {

                int line = audioRecord.read(bsBuffer, 0, recBufSize);

                byte[] tmpBuf = new byte[line];

                System.arraycopy(bsBuffer, 0, tmpBuf, 0, line);

                audioTrack.write(tmpBuf, 0, tmpBuf.length);
            }
            audioTrack.stop();
            audioRecord.stop();
        }
    }


    public void onQuit(View v){
        finish();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(recorder != null){
            recorder.release();
        }
        if(player != null){
            player.release();
        }
    }

}
