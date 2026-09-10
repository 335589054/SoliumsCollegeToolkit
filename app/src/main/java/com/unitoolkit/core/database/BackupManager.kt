package com.unitoolkit.core.database

import androidx.room.withTransaction
import com.unitoolkit.core.model.CoursePreset
import com.unitoolkit.core.model.CustomTheme
import com.unitoolkit.core.model.OverlayConfig
import com.unitoolkit.core.model.PersonalProfile
import com.unitoolkit.core.model.PomodoroPreset
import com.unitoolkit.core.model.defaultPomodoroPresets
import com.unitoolkit.core.storage.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 备份文件的顶层结构：包含全部业务表的数据快照，采用纯文本 JSON 存储。 */
@Serializable
data class BackupData(
    val formatVersion: Int = 1,
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
    val coursePresets: List<CoursePreset> = emptyList(),
    val pomodoroPresets: List<PomodoroPreset> = emptyList(),
    val customThemes: List<CustomTheme> = emptyList(),
    val selectedTheme: String = "",
)

/** 负责将数据库数据序列化为 JSON（导出）以及从 JSON 恢复（导入/清空）。 */
class BackupManager(
    private val db: AppDatabase,
    private val settings: SettingsRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    /** 导出：读取全部表并序列化为 JSON 字符串。 */
    suspend fun export(): String {
        val dao = db.backupDao()
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
            personalProfile = settings.profile.first(),
            overlayConfig = settings.overlayConfig.first(),
            coursePresets = settings.coursePresets.first(),
            pomodoroPresets = settings.pomodoroPresets.first(),
            customThemes = settings.customThemes.first(),
            selectedTheme = settings.themeId.first(),
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
        if (overwrite) {
            settings.setProfile(PersonalProfile())
            settings.setOverlayConfig(OverlayConfig())
            settings.setCoursePresets(emptyList())
            settings.setPomodoroPresets(defaultPomodoroPresets())
            settings.setCustomThemes(emptyList())
            settings.setThemeId("ink")
        }
        data.personalProfile?.let { settings.setProfile(it) }
        data.overlayConfig?.let { settings.setOverlayConfig(it) }
        if (data.coursePresets.isNotEmpty()) settings.setCoursePresets(data.coursePresets)
        if (data.pomodoroPresets.isNotEmpty()) settings.setPomodoroPresets(data.pomodoroPresets)
        if (data.customThemes.isNotEmpty()) settings.setCustomThemes(data.customThemes)
        if (data.selectedTheme.isNotBlank()) settings.setThemeId(data.selectedTheme)
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
}
