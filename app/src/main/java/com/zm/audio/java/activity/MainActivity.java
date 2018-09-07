package com.zm.audio.java.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.zm.audio.R;
import com.zm.audio.java.Listeners.CustomPhoneStateListener;
import com.zm.audio.java.interfaces.IAudioCallback;
import com.zm.audio.java.interfaces.IPhoneState;
import com.zm.audio.java.utils.AudioRecorder;
import com.zm.audio.java.utils.AudioStatus;
import com.zm.audio.java.utils.TimeUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by ZhouMeng on 2018/9/5.
 * 主页，主要用来测试功能
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener, IAudioCallback, IPhoneState {

    private TextView tvRecordTime;
    private LinearLayout llReset;
    private ImageView ivController;
    private LinearLayout llFinish;

    private AudioRecorder audioRecorder;
    private boolean isKeepTime;
    /**
     * 支持定时和周期性执行的线程池
     */
    private ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);
    private int time;
    private static final int INITIAL_DELAY = 0;
    private static final int PERIOD = 1000;

    // 声明一个集合，在后面的代码中用来存储用户拒绝授权的权
    private List<String> mPermissionList = new ArrayList<>();
    private final static int ACCESS_FINE_ERROR_CODE = 0x0245;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        setClickListener(llReset, ivController, llFinish);
        initData();
        registerPhoneStateListener();
    }

    /**
     * 初始化view
     */
    private void initView() {
        tvRecordTime = findViewById(R.id.tv_record_time);
        llReset = findViewById(R.id.ll_reset);
        ivController = findViewById(R.id.iv_controller);
        llFinish = findViewById(R.id.ll_finish);
    }

    /**
     * 遍历设置监听
     *
     * @param views
     */
    private void setClickListener(View... views) {
        for (View view : views) {
            view.setOnClickListener(this);
        }
    }

    /**
     * 初始化数据
     */
    private void initData() {
        setClickable(false);
        audioRecorder = AudioRecorder.getInstance(this);
        scheduledThreadPool.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                if (isKeepTime) {
                    ++time;
                    tvRecordTime.setText(TimeUtil.formatLongToTimeStr(time));
                }
            }
        }, INITIAL_DELAY, PERIOD, TimeUnit.MILLISECONDS);

        setPermissions(new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_SETTINGS,
                Manifest.permission.RECORD_AUDIO},
                ACCESS_FINE_ERROR_CODE);
    }

    /**
     *权限
     */
    private void setPermissions(String[] permissions, int permissionsCode) {
        mPermissionList.clear();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    mPermissionList.add(permission);
                }
            }

            //未授予的权限为空，表示都授予了
            if (mPermissionList.isEmpty()) {
                showToast("已经授权");
            } else {
                //将List转为数组
                permissions = mPermissionList.toArray(new String[mPermissionList.size()]);
                ActivityCompat.requestPermissions(this, permissions, permissionsCode);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < grantResults.length; i++) {
            boolean showRequestPermission = ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i]);
            if (showRequestPermission) {
                showToast("权限未申请");
            }
        }
    }

    protected void showToast(String toastInfo) {
        Toast.makeText(this, toastInfo, Toast.LENGTH_LONG).show();
    }

    /**
     * 注册并监听电话状态
     */
    private void registerPhoneStateListener() {
        CustomPhoneStateListener customPhoneStateListener = new CustomPhoneStateListener( this);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            telephonyManager.listen(customPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    /**
     * 设置重置和完成是否可以点击
     * @param clickable true可以点击
     */
    private void setClickable(boolean clickable) {
        llReset.setClickable(clickable);
        llFinish.setClickable(clickable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (audioRecorder.getStatus() == AudioStatus.STATUS_START) {
            phoneToPause();
        }
    }

    /**
     * 暂停录音和状态修改
     */
    private void phoneToPause() {
        audioRecorder.pauseRecord();
        ivController.setImageResource(R.drawable.icon_stop);
        isKeepTime = false;
    }

    @Override
    protected void onDestroy() {
        audioRecorder.release();
        scheduledThreadPool.shutdown();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_controller:
                try {
                    if (audioRecorder.getStatus() == AudioStatus.STATUS_NO_READY) {
                        //初始化录音
                        String fileName = new SimpleDateFormat("yyyyMMddhhmmss", Locale.CHINA).format(new Date());
                        audioRecorder.createDefaultAudio(fileName);
                        audioRecorder.startRecord();
                        ivController.setImageResource(R.drawable.icon_start);
                        isKeepTime = true;
                        setClickable(true);
                    } else {
                        if (audioRecorder.getStatus() == AudioStatus.STATUS_START) {
                            phoneToPause();
                        } else {
                            audioRecorder.startRecord();
                            ivController.setImageResource(R.drawable.icon_start);
                            isKeepTime = true;
                        }
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.ll_finish:
                finishAndReset();
                break;

            case R.id.ll_reset:
                audioRecorder.setReset();
                finishAndReset();
                break;
        }
    }

    private void finishAndReset() {
        isKeepTime = false;
        audioRecorder.stopRecord();
        ivController.setImageResource(R.drawable.icon_stop);
        time = 0;
        tvRecordTime.setText("00:00:00");
        setClickable(false);
    }

    @Override
    public void showPlay(String filePath) {
        /*
         * 合成完后的操作，根据需要去做处理，此处用于测试播放
         */
        File file = new File(filePath);
        if (file.exists()) {
            audioRecorder.play(filePath);
        }
    }

    @Override
    public void phone() {
        if (audioRecorder.getStatus() == AudioStatus.STATUS_START) {
            phoneToPause();
        }
    }
}