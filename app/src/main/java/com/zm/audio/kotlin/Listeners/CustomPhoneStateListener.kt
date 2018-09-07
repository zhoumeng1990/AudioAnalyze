package com.zm.audio.kotlin.Listeners

import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager

import com.zm.audio.kotlin.interfaces.IPhoneState

/**
 * Created by ZhouMeng on 2018/9/7.
 * 监听电话
 * 因为只用到来电监听，别的没做处理
 */

class CustomPhoneStateListener(private val iPhoneState: IPhoneState) : PhoneStateListener() {

    override fun onCallStateChanged(state: Int, incomingNumber: String) {
        super.onCallStateChanged(state, incomingNumber)
        when (state) {
        /*
             * 无操作
             */
            TelephonyManager.CALL_STATE_IDLE -> {
            }
        /*
             * 电话响铃
             */
            TelephonyManager.CALL_STATE_RINGING -> iPhoneState.phone()
        /*
             * 来电接通 或者 去电，去电接通  但是没法区分
             */
            TelephonyManager.CALL_STATE_OFFHOOK -> {
            }
        }
    }
}
