package com.bugly.utils;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;

import java.io.File;
import java.io.IOException;


public class MediaRecordManager {
    private String path = Environment.getExternalStorageDirectory() + "/test/fly.m4a";
    private MediaRecorder mMediaRecorder;
    private MediaPlayer mediaPlayer;
    private boolean isRecoder = false;
    private static class A{
        private static final MediaRecordManager mediaRecordManager = new MediaRecordManager();
    }

    private MediaRecordManager(){}

    public static final MediaRecordManager getInstance(){
        return A.mediaRecordManager;
    }


    /**
     * 释放资源MediaRecorder
     */
    private void releaseRecorder() {
        if (null != mMediaRecorder) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    /**
     * 停止录音
     */
    public void stopRecord() {
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.stop();
            } catch (IllegalStateException e) {
                // TODO 如果当前java状态和jni里面的状态不一致，
                //e.printStackTrace();
                mMediaRecorder = null;
                mMediaRecorder = new MediaRecorder();
            }
            mMediaRecorder.release();
            mMediaRecorder = null;
        }

    }

    /**
     * 开始播放录音文件
     */
    public void startPlay() {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(path);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    playEndOrFail();
                }
            });

            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    playEndOrFail();
                    return true;
                }
            });
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
            playEndOrFail();
        }
    }

    /**
     * 释放资源 MediaPlayer
     */
    private void playEndOrFail() {
        if (null != mediaPlayer) {
            mediaPlayer.setOnCompletionListener(null);
            mediaPlayer.setOnErrorListener(null);
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * 准备录音
     */
    private void recordOperation() {
        File file =  new File(path);
        file.getParentFile().mkdirs();

        try {
            file.createNewFile();
            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setAudioSamplingRate(44100);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setAudioEncodingBitRate(96000);
            mMediaRecorder.setOutputFile(file.getAbsolutePath());
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    /**
     * 开始录音
     *
     */
    public void startLuYin() {

        isRecoder = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                releaseRecorder();
                recordOperation();
            }
        }).start();
    }

    /**
     * 结束录音
     *
     */
    public void jieshuLuYin() {
        isRecoder = false;
        stopRecord();
    }

    /**
     * 播放录音
     *
     */
    public void playLuYin() {

        if (isRecoder)
        {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                startPlay();
            }
        }).start();
    }


    /**
     * 删除录音文件
     */
    public void deleteFile(){

        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }

    }


}
