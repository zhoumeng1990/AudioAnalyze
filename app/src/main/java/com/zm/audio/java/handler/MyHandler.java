package com.zm.audio.java.handler;

import android.os.Handler;
import android.os.Message;

import com.zm.audio.java.activity.MainActivity;

import java.lang.ref.WeakReference;

/**
 * Created by ZhouMeng on 2018/9/10.
 * 实际项目中的时候，可以在构造函数中把 BaseActivity 传过来，抽象 requestOver 方法，提高代码的可重用性
 */

public class MyHandler extends Handler{
    private final WeakReference<MainActivity> mActivity;

    /**
     * 从 MainActivity 中提取出来，原来是因为内部类会隐式强引用当前类，采用弱引用，避免长生命周期导致内存泄漏
     *
     * @param activity 宿主类
     */
    public MyHandler(MainActivity activity) {
        mActivity = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
        if (mActivity.get() != null) {
            mActivity.get().requestOver(msg);
        }
    }
}
