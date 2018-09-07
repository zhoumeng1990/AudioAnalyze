package com.zm.audio.java.Listeners;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.zm.audio.java.interfaces.IPhoneState;

/**
 * Created by ZhouMeng on 2018/9/7.
 * 监听电话
 * 因为只用到来电监听，别的没做处理
 */

public class CustomPhoneStateListener extends PhoneStateListener {
    private IPhoneState iPhoneState;

    public CustomPhoneStateListener(IPhoneState iPhoneState) {
        this.iPhoneState = iPhoneState;
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        super.onCallStateChanged(state, incomingNumber);
        switch (state) {
            /*
             * 无操作
             */
            case TelephonyManager.CALL_STATE_IDLE:
                break;
            /*
             * 电话响铃
             */
            case TelephonyManager.CALL_STATE_RINGING:
                iPhoneState.phone();
                break;
            /*
             * 来电接通 或者 去电，去电接通  但是没法区分
             */
            case TelephonyManager.CALL_STATE_OFFHOOK:
                break;
        }
    }
}
