package com.solium.Unitoolkit.core.utils

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 账号密码本的本地加密：AES-256-GCM + PBKDF2WithHmacSHA256。
 *
 * - 随机盐（16 字节）+ 高迭代次数从 PIN 派生密钥，PIN 本身从不落盘；
 * - GCM 自带认证标签：PIN 错误时解密直接失败，可明确提示「PIN 错误」，不会产生乱码；
 * - 修改 PIN 时先用原 PIN 解密校验，校验失败即拒绝，不会导致已存密码丢失。
 */
object VaultCrypto {

    /** PBKDF2 迭代次数；迭代越高越难暴力破解，代价是派生耗时略增。 */
    const val ITERATIONS = 120_000

    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    /** 用于校验 PIN 是否正确的固定明文；加密后作为「验证器」保存。 */
    private const val VERIFIER_TEXT = "unitoolkit-vault-verifier-v1"

    private val random = SecureRandom()

    fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /** 由 PIN + 盐派生 AES 密钥。 */
    fun deriveKey(pin: String, saltBase64: String, iterations: Int = ITERATIONS): SecretKey {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations.coerceAtLeast(1), KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    /** 加密：返回 base64(iv + 密文 + 认证标签)；失败返回 null。 */
    fun encrypt(key: SecretKey, plain: String): String? = runCatching {
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        Base64.encodeToString(iv + body, Base64.NO_WRAP)
    }.getOrNull()

    /** 解密：密钥（PIN）不正确时认证失败，返回 null，不会得到乱码。 */
    fun decrypt(key: SecretKey, payload: String): String? {
        if (payload.isBlank()) return null
        val raw = runCatching { Base64.decode(payload, Base64.NO_WRAP) }.getOrNull() ?: return null
        if (raw.size <= IV_BYTES) return null
        return runCatching {
            val iv = raw.copyOfRange(0, IV_BYTES)
            val body = raw.copyOfRange(IV_BYTES, raw.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(body), Charsets.UTF_8)
        }.getOrNull()
    }

    /** 生成密文形式的验证器，用于后续判断 PIN 是否正确。 */
    fun makeVerifier(key: SecretKey): String? = encrypt(key, VERIFIER_TEXT)

    /** 校验密钥是否与验证器匹配。 */
    fun checkVerifier(key: SecretKey, verifier: String): Boolean =
        verifier.isNotBlank() && decrypt(key, verifier) == VERIFIER_TEXT
}
