package com.zm.audio.kotlin.service

import android.app.IntentService
import android.content.Intent
import android.util.Log

/**
 * Created by ZhouMeng on 2018/9/13.
 * 开启服务上传文件
 */
class UploadingService : IntentService(TAG) {

    override fun onHandleIntent(intent: Intent?) {
        Log.d(TAG,"上传操作")
    }

    companion object{
        private const val TAG :String = "UploadingService"
    }
}