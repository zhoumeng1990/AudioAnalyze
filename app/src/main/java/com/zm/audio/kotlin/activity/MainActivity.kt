package com.zm.audio.kotlin.activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.zm.audio.R
import com.zm.audio.kotlin.Listeners.CustomPhoneStateListener
import com.zm.audio.kotlin.handler.MyHandler
import com.zm.audio.kotlin.interfaces.IAudioCallback
import com.zm.audio.kotlin.interfaces.IPhoneState
import com.zm.audio.kotlin.utils.AudioRecorder
import com.zm.audio.kotlin.utils.AudioStatus
import com.zm.audio.kotlin.utils.TimeUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by ZhouMeng on 2018/9/5.
 * 主页，主要用来测试功能
 */

class MainActivity : AppCompatActivity(), View.OnClickListener, IAudioCallback, IPhoneState {

    private var tvRecordTime: TextView? = null
    private var llReset: LinearLayout? = null
    private var ivController: ImageView? = null
    private var llFinish: LinearLayout? = null

    private var audioRecorder: AudioRecorder? = null
    private var isKeepTime: Boolean = false
    /**
     * 支持定时和周期性执行的线程池
     */
    private val scheduledThreadPool = Executors.newScheduledThreadPool(1)
    private var time: Int = 0

    // 声明一个集合，在后面的代码中用来存储用户拒绝授权的权
    private val mPermissionList = ArrayList<String>()

    private val myHandler = MyHandler(this)

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        setClickListener(llReset, ivController, llFinish)
        initData()
        registerPhoneStateListener()
    }

    /**
     * 初始化view
     */
    private fun initView() {
        tvRecordTime = findViewById(R.id.tv_record_time)
        llReset = findViewById(R.id.ll_reset)
        ivController = findViewById(R.id.iv_controller)
        llFinish = findViewById(R.id.ll_finish)
    }

    /**
     * 遍历设置监听
     *
     * @param views
     */
    private fun setClickListener(vararg views: View?) {
        for (view in views) {
            view?.setOnClickListener(this)
        }
    }

    /**
     * 初始化数据
     */
    private fun initData() {
        setClickable(false)
        audioRecorder = AudioRecorder.getInstance(this)
        scheduledThreadPool.scheduleAtFixedRate({
            if (isKeepTime) {
                ++time
                myHandler.sendEmptyMessage(HANDLER_CODE)
            }
        }, INITIAL_DELAY.toLong(), PERIOD.toLong(), TimeUnit.MILLISECONDS)

        setPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_SETTINGS, Manifest.permission.RECORD_AUDIO),
                ACCESS_FINE_ERROR_CODE)
    }

    /**
     * 权限
     */
    private fun setPermissions(permissions: Array<String>, permissionsCode: Int) {
        var permissions = permissions
        mPermissionList.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    mPermissionList.add(permission)
                }
            }

            //未授予的权限为空，表示都授予了
            if (mPermissionList.isEmpty()) {
                showToast("已经授权")
            } else {
                //将List转为数组
                permissions = mPermissionList.toTypedArray()
                ActivityCompat.requestPermissions(this, permissions, permissionsCode)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (i in grantResults.indices) {
            val showRequestPermission = ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])
            if (showRequestPermission) {
                showToast("权限未申请")
            }
        }
    }

    protected fun showToast(toastInfo: String) {
        Toast.makeText(this, toastInfo, Toast.LENGTH_LONG).show()
    }

    /**
     * 注册并监听电话状态
     */
    private fun registerPhoneStateListener() {
        val customPhoneStateListener = CustomPhoneStateListener(this)
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(customPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    /**
     * 设置重置和完成是否可以点击
     * @param clickable true可以点击
     */
    private fun setClickable(clickable: Boolean) {
        llReset!!.isClickable = clickable
        llFinish!!.isClickable = clickable
    }

    override fun onPause() {
        super.onPause()
        if (audioRecorder!!.status == AudioStatus.STATUS_START) {
            phoneToPause()
        }
    }

    /**
     * 暂停录音和状态修改
     */
    private fun phoneToPause() {
        audioRecorder!!.pauseRecord()
        ivController!!.setImageResource(R.drawable.icon_stop)
        isKeepTime = false
    }

    override fun onDestroy() {
        audioRecorder!!.release()
        scheduledThreadPool.shutdown()
        super.onDestroy()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.iv_controller -> try {
                if (audioRecorder!!.status == AudioStatus.STATUS_NO_READY) {
                    //初始化录音
                    val fileName = SimpleDateFormat("yyyyMMddhhmmss", Locale.CHINA).format(Date())
                    audioRecorder!!.createDefaultAudio(fileName)
                    audioRecorder!!.startRecord()
                    ivController!!.setImageResource(R.drawable.icon_start)
                    isKeepTime = true
                    setClickable(true)
                } else {
                    if (audioRecorder!!.status == AudioStatus.STATUS_START) {
                        phoneToPause()
                    } else {
                        audioRecorder!!.startRecord()
                        ivController!!.setImageResource(R.drawable.icon_start)
                        isKeepTime = true
                    }
                }
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }

            R.id.ll_finish -> finishAndReset()

            R.id.ll_reset -> {
                audioRecorder!!.setReset()
                finishAndReset()
            }
        }
    }

    private fun finishAndReset() {
        isKeepTime = false
        audioRecorder!!.stopRecord()
        ivController!!.setImageResource(R.drawable.icon_stop)
        time = 0
        tvRecordTime!!.text = "00:00:00"
        setClickable(false)
    }

    override fun showPlay(filePath: String) {
        /*
         * 合成完后的操作，根据需要去做处理，此处用于测试播放
         */
        val file = File(filePath)
        if (file.exists()) {
            audioRecorder!!.play(filePath)
        }
    }

    override fun phone() {
        if (audioRecorder!!.status == AudioStatus.STATUS_START) {
            phoneToPause()
        }
    }

    fun requestOver(msg: Message) {
        when (msg.what) {
            HANDLER_CODE -> tvRecordTime!!.text = TimeUtil.formatLongToTimeStr(time)
        }
    }

    companion object {
        private const val INITIAL_DELAY = 0
        private const val PERIOD = 1000
        private const val ACCESS_FINE_ERROR_CODE = 0x0245
        private const val HANDLER_CODE = 0x0249
    }
}