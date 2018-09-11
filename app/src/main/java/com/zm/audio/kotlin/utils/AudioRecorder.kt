package com.zm.audio.kotlin.utils

import android.media.*
import android.text.TextUtils
import com.zm.audio.kotlin.interfaces.IAudioCallback
import java.io.*
import java.util.*
import java.util.concurrent.Executors

/**
 * Created by ZhouMeng on 2018/8/31.
 * 用于实现录音、暂停、继续、停止、播放
 * 最近看了下pcm和wav，内容真多，要是有一些参数不理解的，可以查阅资料
 * PCM BufferSize = 采样率 * 采样时间 * 采样位深 / 8 * 通道数（Bytes）
 */
class AudioRecorder private constructor() {
    // 缓冲区字节大小
    private var bufferSizeInBytes = 0

    //录音对象
    private var audioRecord: AudioRecord? = null

    /**
     * 播放声音
     * 一些必要的参数，需要和AudioRecord一一对应，否则声音会出错
     */
    private var audioTrack: AudioTrack? = null

    //录音状态,默认未开始
    /**
     * 获取录音对象的状态
     */
    var status = AudioStatus.STATUS_NO_READY
        private set

    //文件名
    private var fileName: String? = null

    //录音文件集合
    private val filesName = ArrayList<String>()
    /**
     * 此线程池是一个单线程化的线程池，它只会用唯一的工作线程来执行任务，保证所有任务按照指定顺序。
     * 如果这个线程异常结束，会有另一个取代它，保证顺序执行。
     */
    private val cachedThreadPool = Executors.newCachedThreadPool()

    /**
     * 重置，删除所有的pcm文件
     */
    private var isReset = false

    fun setReset() {
        isReset = true
    }

    /**
     * 创建默认的录音对象
     *
     * @param fileName 文件名
     */
    fun createDefaultAudio(fileName: String) {
        // 获得缓冲区字节大小
        bufferSizeInBytes = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING)
        audioRecord = AudioRecord(AUDIO_INPUT, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING, bufferSizeInBytes)
        this.fileName = fileName
        status = AudioStatus.STATUS_READY

        val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()

        val audioFormat = AudioFormat.Builder().setSampleRate(AUDIO_SAMPLE_RATE)
                .setEncoding(AUDIO_ENCODING).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()

        audioTrack = AudioTrack(audioAttributes, audioFormat, bufferSizeInBytes,
                AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
    }

    /**
     * 开始录音
     */
    fun startRecord() {
        if (status == AudioStatus.STATUS_NO_READY || TextUtils.isEmpty(fileName)) {
            throw IllegalStateException("请检查录音权限")
        }
        if (status == AudioStatus.STATUS_START) {
            throw IllegalStateException("正在录音")
        }
        audioRecord!!.startRecording()
        cachedThreadPool.execute { writeDataTOFile() }
    }

    /**
     * 暂停录音
     */
    fun pauseRecord() {
        if (status != AudioStatus.STATUS_START) {
            throw IllegalStateException("没有在录音")
        } else {
            audioRecord!!.stop()
            status = AudioStatus.STATUS_PAUSE
        }
    }

    /**
     * 停止录音
     */
    fun stopRecord() {
        if (status == AudioStatus.STATUS_NO_READY || status == AudioStatus.STATUS_READY) {
            throw IllegalStateException("录音尚未开始")
        } else {
            audioRecord!!.stop()
            status = AudioStatus.STATUS_STOP
            release()
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        //假如有暂停录音
        try {
            if (filesName.size > 0) {
                val filePaths = ArrayList<String>()
                for (fileName in filesName) {
                    filePaths.add(FileUtils.getPcmFileAbsolutePath(fileName))
                }
                //清除
                filesName.clear()
                if (isReset) {
                    isReset = false
                    FileUtils.clearFiles(filePaths)
                } else {
                    //将多个pcm文件转化为wav文件
                    pcmFilesToWavFile(filePaths)
                }
            }
        } catch (e: IllegalStateException) {
            throw IllegalStateException(e.message)
        }

        if (audioRecord != null) {
            audioRecord!!.release()
            audioRecord = null
        }
        status = AudioStatus.STATUS_NO_READY
    }

    /**
     * 播放合成后的wav文件
     *
     * @param filePath 文件的绝对路径
     */
    fun play(filePath: String) {
        audioTrack!!.play()

        cachedThreadPool.execute {
            val file = File(filePath)
            var fis: FileInputStream? = null
            try {
                fis = FileInputStream(file)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

            val buffer = ByteArray(bufferSizeInBytes)
            while (fis != null) {
                try {
                    val readCount = fis.read(buffer)
                    if (readCount == AudioTrack.ERROR_INVALID_OPERATION || readCount == AudioTrack.ERROR_BAD_VALUE) {
                        continue
                    }
                    if (readCount != 0 && readCount != -1) {
                        audioTrack!!.write(buffer, 0, readCount)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
    }

    /**
    * 释放audioTrack
    */
    fun releaseAudioTrack(){
        if (audioTrack?.playState != AudioRecord.RECORDSTATE_STOPPED) {
            audioTrack?.stop()
        }
        audioTrack?.release()
        audioTrack = null
    }

    /**
     * 将音频信息写入文件
     */
    private fun writeDataTOFile() {
        // new一个byte数组用来存一些字节数据，大小为缓冲区大小
        val audioData = ByteArray(bufferSizeInBytes)
        var fos: FileOutputStream? = null
        var readSize = 0
        try {
            var currentFileName = fileName
            if (status == AudioStatus.STATUS_PAUSE) {
                //假如是暂停录音 将文件名后面加个数字,防止重名文件内容被覆盖
                currentFileName += filesName.size
            }
            filesName.add(currentFileName!!)
            val file = File(FileUtils.getPcmFileAbsolutePath(currentFileName))
            if (file.exists()) {
                file.delete()
            }
            // 建立一个可存取字节的文件
            fos = FileOutputStream(file)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            throw IllegalStateException(e.message)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        //将录音状态设置成正在录音状态
        status = AudioStatus.STATUS_START
        while (status == AudioStatus.STATUS_START) {
            readSize = audioRecord!!.read(audioData, 0, bufferSizeInBytes)
            if (AudioRecord.ERROR_INVALID_OPERATION != readSize && fos != null) {
                try {
                    fos.write(audioData)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
        try {
            if (fos != null) {
                fos.close()// 关闭写入流
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    /**
     * 将pcm合并成wav
     *
     * @param filePaths pcm文件的绝对路径
     */
    private fun pcmFilesToWavFile(filePaths: List<String>) {
        cachedThreadPool.execute {
            val filePath = FileUtils.getWavFileAbsolutePath(fileName)
            if (PcmToWav.mergePCMFilesToWAVFile(filePaths, filePath)) {
                //合成后回调
                if (iAudioCallback != null) {
                    iAudioCallback!!.showPlay(filePath)
                }
            } else {
                throw IllegalStateException("合成失败")
            }
            fileName = null
        }
    }

    companion object {
        private var audioRecorder: AudioRecorder? = null
        //音频输入-麦克风
        private const val AUDIO_INPUT = MediaRecorder.AudioSource.MIC
        /**
         * 采样率即采样频率，采样频率越高，能表现的频率范围就越大
         * 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
         */
        private const val AUDIO_SAMPLE_RATE = 44100
        // 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
        private const val AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO
        /**
         * 位深度也叫采样位深，音频的位深度决定动态范围
         * 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
         */
        private const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT

        //用来回调，转码后的文件绝对路径
        private var iAudioCallback: IAudioCallback? = null

        /**
         * 单例，双重检验
         * @param iAudio 用于合成后回调
         * @return
         */
        fun getInstance(iAudio: IAudioCallback): AudioRecorder {
            if (audioRecorder == null) {
                synchronized(AudioRecord::class.java) {
                    if (audioRecorder == null) {
                        audioRecorder = AudioRecorder()
                        iAudioCallback = iAudio
                    }
                }
            }
            return audioRecorder!!
        }
    }
}
