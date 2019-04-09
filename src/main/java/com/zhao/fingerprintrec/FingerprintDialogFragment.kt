package com.zhao.fingerprintrec

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import android.support.v4.os.CancellationSignal
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast

import javax.crypto.Cipher

@TargetApi(23)
class FingerprintDialogFragment<T: Activity> : DialogFragment() {

    private var fingerprintManager: FingerprintManagerCompat? = null

    private var mCancellationSignal: CancellationSignal? = null

    private var mCipher: Cipher? = null

    private var mActivity: T? = null

    private var errorMsg: TextView? = null

    /**
     * 标识是否是用户主动取消的认证。
     */
    private var isSelfCancelled: Boolean = false

    fun setCipher(cipher: Cipher) {
        mCipher = cipher
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mActivity = activity as T?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fingerprintManager = FingerprintManagerCompat.from(mActivity!!)
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fingerprint_dialog, container, false)
        errorMsg = v.findViewById(R.id.error_msg)
        v.findViewById<TextView>(R.id.cancel).setOnClickListener {
            dismiss()
            stopListening()
        }
        return v
    }

    override fun onResume() {
        super.onResume()
        // 开始指纹认证监听
        startListening(mCipher)
    }

    override fun onPause() {
        super.onPause()
        // 停止指纹认证监听
        stopListening()
    }

    private fun startListening(cipher: Cipher?) {
        isSelfCancelled = false
        mCancellationSignal = CancellationSignal()
        fingerprintManager!!.authenticate(
            FingerprintManagerCompat.CryptoObject(cipher!!),
            0,
            mCancellationSignal,
            object : FingerprintManagerCompat.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    if (!isSelfCancelled) {
                        errorMsg!!.text = errString
                        if (errorCode == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT) {
                            Toast.makeText(mActivity, errString, Toast.LENGTH_SHORT).show()
                            dismiss()
                        }
                    }
                }

                override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
                    errorMsg!!.text = helpString
                }

                override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
                    Toast.makeText(mActivity, "指纹认证成功", Toast.LENGTH_SHORT).show()
                    dismiss()
                }

                override fun onAuthenticationFailed() {
                    errorMsg!!.text = "指纹认证失败，请再试一次"
                }
            },
            null
        )
    }

    private fun stopListening() {
        if (mCancellationSignal != null) {
            mCancellationSignal!!.cancel()
            mCancellationSignal = null
            isSelfCancelled = true
        }
    }

}
