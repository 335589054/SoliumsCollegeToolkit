package com.solium.Unitoolkit.core.database

import android.content.Context
import androidx.room.withTransaction
import com.solium.Unitoolkit.core.model.CoursePreset
import com.solium.Unitoolkit.core.model.CustomTheme
import com.solium.Unitoolkit.core.model.OverlayConfig
import com.solium.Unitoolkit.core.model.PeriodTiming
import com.solium.Unitoolkit.core.model.PersonalProfile
import com.solium.Unitoolkit.core.model.PomodoroPreset
import com.solium.Unitoolkit.core.model.GpaFormula
import com.solium.Unitoolkit.core.model.defaultGpaFormula
import com.solium.Unitoolkit.core.model.defaultPomodoroPresets
import com.solium.Unitoolkit.core.storage.SettingsRepository
import com.solium.Unitoolkit.core.theme.UiMode
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 备份文件的顶层结构：包含全部业务表的数据快照，采用纯文本 JSON 存储。 */
@Serializable
data class BackupData(
    val formatVersion: Int = 3,
    val exportedAt: Long = System.currentTimeMillis(),
    val courses: List<CourseEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val todos: List<TodoEntity> = emptyList(),
    val pomodoroRecords: List<PomodoroRecordEntity> = emptyList(),
    val gpaCourses: List<GpaCourseEntity> = emptyList(),
    val links: List<LinkEntity> = emptyList(),
    val ledgerEntries: List<LedgerEntryEntity> = emptyList(),
    val ledgerCategories: List<LedgerCategoryEntity> = emptyList(),
    val schedules: List<ScheduleEntity> = emptyList(),
    val overlayPresets: List<OverlayPresetEntity> = emptyList(),
    val shoppingActivities: List<ShoppingActivityEntity> = emptyList(),
    val shoppingItems: List<ShoppingItemEntity> = emptyList(),
    val personalProfile: PersonalProfile? = null,
    val overlayConfig: OverlayConfig? = null,
    /** 内嵌图片（base64），对应 PersonalProfile.avatarPath / qqQrPath / wechatQrPath。 */
    val profileAvatarBase64: String = "",
    val profileQqBase64: String = "",
    val profileWechatBase64: String = "",
    /** 内嵌上隐条遮罩图片（base64），对应 OverlayConfig.imagePath。 */
    val overlayImageBase64: String = "",
    val coursePresets: List<CoursePreset> = emptyList(),
    val pomodoroPresets: List<PomodoroPreset> = emptyList(),
    val customThemes: List<CustomTheme> = emptyList(),
    val selectedTheme: String = "",
    // v1.1.0 补齐此前遗漏的 DataStore 配置项，确保换机 / 还原后各项设置可完整恢复
    /** 深色模式（SYSTEM / LIGHT / DARK） */
    val uiMode: String = "",
    val keepScreenOn: Boolean? = null,
    val immersiveBrightness: Float? = null,
    val homeCards: List<String> = emptyList(),
    val quickTools: List<String> = emptyList(),
    val widgetQuickTools: List<String> = emptyList(),
    val themeGroupOrder: List<String> = emptyList(),
    val semesterWeeks: Int? = null,
    val semesterStartDate: String = "",
    val gpaFormula: GpaFormula? = null,
    val periodTimings: List<PeriodTiming> = emptyList(),
)

/** 负责将数据库数据序列化为 JSON（导出）以及从 JSON 恢复（导入/清空）。 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    /** 导出：读取全部表并序列化为 JSON 字符串。 */
    suspend fun export(): String {
        val dao = db.backupDao()
        val profile = settings.profile.first()
        val overlay = settings.overlayConfig.first()
        val data = BackupData(
            courses = dao.allCourses(),
            notes = dao.allNotes(),
            todos = dao.allTodos(),
            pomodoroRecords = dao.allPomodoroRecords(),
            gpaCourses = dao.allGpaCourses(),
            links = dao.allLinks(),
            ledgerEntries = dao.allLedgerEntries(),
            ledgerCategories = dao.allLedgerCategories(),
            schedules = dao.allSchedules(),
            overlayPresets = dao.allOverlayPresets(),
            shoppingActivities = dao.allShoppingActivities(),
            shoppingItems = dao.allShoppingItems(),
            personalProfile = profile,
            overlayConfig = overlay,
            profileAvatarBase64 = fileToBase64(profile.avatarPath),
            profileQqBase64 = fileToBase64(profile.qqQrPath),
            profileWechatBase64 = fileToBase64(profile.wechatQrPath),
            overlayImageBase64 = fileToBase64(overlay.imagePath),
            coursePresets = settings.coursePresets.first(),
            pomodoroPresets = settings.pomodoroPresets.first(),
            customThemes = settings.customThemes.first(),
            selectedTheme = settings.themeId.first(),
            // v1.1.0：导出 DataStore 配置，保证换机 / 还原后设置完整
            uiMode = settings.uiMode.first().name,
            keepScreenOn = settings.keepScreenOn.first(),
            immersiveBrightness = settings.immersiveBrightness.first(),
            homeCards = settings.homeCards.first(),
            quickTools = settings.quickTools.first(),
            widgetQuickTools = settings.widgetQuickTools.first(),
            themeGroupOrder = settings.themeGroupOrder.first(),
            semesterWeeks = settings.semesterWeeks.first(),
            semesterStartDate = settings.semesterStartDate.first(),
            gpaFormula = settings.gpaFormula.first(),
            periodTimings = settings.periodTimings.first(),
        )
        return json.encodeToString(BackupData.serializer(), data)
    }

    /**
     * 导入：从 JSON 恢复数据。
     * @param overwrite true=覆盖（先清空再写入）；false=增量（按主键合并，重复主键覆盖）
     */
    suspend fun import(raw: String, overwrite: Boolean) {
        val data = json.decodeFromString(BackupData.serializer(), raw)
        val dao = db.backupDao()
        db.withTransaction {
            if (overwrite) clearTables(dao)
            insertAll(dao, data)
        }

        // 把内嵌图片写回私有目录真实文件，并把 path 字段更新为新文件路径
        val avatarPath = saveBase64Image(data.profileAvatarBase64, "avatar")
        val qqPath = saveBase64Image(data.profileQqBase64, "qq_qr")
        val wechatPath = saveBase64Image(data.profileWechatBase64, "wechat_qr")
        val overlayPath = saveBase64Image(data.overlayImageBase64, "overlay")
        val restoredProfile = data.personalProfile?.let {
            it.copy(
                avatarPath = avatarPath.ifBlank { it.avatarPath },
                qqQrPath = qqPath.ifBlank { it.qqQrPath },
                wechatQrPath = wechatPath.ifBlank { it.wechatQrPath },
            )
        }
        val restoredOverlay = data.overlayConfig?.let {
            it.copy(imagePath = overlayPath.ifBlank { it.imagePath })
        }

        if (overwrite) {
            settings.setProfile(PersonalProfile())
            settings.setOverlayConfig(OverlayConfig())
            settings.setCoursePresets(emptyList())
            settings.setPomodoroPresets(defaultPomodoroPresets())
            settings.setCustomThemes(emptyList())
            settings.setThemeId("ink")
            // v1.1.0：覆盖导入前先把新增配置重置为默认，再按备份非空恢复
            settings.setUiMode(UiMode.SYSTEM)
            settings.setHomeCards(settings.defaultHomeCards())
            settings.setQuickTools(settings.defaultQuickTools())
            settings.setWidgetQuickTools(emptyList())
            settings.setThemeGroupOrder(emptyList())
            settings.setSemesterWeeks(20)
            settings.setSemesterStartDate("")
            settings.setGpaFormula(defaultGpaFormula())
            settings.setKeepScreenOn(false)
            settings.setImmersiveBrightness(0.3f)
            settings.setPeriodTimings(emptyList())
        }
        restoredProfile?.let { settings.setProfile(it) }
        restoredOverlay?.let { settings.setOverlayConfig(it) }
        if (data.coursePresets.isNotEmpty()) settings.setCoursePresets(data.coursePresets)
        if (data.pomodoroPresets.isNotEmpty()) settings.setPomodoroPresets(data.pomodoroPresets)
        if (data.customThemes.isNotEmpty()) settings.setCustomThemes(data.customThemes)
        if (data.selectedTheme.isNotBlank()) settings.setThemeId(data.selectedTheme)
        // v1.1.0：恢复新增配置（旧备份缺省字段不覆盖当前值）
        if (data.uiMode.isNotBlank()) {
            runCatching { UiMode.valueOf(data.uiMode) }.getOrNull()?.let { settings.setUiMode(it) }
        }
        if (data.homeCards.isNotEmpty()) settings.setHomeCards(data.homeCards)
        if (data.quickTools.isNotEmpty()) settings.setQuickTools(data.quickTools)
        if (data.widgetQuickTools.isNotEmpty()) settings.setWidgetQuickTools(data.widgetQuickTools)
        if (data.themeGroupOrder.isNotEmpty()) settings.setThemeGroupOrder(data.themeGroupOrder)
        data.semesterWeeks?.let { settings.setSemesterWeeks(it) }
        if (data.semesterStartDate.isNotBlank()) settings.setSemesterStartDate(data.semesterStartDate)
        data.gpaFormula?.let { settings.setGpaFormula(it) }
        data.keepScreenOn?.let { settings.setKeepScreenOn(it) }
        data.immersiveBrightness?.let { settings.setImmersiveBrightness(it) }
        if (data.periodTimings.isNotEmpty()) settings.setPeriodTimings(data.periodTimings)
    }

    /** 清空全部业务数据（保留数据库结构）。 */
    suspend fun clearAll() {
        db.withTransaction { clearTables(db.backupDao()) }
        settings.setProfile(PersonalProfile())
        settings.setOverlayConfig(OverlayConfig())
        settings.setCoursePresets(emptyList())
        settings.setPomodoroPresets(defaultPomodoroPresets())
        settings.setCustomThemes(emptyList())
        settings.setThemeId("ink")
        // v1.1.0：清空时同步重置新增配置为默认
        settings.setUiMode(UiMode.SYSTEM)
        settings.setHomeCards(settings.defaultHomeCards())
        settings.setQuickTools(settings.defaultQuickTools())
        settings.setWidgetQuickTools(emptyList())
        settings.setThemeGroupOrder(emptyList())
        settings.setSemesterWeeks(20)
        settings.setSemesterStartDate("")
        settings.setGpaFormula(defaultGpaFormula())
        settings.setKeepScreenOn(false)
        settings.setImmersiveBrightness(0.3f)
        settings.setPeriodTimings(emptyList())
    }

    private suspend fun clearTables(dao: BackupDao) {
        // 先删子表，再删父表，避免外键依赖
        dao.clearShoppingItems()
        dao.clearShoppingActivities()
        dao.clearLedgerEntries()
        dao.clearLedgerCategories()
        dao.clearSchedules()
        dao.clearLinks()
        dao.clearGpaCourses()
        dao.clearPomodoroRecords()
        dao.clearTodos()
        dao.clearNotes()
        dao.clearCourses()
        dao.clearOverlayPresets()
    }

    private suspend fun insertAll(dao: BackupDao, data: BackupData) {
        // 先插父表，再插子表，保证关联 ID 有效
        dao.insertShoppingActivities(data.shoppingActivities)
        dao.insertShoppingItems(data.shoppingItems)
        dao.insertLedgerEntries(data.ledgerEntries)
        dao.insertLedgerCategories(data.ledgerCategories)
        dao.insertSchedules(data.schedules)
        dao.insertLinks(data.links)
        dao.insertGpaCourses(data.gpaCourses)
        dao.insertPomodoroRecords(data.pomodoroRecords)
        dao.insertTodos(data.todos)
        dao.insertNotes(data.notes)
        dao.insertCourses(data.courses)
        dao.insertOverlayPresets(data.overlayPresets)
    }

    /** 读取本地图片文件并转为 base64；空路径或读取失败返回空串。 */
    private fun fileToBase64(path: String): String {
        if (path.isBlank()) return ""
        return runCatching {
            val file = java.io.File(path)
            if (!file.exists()) return ""
            android.util.Base64.encodeToString(file.readBytes(), android.util.Base64.NO_WRAP)
        }.getOrDefault("")
    }

    /** 把 base64 图片解码写回私有目录 filesDir/images/ 下的真实文件，返回新文件绝对路径；失败返回空串。 */
    private fun saveBase64Image(base64: String, name: String): String {
        if (base64.isBlank()) return ""
        return runCatching {
            val bytes = android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
            val dir = java.io.File(context.filesDir, "images").apply { if (!exists()) mkdirs() }
            val target = java.io.File(dir, "$name.img")
            target.writeBytes(bytes)
            target.absolutePath
        }.getOrDefault("")
    }
}
