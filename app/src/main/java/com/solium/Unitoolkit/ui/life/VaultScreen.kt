package com.solium.Unitoolkit.ui.life

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.solium.Unitoolkit.core.model.VaultEntry
import com.solium.Unitoolkit.ui.components.EmptyHint
import com.solium.Unitoolkit.ui.components.SectionLabel
import com.solium.Unitoolkit.ui.components.SimpleTopBar

private const val MIN_PIN_LENGTH = 4
private const val MAX_PIN_LENGTH = 8

@Composable
fun VaultScreen(navController: NavController, vm: VaultViewModel = viewModel(factory = VaultViewModel.Factory)) {
    val hasPin by vm.hasPin.collectAsState()
    val unlocked by vm.unlocked.collectAsState()
    val entries by vm.entries.collectAsState()
    val message by vm.message.collectAsState()
    val busy by vm.busy.collectAsState()
    val lockRemaining by vm.lockRemainingMs.collectAsState()
    val attemptsLeft by vm.attemptsLeft.collectAsState()

    var editing by remember { mutableStateOf<VaultEntry?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<VaultEntry?>(null) }
    var changingPin by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            SimpleTopBar(
                title = "账号密码本",
                onBack = { navController.popBackStack() },
                trailing = {
                    if (unlocked) {
                        TextButton(onClick = { vm.lock() }) { Text("锁定") }
                    }
                },
            )
        },
        floatingActionButton = {
            if (unlocked) {
                FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Default.Add, contentDescription = "新增账号") }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
        ) {
            when {
                hasPin == null -> EmptyHint("正在读取…")
                hasPin == false -> SetupPanel(busy = busy, message = message, onSetup = { vm.setupPin(it) })
                !unlocked -> UnlockPanel(
                    busy = busy,
                    message = message,
                    lockRemaining = lockRemaining,
                    attemptsLeft = attemptsLeft,
                    onUnlock = { vm.unlock(it) },
                    onInput = { vm.clearMessage() },
                )
                else -> UnlockedPanel(
                    entries = entries,
                    message = message,
                    onEdit = { editing = it },
                    onDelete = { deleting = it },
                    onChangePin = { changingPin = true },
                    onCopy = { label, value -> copyToClipboard(context, label, value) },
                )
            }
        }
    }

    if (creating || editing != null) {
        VaultEntryDialog(
            initial = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { e ->
                if (editing == null) vm.addEntry(e) else vm.updateEntry(e)
                creating = false; editing = null
            },
        )
    }
    deleting?.let { e ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除账号") },
            text = { Text("确定删除「${e.platform}」的账号记录吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = { vm.deleteEntry(e.id); deleting = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } },
        )
    }
    if (changingPin) {
        ChangePinDialog(
            onDismiss = { changingPin = false },
            onConfirm = { oldPin, newPin -> vm.changePin(oldPin, newPin); changingPin = false },
        )
    }
}

/** 首次使用：设置 PIN。 */
@Composable
private fun SetupPanel(busy: Boolean, message: String, onSetup: (String) -> Unit) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf(0) } // 0=输入新 PIN，1=确认
    var error by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Text("设置密码本 PIN", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "密码使用 AES-256-GCM 加密后保存在本机，PIN 不会被保存。忘记 PIN 将无法恢复已存密码，请牢记。",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
        )
        PinPad(
            title = if (stage == 0) "输入新的 PIN（$MIN_PIN_LENGTH-$MAX_PIN_LENGTH 位）" else "再次输入以确认",
            value = if (stage == 0) first else second,
            onValueChange = {
                error = ""
                if (stage == 0) first = it else second = it
            },
            onConfirm = {
                if (stage == 0) {
                    if (first.length < MIN_PIN_LENGTH) error = "PIN 至少 $MIN_PIN_LENGTH 位"
                    else stage = 1
                } else {
                    if (second != first) {
                        error = "两次输入不一致，请重新输入"
                        second = ""
                    } else {
                        onSetup(first)
                    }
                }
            },
            confirmEnabled = (if (stage == 0) first else second).length >= MIN_PIN_LENGTH,
            hint = if (error.isBlank()) message else "",
            error = error,
            busy = busy,
        )
    }
}

/** 解锁：应用内简易键盘输入 PIN。 */
@Composable
private fun UnlockPanel(
    busy: Boolean,
    message: String,
    lockRemaining: Long,
    attemptsLeft: Int,
    onUnlock: (String) -> Unit,
    onInput: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    val locked = lockRemaining > 0

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp),
        )
        Text("密码本已锁定", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 8.dp))
        Text(
            if (locked) "错误次数过多，请稍后再试"
            else "输入 PIN 解锁（1 小时内最多 5 次机会）",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        PinPad(
            title = "",
            value = pin,
            onValueChange = { pin = it; onInput() },
            onConfirm = { if (pin.length >= MIN_PIN_LENGTH) onUnlock(pin) },
            confirmEnabled = pin.length >= MIN_PIN_LENGTH && !locked,
            hint = if (locked) "已锁定，剩余 ${formatLock(lockRemaining)}" else "剩余尝试次数：$attemptsLeft",
            error = message,
            busy = busy,
            enabled = !locked,
        )
    }
}

@Composable
private fun UnlockedPanel(
    entries: List<VaultEntry>,
    message: String,
    onEdit: (VaultEntry) -> Unit,
    onDelete: (VaultEntry) -> Unit,
    onChangePin: () -> Unit,
    onCopy: (String, String) -> Unit,
) {
    val revealed = remember { mutableStateOf<Set<String>>(emptySet()) }

    Column(Modifier.fillMaxSize()) {
        if (message.isNotBlank()) {
            Text(message, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 6.dp))
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("共 ${entries.size} 条记录", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            TextButton(onClick = onChangePin) { Text("修改 PIN", fontSize = 13.sp) }
        }
        if (entries.isEmpty()) {
            EmptyHint("还没有账号记录，点击右下角添加")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                items(entries, key = { it.id }) { e ->
                    val show = e.id in revealed.value
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(e.platform, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { onEdit(e) }) { Text("编辑", fontSize = 13.sp) }
                            IconButton(onClick = { onDelete(e) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        CopyRow("账号", e.account, onCopy)
                        CopyRow("密码", if (show) e.password else "••••••••", onCopy, copyValue = e.password) {
                            revealed.value = if (show) revealed.value - e.id else revealed.value + e.id
                        }
                        if (e.note.isNotBlank()) {
                            Text(e.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CopyRow(label: String, display: String, onCopy: (String, String) -> Unit, copyValue: String = display, onToggle: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(40.dp))
        Text(display, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (onToggle != null) {
            TextButton(onClick = onToggle) { Text(if (display.startsWith("•")) "显示" else "隐藏", fontSize = 12.sp) }
        }
        TextButton(onClick = { onCopy(label, copyValue) }) { Text("复制", fontSize = 12.sp) }
    }
}

/** 应用内简易数字键盘：1-9 / 0 / 退格 / 清空 + 确认。 */
@Composable
private fun PinPad(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean,
    hint: String,
    error: String,
    busy: Boolean,
    enabled: Boolean = true,
) {
    if (title.isNotBlank()) {
        Text(title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
    }
    // 已输入位数的圆点
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
        repeat(value.length.coerceAtLeast(1)) { i ->
            Box(
                Modifier.size(12.dp).clip(CircleShape)
                    .background(if (i < value.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
    if (error.isNotBlank()) {
        Text(error, fontSize = 12.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
    } else if (hint.isNotBlank()) {
        Text(hint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
    }

    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "清空", "0", "⌫")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { k ->
                    Box(
                        Modifier.size(width = 78.dp, height = 52.dp).clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(enabled = enabled) {
                                when (k) {
                                    "清空" -> onValueChange("")
                                    "⌫" -> onValueChange(value.dropLast(1))
                                    else -> if (value.length < MAX_PIN_LENGTH) onValueChange(value + k)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(k, fontSize = if (k.length > 1) 14.sp else 20.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(if (confirmEnabled && !busy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = confirmEnabled && !busy) { onConfirm() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (busy) "处理中…" else "确认",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (confirmEnabled && !busy) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun VaultEntryDialog(initial: VaultEntry?, onDismiss: () -> Unit, onSave: (VaultEntry) -> Unit) {
    var platform by remember { mutableStateOf(initial?.platform ?: "") }
    var account by remember { mutableStateOf(initial?.account ?: "") }
    var password by remember { mutableStateOf(initial?.password ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var showPassword by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(if (initial == null) "新增账号" else "编辑账号", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(value = platform, onValueChange = { platform = it }, label = { Text("平台名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = account, onValueChange = { account = it }, label = { Text("账号") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                singleLine = true,
                // 使用系统安全键盘（Password 类型），避免普通输入法记录 / 联想密码
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "隐藏" else "显示", fontSize = 12.sp) } },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(
                    onClick = { onSave(VaultEntry(id = initial?.id ?: "", platform = platform.trim(), account = account.trim(), password = password, note = note.trim())) },
                    enabled = platform.isNotBlank() && (account.isNotBlank() || password.isNotBlank()),
                ) { Text("保存", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun ChangePinDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val valid = oldPin.length >= MIN_PIN_LENGTH && newPin.length >= MIN_PIN_LENGTH && newPin == confirmPin

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("修改 PIN", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "需先验证原 PIN；原 PIN 错误时不会做任何改动，已存密码不会丢失。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = oldPin,
                onValueChange = { oldPin = it },
                label = { Text("原 PIN") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = newPin,
                onValueChange = { newPin = it },
                label = { Text("新 PIN（至少 $MIN_PIN_LENGTH 位）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { confirmPin = it },
                label = { Text("再次输入新 PIN") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                isError = confirmPin.isNotBlank() && confirmPin != newPin,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = { onConfirm(oldPin, newPin) }, enabled = valid) { Text("确认修改", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

private fun formatLock(ms: Long): String {
    val minutes = ms / 60000 + 1
    return if (minutes >= 60) "${minutes / 60} 小时" else "$minutes 分钟"
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    if (value.isBlank()) return
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText(label, value))
}
