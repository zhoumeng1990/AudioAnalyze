package com.zm.audio.kotlin.handler

import android.os.Handler
import android.os.Message

import com.zm.audio.kotlin.activity.MainActivity

import java.lang.ref.WeakReference

/**
 * Created by ZhouMeng on 2018/9/10.
 * 实际项目中的时候，可以在构造函数中把 BaseActivity 传过来，抽象 requestOver 方法，提高代码的可重用性
 */

class MyHandler
/**
 * 从 MainActivity 中提取出来，原来是因为内部类会隐式强引用当前类，采用弱引用，避免长生命周期导致内存泄漏
 *
 * @param activity
 */
(activity: MainActivity) : Handler() {
    private val mActivity: WeakReference<MainActivity> = WeakReference(activity)

    override fun handleMessage(msg: Message) {
        if (mActivity.get() != null) {
            mActivity.get()!!.requestOver(msg)
        }
    }
}
