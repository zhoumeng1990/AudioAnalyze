package com.zm.audio.kotlin.utils

import android.os.Environment
import android.text.TextUtils

import java.io.File

/**
 * Created by ZhouMeng on 2018/8/31.
 * 管理录音文件的类
 */
object FileUtils {

    private const val rootPath = "zm"
    //原始文件(不能播放)
    private const val AUDIO_PCM_BASE_PATH = "/$rootPath/pcm/"
    //可播放的高质量音频文件
    private const val AUDIO_WAV_BASE_PATH = "/$rootPath/wav/"

    /**
     * 判断是否有外部存储设备sdcard
     *
     * @return true | false
     */
    private val isSdcardExit: Boolean
        get() = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

    fun getPcmFileAbsolutePath(fileName: String): String {
        var fileName = fileName
        if (TextUtils.isEmpty(fileName)) {
            throw NullPointerException("fileName isEmpty")
        }
        if (!isSdcardExit) {
            throw IllegalStateException("sd card no found")
        }
        var mAudioRawPath = ""
        if (isSdcardExit) {
            if (!fileName.endsWith(".pcm")) {
                fileName = "$fileName.pcm"
            }
            val fileBasePath = Environment.getExternalStorageDirectory().absolutePath + AUDIO_PCM_BASE_PATH
            val file = File(fileBasePath)
            //创建目录
            if (!file.exists()) {
                file.mkdirs()
            }
            mAudioRawPath = fileBasePath + fileName
        }
        return mAudioRawPath
    }

    fun getWavFileAbsolutePath(fileName: String?): String {
        var fileName: String? = fileName ?: throw NullPointerException("fileName is null")
        if (!isSdcardExit) {
            throw IllegalStateException("sd card no found")
        }

        var mAudioWavPath = ""
        if (isSdcardExit) {
            if (!fileName!!.endsWith(".wav")) {
                fileName = "$fileName.wav"
            }
            val fileBasePath = Environment.getExternalStorageDirectory().absolutePath + AUDIO_WAV_BASE_PATH
            val file = File(fileBasePath)
            //创建目录
            if (!file.exists()) {
                file.mkdirs()
            }
            mAudioWavPath = fileBasePath + fileName
        }
        return mAudioWavPath
    }

    /**
     * 清除文件
     *
     * @param filePathList
     */
    fun clearFiles(filePathList: List<String>) {
        for (i in filePathList.indices) {
            val file = File(filePathList[i])
            if (file.exists()) {
                file.delete()
            }
        }
    }

    fun deleteFile(file: File) {
        if (file.isDirectory) {
            val files = file.listFiles()
            for (i in files.indices) {
                val f = files[i]
                deleteFile(f)
            }
            file.delete()//如要保留文件夹，只删除文件，请注释这行
        } else if (file.exists()) {
            file.delete()
        }
    }
}
