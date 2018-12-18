package com.zm.audio.java.utils;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.text.TextUtils;

import com.zm.audio.java.interfaces.IAudioCallback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ZhouMeng on 2018/8/31.
 * 用于实现录音、暂停、继续、停止、播放
 * 最近看了下pcm和wav，内容真多，要是有一些参数不理解的，可以查阅资料
 * PCM BufferSize = 采样率 * 采样时间 * 采样位深 / 8 * 通道数（Bytes）
 */
public class AudioRecorder {
    private static AudioRecorder audioRecorder;
    //音频输入-麦克风
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
    /**
     * 采样率即采样频率，采样频率越高，能表现的频率范围就越大
     * 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
     */
    private final static int AUDIO_SAMPLE_RATE = 16000;
    // 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;

    /**
     * 位深度也叫采样位深，音频的位深度决定动态范围
     * 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
     */
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    // 缓冲区字节大小
    private int bufferSizeInBytes = 0;

    //录音对象
    private AudioRecord audioRecord;

    /**
     * 播放声音
     * 一些必要的参数，需要和AudioRecord一一对应，否则声音会出错
     */
    private AudioTrack audioTrack;

    //录音状态,默认未开始
    private AudioStatus status = AudioStatus.STATUS_NO_READY;

    //文件名
    private String fileName;

    //录音文件集合
    private List<String> filesName = new ArrayList<>();

    //用来回调，转码后的文件绝对路径
    private static IAudioCallback iAudioCallback;
    /**
     * 创建带有缓存的线程池
     * 当执行第二个任务时第一个任务已经完成，会复用执行第一个任务的线程，而不用每次新建线程。
     * 如果线程池长度超过处理需要，可灵活回收空闲线程，若无可回收，则新建线程。
     * 一开始选择错误，选用newSingleThreadExecutor，导致停止后在录制，出现一堆问题
     */
    private ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

    /**
     * 重置，删除所有的pcm文件
     */
    private boolean isReset = false;

    private AudioRecorder() {
    }

    public void setReset() {
        isReset = true;
    }

    /**
     * 单例，双重检验
     *
     * @param iAudio 用于合成后回调
     * @return
     */
    public static AudioRecorder getInstance(IAudioCallback iAudio) {
        if (audioRecorder == null) {
            synchronized (AudioRecord.class) {
                if (audioRecorder == null) {
                    audioRecorder = new AudioRecorder();
                    iAudioCallback = iAudio;
                }
            }
        }
        return audioRecorder;
    }

    /**
     * 创建默认的录音对象
     *
     * @param fileName 文件名
     */
    public void createDefaultAudio(String fileName) {
        // 获得缓冲区字节大小
        bufferSizeInBytes = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING);
        audioRecord = new AudioRecord(AUDIO_INPUT, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING, bufferSizeInBytes);
        this.fileName = fileName;
        status = AudioStatus.STATUS_READY;

        AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();

        AudioFormat audioFormat = new AudioFormat.Builder().setSampleRate(AUDIO_SAMPLE_RATE)
                .setEncoding(AUDIO_ENCODING).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();

        audioTrack = new AudioTrack(audioAttributes, audioFormat, bufferSizeInBytes,
                AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
    }

    /**
     * 开始录音
     */
    public void startRecord() {
        if (status == AudioStatus.STATUS_NO_READY || TextUtils.isEmpty(fileName)) {
            throw new IllegalStateException("请检查录音权限");
        }
        if (status == AudioStatus.STATUS_START) {
            throw new IllegalStateException("正在录音");
        }
        audioRecord.startRecording();
        cachedThreadPool.execute(this::writeDataTOFile);
    }

    /**
     * 暂停录音
     */
    public void pauseRecord() {
        if (status != AudioStatus.STATUS_START) {
            throw new IllegalStateException("没有在录音");
        } else {
            audioRecord.stop();
            status = AudioStatus.STATUS_PAUSE;
        }
    }

    /**
     * 停止录音
     */
    public void stopRecord() {
        if (status == AudioStatus.STATUS_NO_READY || status == AudioStatus.STATUS_READY) {
            throw new IllegalStateException("录音尚未开始");
        } else {
            audioRecord.stop();
            status = AudioStatus.STATUS_STOP;
            release();
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        //假如有暂停录音
        try {
            if (filesName.size() > 0) {
                List<String> filePaths = new ArrayList<>();
                for (String fileName : filesName) {
                    filePaths.add(FileUtils.getPcmFileAbsolutePath(fileName));
                }
                //清除
                filesName.clear();
                if (isReset) {
                    isReset = false;
                    FileUtils.clearFiles(filePaths);
                } else {
                    //将多个pcm文件转化为wav文件
                    pcmFilesToWavFile(filePaths);
                }
            }
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e.getMessage());
        }

        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
        status = AudioStatus.STATUS_NO_READY;
    }

    /**
     * 播放合成后的wav文件
     *
     * @param filePath 文件的绝对路径
     */
    public void play(final String filePath) {
        audioTrack.play();

        cachedThreadPool.execute(() -> {
            File file = new File(filePath);
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            byte[] buffer = new byte[bufferSizeInBytes];
            while (fis != null) {
                try {
                    int readCount = fis.read(buffer);
                    if (readCount == AudioTrack.ERROR_INVALID_OPERATION || readCount == AudioTrack.ERROR_BAD_VALUE) {
                        continue;
                    }
                    if (readCount != 0 && readCount != -1) {
                        audioTrack.write(buffer, 0, readCount);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 释放audioTrack
     */
    public void releaseAudioTrack(){
        if (audioTrack == null) {
            return;
        }
        if (audioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
            audioTrack.stop();
        }
        audioTrack.release();
        audioTrack = null;
    }

    /**
     * 将音频信息写入文件
     */
    private void writeDataTOFile() {
        // new一个byte数组用来存一些字节数据，大小为缓冲区大小
        byte[] audioData = new byte[bufferSizeInBytes];
        FileOutputStream fos = null;
        int readSize = 0;
        try {
            String currentFileName = fileName;
            if (status == AudioStatus.STATUS_PAUSE) {
                //假如是暂停录音 将文件名后面加个数字,防止重名文件内容被覆盖
                currentFileName += filesName.size();
            }
            filesName.add(currentFileName);
            File file = new File(FileUtils.getPcmFileAbsolutePath(currentFileName));
            if (file.exists()) {
                file.delete();
            }
            // 建立一个可存取字节的文件
            fos = new FileOutputStream(file);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //将录音状态设置成正在录音状态
        status = AudioStatus.STATUS_START;
        while (status == AudioStatus.STATUS_START) {
            readSize = audioRecord.read(audioData, 0, bufferSizeInBytes);
            if (AudioRecord.ERROR_INVALID_OPERATION != readSize && fos != null) {
                try {
                    fos.write(audioData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            if (fos != null) {
                fos.close();// 关闭写入流
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将pcm合并成wav
     *
     * @param filePaths pcm文件的绝对路径
     */
    private void pcmFilesToWavFile(final List<String> filePaths) {
        cachedThreadPool.execute(() -> {
            String filePath = FileUtils.getWavFileAbsolutePath(fileName);
            if (PcmToWav.mergePCMFilesToWAVFile(filePaths, filePath)) {
                //合成后回调
                if (iAudioCallback != null) {
                    iAudioCallback.showPlay(filePath);
                }
            } else {
                throw new IllegalStateException("合成失败");
            }
            fileName = null;
        });
    }

    /**
     * 获取录音对象的状态
     */
    public AudioStatus getStatus() {
        return status;
    }
}