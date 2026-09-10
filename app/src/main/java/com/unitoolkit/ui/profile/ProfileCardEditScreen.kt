package com.unitoolkit.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.unitoolkit.core.model.PersonalProfile
import com.unitoolkit.core.model.PersonalInfoItem
import com.unitoolkit.ui.components.SectionLabel
import com.unitoolkit.ui.components.SimpleTopBar

@Composable
fun ProfileCardEditScreen(
    navController: NavController,
    vm: ProfileCardViewModel = viewModel(factory = ProfileCardViewModel.Factory),
) {
    val saved by vm.profile.collectAsState()
    var current by remember { mutableStateOf(PersonalProfile()) }
    var imageTarget by remember { mutableStateOf<ProfileImageKind?>(null) }

    LaunchedEffect(saved) { current = saved }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val target = imageTarget
        imageTarget = null
        if (target != null && uri != null) {
            vm.importImage(target, uri) { path ->
                current = when (target) {
                    ProfileImageKind.AVATAR -> current.copy(avatarPath = path)
                    ProfileImageKind.QQ_QR -> current.copy(qqQrPath = path)
                    ProfileImageKind.WECHAT_QR -> current.copy(wechatQrPath = path)
                }
            }
        }
    }

    fun pick(kind: ProfileImageKind) {
        imageTarget = kind
        picker.launch("image/*")
    }

    Scaffold(
        topBar = {
            SimpleTopBar(
                title = "编辑个人名片",
                onBack = { navController.popBackStack() },
                trailing = {
                    TextButton(onClick = {
                        vm.save(
                            current.copy(
                                name = current.name.trim(),
                                phone = current.phone.trim(),
                                qq = current.qq.trim(),
                                bilibiliName = current.bilibiliName.trim(),
                                bilibiliUid = current.bilibiliUid.trim(),
                                customInfo = current.customInfo.mapNotNull {
                                    val label = it.label.trim()
                                    val value = it.value.trim()
                                    if (label.isNotBlank() || value.isNotBlank()) PersonalInfoItem(label, value) else null
                                },
                            ),
                        )
                        navController.popBackStack()
                    }) { Text("保存", fontWeight = FontWeight.Bold) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionLabel("头像与二维码")
            ImageFieldRow(
                title = "头像",
                path = current.avatarPath,
                placeholder = "选择头像图片",
                onPick = { pick(ProfileImageKind.AVATAR) },
                onClear = { current = current.copy(avatarPath = "") },
            )
            ImageFieldRow(
                title = "QQ 二维码",
                path = current.qqQrPath,
                placeholder = "选择 QQ 二维码截图",
                onPick = { pick(ProfileImageKind.QQ_QR) },
                onClear = { current = current.copy(qqQrPath = "") },
            )
            ImageFieldRow(
                title = "微信二维码",
                path = current.wechatQrPath,
                placeholder = "选择微信二维码截图",
                onPick = { pick(ProfileImageKind.WECHAT_QR) },
                onClear = { current = current.copy(wechatQrPath = "") },
            )

            Spacer(Modifier.height(8.dp))
            SectionLabel("基本信息")
            OutlinedTextField(
                value = current.name,
                onValueChange = { current = current.copy(name = it) },
                label = { Text("姓名 / 称呼") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = current.phone,
                onValueChange = { current = current.copy(phone = it) },
                label = { Text("手机号") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = current.qq,
                onValueChange = { current = current.copy(qq = it) },
                label = { Text("QQ 号") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = current.bilibiliName,
                onValueChange = { current = current.copy(bilibiliName = it) },
                label = { Text("B站昵称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = current.bilibiliUid,
                onValueChange = { current = current.copy(bilibiliUid = it) },
                label = { Text("B站 UID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
            SectionLabel("自定义信息栏")
            current.customInfo.forEachIndexed { index, item ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = item.label,
                        onValueChange = { v ->
                            current = current.copy(
                                customInfo = current.customInfo.toMutableList().apply { this[index] = item.copy(label = v) },
                            )
                        },
                        label = { Text("名称") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = item.value,
                        onValueChange = { v ->
                            current = current.copy(
                                customInfo = current.customInfo.toMutableList().apply { this[index] = item.copy(value = v) },
                            )
                        },
                        label = { Text("内容") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f),
                    )
                    TextButton(
                        onClick = {
                            current = current.copy(customInfo = current.customInfo.filterIndexed { i, _ -> i != index })
                        },
                    ) { Text("删", color = MaterialTheme.colorScheme.error) }
                }
            }
            TextButton(onClick = { current = current.copy(customInfo = current.customInfo + PersonalInfoItem()) }) {
                Text("＋ 添加自定义信息")
            }
        }
    }
}

@Composable
private fun ImageFieldRow(
    title: String,
    path: String,
    placeholder: String,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (path.isNotBlank()) {
                LocalFileImage(path = path, modifier = Modifier.fillMaxSize(), contentDescription = title)
            } else {
                Text("未选", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(placeholder, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (path.isNotBlank()) {
            TextButton(onClick = onClear) { Text("清除", color = MaterialTheme.colorScheme.error) }
        }
        TextButton(onClick = onPick) { Text("选择") }
    }
}
