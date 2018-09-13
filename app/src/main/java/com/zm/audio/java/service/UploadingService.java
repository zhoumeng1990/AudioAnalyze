package com.zm.audio.java.service;


import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by ZhouMeng on 2018/9/11.
 * 上传大文件
 */

public class UploadingService extends IntentService {
    private final static String TAG = "UploadingService";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     * <p>
     * //     * @param name Used to name the worker thread, important only for debugging.
     */
    public UploadingService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG,"上传操作");
    }
}