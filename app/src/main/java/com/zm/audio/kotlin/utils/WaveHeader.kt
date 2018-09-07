package com.zm.audio.kotlin.utils

import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Created by ZhouMeng on 2018/8/31.
 * wav文件头
 */
class WaveHeader {
    val fileID = charArrayOf('R', 'I', 'F', 'F')
    var fileLength: Int = 0
    var wavTag = charArrayOf('W', 'A', 'V', 'E')
    var FmtHdrID = charArrayOf('f', 'm', 't', ' ')
    var FmtHdrLeth: Int = 0
    var FormatTag: Short = 0
    var Channels: Short = 0
    var SamplesPerSec: Int = 0
    var AvgBytesPerSec: Int = 0
    var BlockAlign: Short = 0
    var BitsPerSample: Short = 0
    var DataHdrID = charArrayOf('d', 'a', 't', 'a')
    var DataHdrLeth: Int = 0

    val header: ByteArray
        @Throws(IOException::class)
        get() {
            val bos = ByteArrayOutputStream()
            WriteChar(bos, fileID)
            WriteInt(bos, fileLength)
            WriteChar(bos, wavTag)
            WriteChar(bos, FmtHdrID)
            WriteInt(bos, FmtHdrLeth)
            WriteShort(bos, FormatTag.toInt())
            WriteShort(bos, Channels.toInt())
            WriteInt(bos, SamplesPerSec)
            WriteInt(bos, AvgBytesPerSec)
            WriteShort(bos, BlockAlign.toInt())
            WriteShort(bos, BitsPerSample.toInt())
            WriteChar(bos, DataHdrID)
            WriteInt(bos, DataHdrLeth)
            bos.flush()
            val r = bos.toByteArray()
            bos.close()
            return r
        }

    @Throws(IOException::class)
    private fun WriteShort(bos: ByteArrayOutputStream, s: Int) {
        val myByte = ByteArray(2)
        myByte[1] = (s shl 16 shr 24).toByte()
        myByte[0] = (s shl 24 shr 24).toByte()
        bos.write(myByte)
    }

    @Throws(IOException::class)
    private fun WriteInt(bos: ByteArrayOutputStream, n: Int) {
        val buf = ByteArray(4)
        buf[3] = (n shr 24).toByte()
        buf[2] = (n shl 8 shr 24).toByte()
        buf[1] = (n shl 16 shr 24).toByte()
        buf[0] = (n shl 24 shr 24).toByte()
        bos.write(buf)
    }

    private fun WriteChar(bos: ByteArrayOutputStream, id: CharArray) {
        for (c in id) {
            bos.write(c.toInt())
        }
    }
}
