package com.zhao.fingerprintrec
import android.annotation.TargetApi
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import android.widget.Toast
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

@TargetApi(23)
object FingerRecoUtil {
    private const val DEFAULT_KEY_NAME = "default_key"
    private lateinit var keyStore: KeyStore
    fun supportFingerprint(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 23) {
            Toast.makeText(context.applicationContext, "您的系统版本过低，不支持指纹功能", Toast.LENGTH_SHORT).show()
            return false
        } else {
            val keyguardManager =
                context.applicationContext.getSystemService<KeyguardManager>(KeyguardManager::class.java)
            val fingerprintManager = FingerprintManagerCompat.from(context.applicationContext)
            if (!fingerprintManager.isHardwareDetected) {
                Toast.makeText(context.applicationContext, "您的手机不支持指纹功能", Toast.LENGTH_SHORT).show()
                return false
            } else if (!keyguardManager.isKeyguardSecure) {
                Toast.makeText(context.applicationContext, "您还未设置锁屏，请先设置锁屏并添加一个指纹", Toast.LENGTH_SHORT).show()
                return false
            } else if (!fingerprintManager.hasEnrolledFingerprints()) {
                Toast.makeText(context.applicationContext, "您至少需要在系统设置中添加一个指纹", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }
    fun getCipher():Cipher{
        initKey()
        return initCipher()
    }


    private fun initKey() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val builder = KeyGenParameterSpec.Builder(
                DEFAULT_KEY_NAME,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setUserAuthenticationRequired(true)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun initCipher():Cipher {
        try {
            val key = keyStore.getKey(DEFAULT_KEY_NAME, null) as SecretKey
            val cipher = Cipher.getInstance(
                KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            return cipher
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}