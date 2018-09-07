package com.zm.audio.java.utils;

import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.util.List;

/**
 * Created by ZhouMeng on 2018/8/31.
 * 管理录音文件的类
 */
public class FileUtils {

    private  static String rootPath="zm";
    //原始文件(不能播放)
    public final static String AUDIO_PCM_BASE_PATH = "/"+rootPath+"/pcm/";
    //可播放的高质量音频文件
    public final static String AUDIO_WAV_BASE_PATH = "/"+rootPath+"/wav/";

    public static String getPcmFileAbsolutePath(String fileName){
        if(TextUtils.isEmpty(fileName)){
            throw new NullPointerException("fileName isEmpty");
        }
        if(!isSdcardExit()){
            throw new IllegalStateException("sd card no found");
        }
        String mAudioRawPath = "";
        if (isSdcardExit()) {
            if (!fileName.endsWith(".pcm")) {
                fileName = fileName + ".pcm";
            }
            String fileBasePath = Environment.getExternalStorageDirectory().getAbsolutePath() + AUDIO_PCM_BASE_PATH;
            File file = new File(fileBasePath);
            //创建目录
            if (!file.exists()) {
                file.mkdirs();
            }
            mAudioRawPath = fileBasePath + fileName;
        }
        return mAudioRawPath;
    }

    public static String getWavFileAbsolutePath(String fileName) {
        if(fileName==null){
            throw new NullPointerException("fileName is null");
        }
        if(!isSdcardExit()){
            throw new IllegalStateException("sd card no found");
        }

        String mAudioWavPath = "";
        if (isSdcardExit()) {
            if (!fileName.endsWith(".wav")) {
                fileName = fileName + ".wav";
            }
            String fileBasePath = Environment.getExternalStorageDirectory().getAbsolutePath() + AUDIO_WAV_BASE_PATH;
            File file = new File(fileBasePath);
            //创建目录
            if (!file.exists()) {
                file.mkdirs();
            }
            mAudioWavPath = fileBasePath + fileName;
        }
        return mAudioWavPath;
    }

    /**
     * 判断是否有外部存储设备sdcard
     *
     * @return true | false
     */
    private static boolean isSdcardExit() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 清除文件
     *
     * @param filePathList
     */
    public static void clearFiles(List<String> filePathList) {
        for (int i = 0; i < filePathList.size(); i++) {
            File file = new File(filePathList.get(i));
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public static void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                deleteFile(f);
            }
            file.delete();//如要保留文件夹，只删除文件，请注释这行
        } else if (file.exists()) {
            file.delete();
        }
    }
}
