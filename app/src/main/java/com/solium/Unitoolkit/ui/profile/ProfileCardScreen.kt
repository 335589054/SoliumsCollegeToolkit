package com.solium.Unitoolkit.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.solium.Unitoolkit.core.model.PersonalProfile
import com.solium.Unitoolkit.ui.components.AppIcon
import com.solium.Unitoolkit.ui.components.PrimaryButton
import com.solium.Unitoolkit.ui.components.SimpleTopBar
import com.solium.Unitoolkit.ui.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun ProfileCardScreen(
    navController: NavController,
    vm: ProfileCardViewModel = viewModel(factory = ProfileCardViewModel.Factory),
) {
    val profile by vm.profile.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val onCopy: (String) -> Unit = { text ->
        if (text.isNotBlank()) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("profile", text))
            scope.launch { snackbarHostState.showSnackbar("已复制") }
        }
    }
    Scaffold(
        topBar = {
            SimpleTopBar(
                title = "个人名片",
                onBack = { navController.popBackStack() },
                trailing = {
                    TextButton(onClick = { navController.navigate(Routes.PROFILE_EDIT) }) {
                        Text("编辑")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            val compact = maxHeight < 560.dp
            if (hasProfileContent(profile)) {
                PersonalCardBody(
                    profile = profile,
                    compact = compact,
                    avatarSize = if (compact) 58.dp else 76.dp,
                    qrSize = if (compact) 80.dp else 112.dp,
                    gap = if (compact) 4.dp else 8.dp,
                    onCopy = onCopy,
                )
            } else {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AppIcon("user", size = 56.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "还没有个人名片信息",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(
                        label = "去设置填写",
                        onClick = { navController.navigate(Routes.PROFILE_EDIT) },
                    )
                }
            }
        }
    }
}

private fun hasProfileContent(p: PersonalProfile): Boolean =
    p.name.isNotBlank() ||
        p.phone.isNotBlank() ||
        p.qq.isNotBlank() ||
        p.bilibiliName.isNotBlank() ||
        p.bilibiliUid.isNotBlank() ||
        p.customInfo.any { it.label.isNotBlank() || it.value.isNotBlank() } ||
        p.avatarPath.isNotBlank() ||
        p.qqQrPath.isNotBlank() ||
        p.wechatQrPath.isNotBlank()

@Composable
private fun PersonalCardBody(
    profile: PersonalProfile,
    compact: Boolean,
    avatarSize: androidx.compose.ui.unit.Dp,
    qrSize: androidx.compose.ui.unit.Dp,
    gap: androidx.compose.ui.unit.Dp,
    onCopy: (String) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(if (compact) 12.dp else 18.dp),
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            ProfileHeader(profile, compact, avatarSize, onCopy)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                ProfileInfoCell("手机号", profile.phone, Modifier.weight(1f), compact, onCopy)
                ProfileInfoCell("QQ", profile.qq, Modifier.weight(1f), compact, onCopy)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                BilibiliCell(profile.bilibiliName, profile.bilibiliUid, Modifier.weight(1f), compact, onCopy)
                if (profile.customInfo.isNotEmpty()) {
                    val first = profile.customInfo.firstOrNull { it.label.isNotBlank() || it.value.isNotBlank() }
                    if (first != null) {
                        ProfileInfoCell(
                            first.label.ifBlank { "自定义" },
                            first.value,
                            Modifier.weight(1f),
                            compact,
                            onCopy,
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
            val extra = profile.customInfo.filter { it.label.isNotBlank() || it.value.isNotBlank() }.drop(1)
            extra.chunked(2).forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    rowItems.forEach { item ->
                        ProfileInfoCell(item.label.ifBlank { "自定义" }, item.value, Modifier.weight(1f), compact, onCopy)
                    }
                    repeat(2 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            if (profile.qqQrPath.isNotBlank() || profile.wechatQrPath.isNotBlank()) {
                Spacer(Modifier.height(if (compact) 2.dp else 6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
                ) {
                    if (profile.qqQrPath.isNotBlank()) {
                        QrImage("QQ 二维码", profile.qqQrPath, qrSize, compact, Modifier.weight(1f))
                    }
                    if (profile.wechatQrPath.isNotBlank()) {
                        QrImage("微信二维码", profile.wechatQrPath, qrSize, compact, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: PersonalProfile,
    compact: Boolean,
    avatarSize: androidx.compose.ui.unit.Dp,
    onCopy: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ProfileAvatar(profile.avatarPath, avatarSize)
        Spacer(Modifier.width(if (compact) 10.dp else 14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                profile.name.ifBlank { "个人名片" },
                fontSize = if (compact) 18.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = listOfNotNull(
                profile.bilibiliName.ifBlank { null },
                profile.bilibiliUid.ifBlank { null }?.let { "UID $it" },
            ).joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    fontSize = if (compact) 11.sp else 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onCopy(subtitle) },
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(path: String, size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (path.isNotBlank()) {
            LocalFileImage(
                path = path,
                modifier = Modifier.fillMaxSize(),
                contentDescription = "头像",
            )
        } else {
            AppIcon("user", size = size * 0.55f, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ProfileInfoCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    compact: Boolean,
    onCopy: (String) -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .padding(horizontal = 8.dp, vertical = if (compact) 5.dp else 8.dp)
            .clickable(enabled = value.isNotBlank()) { onCopy(value) },
    ) {
        Text(
            label,
            fontSize = if (compact) 9.sp else 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value.ifBlank { "未填写" },
            fontSize = if (compact) 11.sp else 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** B站信息格：昵称一行、UID 单独一行（UID 不截断），均可点击复制。 */
@Composable
private fun BilibiliCell(
    name: String,
    uid: String,
    modifier: Modifier = Modifier,
    compact: Boolean,
    onCopy: (String) -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .padding(horizontal = 8.dp, vertical = if (compact) 5.dp else 8.dp),
    ) {
        Text(
            "B站",
            fontSize = if (compact) 9.sp else 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            name.ifBlank { "未填写" },
            fontSize = if (compact) 11.sp else 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (name.isNotBlank()) Modifier.clickable { onCopy(name) } else Modifier,
        )
        if (uid.isNotBlank()) {
            Text(
                "UID $uid",
                fontSize = if (compact) 11.sp else 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onCopy(uid) },
            )
        }
    }
}

@Composable
private fun QrImage(
    label: String,
    path: String,
    size: androidx.compose.ui.unit.Dp,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(size)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            LocalFileImage(path = path, modifier = Modifier.fillMaxSize(), contentDescription = label)
        }
        Spacer(Modifier.height(if (compact) 2.dp else 5.dp))
        Text(
            label,
            fontSize = if (compact) 10.sp else 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
