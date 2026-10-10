package com.solium.Unitoolkit.ui.life

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solium.Unitoolkit.UniToolkitApp
import com.solium.Unitoolkit.core.model.VaultEntry
import com.solium.Unitoolkit.core.utils.VaultCrypto
import com.solium.Unitoolkit.core.utils.appFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.crypto.SecretKey

/** 账号密码本：解锁 / 增删改 / 修改 PIN。密码内容整体使用 AES-GCM 加密后落盘。 */
class VaultViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = (app as UniToolkitApp).settings
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** null 表示还在读取本地状态（未设置 PIN / 已设置）。 */
    private val _hasPin = MutableStateFlow<Boolean?>(null)
    val hasPin: StateFlow<Boolean?> = _hasPin.asStateFlow()

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    private val _entries = MutableStateFlow<List<VaultEntry>>(emptyList())
    val entries: StateFlow<List<VaultEntry>> = _entries.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** 锁定剩余毫秒；> 0 时禁止输入。 */
    private val _lockRemainingMs = MutableStateFlow(0L)
    val lockRemainingMs: StateFlow<Long> = _lockRemainingMs.asStateFlow()

    private val _attemptsLeft = MutableStateFlow(MAX_ATTEMPTS)
    val attemptsLeft: StateFlow<Int> = _attemptsLeft.asStateFlow()

    private var key: SecretKey? = null

    init {
        viewModelScope.launch { refreshLockState() }
    }

    fun clearMessage() {
        _message.value = ""
    }

    /** 计算当前是否处于「1 小时内错误次数过多」的锁定状态。 */
    private suspend fun refreshLockState() {
        _hasPin.value = settings.vaultSalt.first().isNotBlank()
        val count = settings.vaultFailCount.first()
        val since = settings.vaultFailSince.first()
        val remaining = lockRemaining(count, since)
        _lockRemainingMs.value = remaining
        _attemptsLeft.value = (MAX_ATTEMPTS - count).coerceAtLeast(0)
    }

    private fun lockRemaining(count: Int, since: Long): Long {
        if (count < MAX_ATTEMPTS || since <= 0L) return 0L
        val elapsed = System.currentTimeMillis() - since
        return (WINDOW_MS - elapsed).coerceAtLeast(0L)
    }

    /** 首次设置 PIN：生成随机盐、派生密钥并把空列表加密写入。 */
    fun setupPin(pin: String) = viewModelScope.launch {
        _busy.value = true
        val ok = withContext(Dispatchers.Default) {
            runCatching {
                val salt = VaultCrypto.newSalt()
                val k = VaultCrypto.deriveKey(pin, salt)
                val verifier = VaultCrypto.makeVerifier(k) ?: return@runCatching false
                val blob = VaultCrypto.encrypt(k, encode(emptyList())) ?: return@runCatching false
                withContext(Dispatchers.IO) { settings.setVault(salt, VaultCrypto.ITERATIONS, verifier, blob) }
                key = k
                true
            }.getOrDefault(false)
        }
        _busy.value = false
        if (ok) {
            _hasPin.value = true
            _unlocked.value = true
            _entries.value = emptyList()
            _message.value = "已设置 PIN，密码本已创建"
        } else {
            _message.value = "初始化失败，请重试"
        }
    }

    /** 解锁：PIN 错误只提示，不产生乱码；连续错误超过上限会锁定 1 小时。 */
    fun unlock(pin: String) = viewModelScope.launch {
        val count = settings.vaultFailCount.first()
        val since = settings.vaultFailSince.first()
        val remaining = lockRemaining(count, since)
        if (remaining > 0) {
            _lockRemainingMs.value = remaining
            _message.value = "错误次数过多，请 ${formatRemaining(remaining)} 后再试"
            return@launch
        }
        if (since > 0L && System.currentTimeMillis() - since >= WINDOW_MS) {
            settings.setVaultFailState(0, 0L)
        }
        val windowExpired = since <= 0L || System.currentTimeMillis() - since >= WINDOW_MS
        val baseCount = if (windowExpired) 0 else count
        _busy.value = true
        val salt = settings.vaultSalt.first()
        val iterations = settings.vaultIterations.first().takeIf { it > 0 } ?: VaultCrypto.ITERATIONS
        val verifier = settings.vaultVerifier.first()
        val blob = settings.vaultBlob.first()
        val result = withContext(Dispatchers.Default) {
            runCatching {
                val k = VaultCrypto.deriveKey(pin, salt, iterations)
                if (!VaultCrypto.checkVerifier(k, verifier)) return@runCatching null
                k to (VaultCrypto.decrypt(k, blob) ?: "[]")
            }.getOrNull()
        }
        _busy.value = false
        if (result == null) {
            val nextCount = baseCount + 1
            val nextSince = if (baseCount == 0) System.currentTimeMillis() else since
            settings.setVaultFailState(nextCount, nextSince)
            _attemptsLeft.value = (MAX_ATTEMPTS - nextCount).coerceAtLeast(0)
            if (nextCount >= MAX_ATTEMPTS) {
                _lockRemainingMs.value = WINDOW_MS
                _message.value = "PIN 连续错误 $MAX_ATTEMPTS 次，已锁定 1 小时"
            } else {
                _message.value = "PIN 错误，还可尝试 ${MAX_ATTEMPTS - nextCount} 次"
            }
        } else {
            key = result.first
            _entries.value = decode(result.second)
            _unlocked.value = true
            _attemptsLeft.value = MAX_ATTEMPTS
            _lockRemainingMs.value = 0L
            _message.value = ""
        }
    }

    fun lock() {
        key = null
        _unlocked.value = false
        _entries.value = emptyList()
        _message.value = ""
    }

    fun addEntry(entry: VaultEntry) = viewModelScope.launch {
        persist(_entries.value + entry.copy(id = System.currentTimeMillis().toString()))
    }

    fun updateEntry(entry: VaultEntry) = viewModelScope.launch {
        persist(_entries.value.map { if (it.id == entry.id) entry else it })
    }

    fun deleteEntry(id: String) = viewModelScope.launch {
        persist(_entries.value.filterNot { it.id == id })
    }

    /** 修改 PIN：先用原 PIN 校验；原 PIN 错误则拒绝且不改动任何数据。 */
    fun changePin(oldPin: String, newPin: String) = viewModelScope.launch {
        val salt = settings.vaultSalt.first()
        val iterations = settings.vaultIterations.first().takeIf { it > 0 } ?: VaultCrypto.ITERATIONS
        val verifier = settings.vaultVerifier.first()
        if (salt.isBlank()) {
            _message.value = "尚未设置 PIN"
            return@launch
        }
        _busy.value = true
        val ok = withContext(Dispatchers.Default) {
            runCatching {
                val oldKey = VaultCrypto.deriveKey(oldPin, salt, iterations)
                if (!VaultCrypto.checkVerifier(oldKey, verifier)) return@runCatching false
                val newSalt = VaultCrypto.newSalt()
                val newKey = VaultCrypto.deriveKey(newPin, newSalt)
                val newVerifier = VaultCrypto.makeVerifier(newKey) ?: return@runCatching false
                val newBlob = VaultCrypto.encrypt(newKey, encode(_entries.value)) ?: return@runCatching false
                withContext(Dispatchers.IO) { settings.setVault(newSalt, VaultCrypto.ITERATIONS, newVerifier, newBlob) }
                key = newKey
                true
            }.getOrDefault(false)
        }
        _busy.value = false
        _message.value = if (ok) "PIN 已修改" else "原 PIN 错误，密码本未做任何改动"
    }

    /** 清空密码本（含 PIN），用于「清空数据」。 */
    fun wipe() = viewModelScope.launch {
        settings.clearVault()
        key = null
        _hasPin.value = false
        _unlocked.value = false
        _entries.value = emptyList()
    }

    private suspend fun persist(list: List<VaultEntry>) {
        val k = key
        if (k == null) {
            _message.value = "请先解锁"
            return
        }
        val blob = withContext(Dispatchers.Default) { VaultCrypto.encrypt(k, encode(list)) }
        if (blob == null) {
            _message.value = "保存失败"
            return
        }
        settings.setVaultBlob(blob)
        _entries.value = list
    }

    private fun encode(list: List<VaultEntry>): String =
        json.encodeToString(ListSerializer(VaultEntry.serializer()), list)

    private fun decode(raw: String): List<VaultEntry> =
        runCatching { json.decodeFromString(ListSerializer(VaultEntry.serializer()), raw) }.getOrDefault(emptyList())

    private fun formatRemaining(ms: Long): String {
        val totalMinutes = ms / 60000 + 1
        return if (totalMinutes >= 60) "${totalMinutes / 60} 小时" else "$totalMinutes 分钟"
    }

    companion object {
        const val MAX_ATTEMPTS = 5
        const val WINDOW_MS = 60L * 60L * 1000L
        val Factory = appFactory(::VaultViewModel)
    }
}
