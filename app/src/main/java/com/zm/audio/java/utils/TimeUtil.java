package com.zm.audio.java.utils;

/**
 * Created by ZhouMeng on 2018/9/4.
 * 数字转时分秒
 */
public class TimeUtil {
    public static String formatLongToTimeStr(int time) {
        int hour = 0;
        int minute = 0;
        int second = time;
        if (second > 60) {
            minute = second / 60;
            second %= 60;
        }

        if (minute > 60) {
            hour = minute / 60;
            minute %= 60;
        }
        return (hour < 10 ? "0" + hour : hour) + ":" + (minute < 10 ? "0" + minute : minute) + ":"
                + (second < 10 ? "0" + second : second);
    }
}
