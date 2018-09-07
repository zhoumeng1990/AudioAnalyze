package com.zm.audio.kotlin.utils

/**
 * Created by ZhouMeng on 2018/9/4.
 * 数字转时分秒
 */

object TimeUtil {
    fun formatLongToTimeStr(time: Int): String {
        var hour = 0
        var minute = 0
        var second = time
        if (second > 60) {
            minute = second / 60         //取整
            second %= 60         //取余
        }

        if (minute > 60) {
            hour = minute / 60
            minute %= 60
        }
        return ((if (hour < 10) "0$hour" else hour).toString() + ":" + (if (minute < 10) "0$minute" else minute) + ":"
                + if (second < 10) "0$second" else second)
    }
}
