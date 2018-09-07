package com.zm.audio.kotlin.utils

import android.util.Log
import java.io.*

/**
 * Created by ZhouMeng on 2018/8/31.
 * 将pcm文件转化为wav文件
 * pcm是无损wav文件中音频数据的一种编码方式，pcm加上wav文件头就可以转为wav格式，但wav还可以用其它方式编码。
 * 此类就是通过给pcm加上wav的文件头，来转为wav格式
 */
object PcmToWav {
    /**
     * 合并多个pcm文件为一个wav文件
     * @param filePathList    pcm文件路径集合
     * @param destinationPath 目标wav文件路径
     * @return true|false
     */
    fun mergePCMFilesToWAVFile(filePathList: List<String>, destinationPath: String): Boolean {
        val file = arrayOfNulls<File>(filePathList.size)
        val buffer: ByteArray?

        var TOTAL_SIZE = 0
        val fileNum = filePathList.size

        for (i in 0 until fileNum) {
            file[i] = File(filePathList[i])
            TOTAL_SIZE += file[i]!!.length().toInt()
        }

        // 填入参数，比特率等等。这里用的是16位单声道 8000 hz
        val header = WaveHeader()
        // 长度字段 = 内容的大小（TOTAL_SIZE) + 头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
        header.fileLength = TOTAL_SIZE + (44 - 8)
        header.FmtHdrLeth = 16
        header.BitsPerSample = 16
        header.Channels = 2
        header.FormatTag = 0x0001
        header.SamplesPerSec = 8000
        header.BlockAlign = (header.Channels * header.BitsPerSample / 8).toShort()
        header.AvgBytesPerSec = header.BlockAlign * header.SamplesPerSec
        header.DataHdrLeth = TOTAL_SIZE

        val h: ByteArray?
        try {
            h = header.header
        } catch (e1: IOException) {
            Log.e("PcmToWav", e1.message)
            return false
        }

        // WAV标准，头部应该是44字节,如果不是44个字节则不进行转换文件
        if (h.size != 44) {
            return false
        }

        //先删除目标文件
        val destFile = File(destinationPath)
        if (destFile.exists()) {
            destFile.delete()
        }

        //合成所有的pcm文件的数据，写到目标文件
        try {
            buffer = ByteArray(1024 * 4) // Length of All Files, Total Size
            var inStream: InputStream?
            val ouStream: OutputStream?

            ouStream = BufferedOutputStream(FileOutputStream(
                    destinationPath))
            ouStream.write(h, 0, h.size)
            for (j in 0 until fileNum) {
                inStream = BufferedInputStream(FileInputStream(file[j]))
                var size = inStream.read(buffer)
                while (size != -1) {
                    ouStream.write(buffer)
                    size = inStream.read(buffer)
                }
                inStream.close()
            }
            ouStream.close()
        } catch (ioe: IOException) {
            ioe.message
            return false
        }

        FileUtils.clearFiles(filePathList)
        //        File wavFile = new File(new File(destinationPath).getParent());
        //        if (wavFile.exists()) {
        //            FileUtils.deleteFile(wavFile);
        //        }

        return true
    }
}