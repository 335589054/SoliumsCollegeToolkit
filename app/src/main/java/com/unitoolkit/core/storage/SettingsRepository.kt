package com.unitoolkit.core.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.unitoolkit.core.model.GpaFormula
import com.unitoolkit.core.model.CoursePreset
import com.unitoolkit.core.model.CustomTheme
import com.unitoolkit.core.model.OverlayConfig
import com.unitoolkit.core.model.PersonalProfile
import com.unitoolkit.core.model.PomodoroPreset
import com.unitoolkit.core.model.defaultGpaFormula
import com.unitoolkit.core.model.defaultPomodoroPresets
import com.unitoolkit.core.theme.ThemeKey
import com.unitoolkit.core.theme.UiMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "settings")

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

class SettingsRepository(private val context: Context) {
    private val store = context.dataStore

    private companion object {
        val KEY_THEME = stringPreferencesKey("app_theme")
        val KEY_UI_MODE = stringPreferencesKey("app_ui_mode")
        val KEY_GPA_FORMULA = stringPreferencesKey("gpa_formula")
        val KEY_POMODORO_PRESETS = stringPreferencesKey("pomodoro_presets")
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("pomodoro_keep_screen_on")
        val KEY_IMMERSIVE_BRIGHTNESS = floatPreferencesKey("screen_brightness_immersive")
        val KEY_CURRENT_WEEK = intPreferencesKey("timetable_current_week")
        val KEY_SEMESTER_WEEKS = intPreferencesKey("timetable_semester_weeks")
        val KEY_SEMESTER_START = stringPreferencesKey("timetable_semester_start")
        val KEY_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val KEY_HOME_CARDS = stringPreferencesKey("home_cards")
        val KEY_QUICK_TOOLS = stringPreferencesKey("quick_tools")
        val KEY_PROFILE = stringPreferencesKey("personal_profile")
        val KEY_OVERLAY_CONFIG = stringPreferencesKey("overlay_config")
        val KEY_COURSE_PRESETS = stringPreferencesKey("course_presets")
        val KEY_CUSTOM_THEMES = stringPreferencesKey("custom_themes")
    }

    /** 首页卡片类型与默认顺序 */
    fun defaultHomeCards(): List<String> = listOf("course", "ledger", "todo", "links", "schedule")

    /** 首页快捷入口内置工具（默认全部展示） */
    fun defaultQuickTools(): List<String> = listOf(
        "course", "note", "todo", "pomodoro", "ledger", "utility", "schedule", "gpa", "links", "shopping", "overlay",
    )

    val themeKey: Flow<ThemeKey> = store.data.map { ThemeKey.fromKey(it[KEY_THEME]) }
    val uiMode: Flow<UiMode> = store.data.map {
        when (it[KEY_UI_MODE]) {
            "LIGHT" -> UiMode.LIGHT
            "DARK" -> UiMode.DARK
            else -> UiMode.SYSTEM
        }
    }
    val gpaFormula: Flow<GpaFormula> = store.data.map {
        it[KEY_GPA_FORMULA]?.let { s -> runCatching { json.decodeFromString(GpaFormula.serializer(), s) }.getOrNull() }
            ?: defaultGpaFormula()
    }
    val pomodoroPresets: Flow<List<PomodoroPreset>> = store.data.map {
        it[KEY_POMODORO_PRESETS]?.let { s ->
            runCatching { json.decodeFromString(ListSerializer(PomodoroPreset.serializer()), s) }.getOrNull()
        } ?: defaultPomodoroPresets()
    }
    val keepScreenOn: Flow<Boolean> = store.data.map { it[KEY_KEEP_SCREEN_ON] ?: false }
    val immersiveBrightness: Flow<Float> = store.data.map { it[KEY_IMMERSIVE_BRIGHTNESS] ?: 0.3f }
    val currentWeek: Flow<Int> = store.data.map { it[KEY_CURRENT_WEEK] ?: 1 }
    val semesterWeeks: Flow<Int> = store.data.map { it[KEY_SEMESTER_WEEKS] ?: 20 }
    val semesterStartDate: Flow<String> = store.data.map { it[KEY_SEMESTER_START] ?: "" }
    val firstLaunch: Flow<Boolean> = store.data.map { it[KEY_FIRST_LAUNCH] ?: true }
    val homeCards: Flow<List<String>> = store.data.map {
        it[KEY_HOME_CARDS]?.let { s ->
            runCatching { json.decodeFromString(ListSerializer(String.serializer()), s) }.getOrNull()
        } ?: defaultHomeCards()
    }
    val quickTools: Flow<List<String>> = store.data.map {
        it[KEY_QUICK_TOOLS]?.let { s ->
            runCatching { json.decodeFromString(ListSerializer(String.serializer()), s) }.getOrNull()
        } ?: defaultQuickTools()
    }
    val profile: Flow<PersonalProfile> = store.data.map {
        it[KEY_PROFILE]?.let { s ->
            runCatching { json.decodeFromString(PersonalProfile.serializer(), s) }.getOrNull()
        } ?: PersonalProfile()
    }
    val overlayConfig: Flow<OverlayConfig> = store.data.map {
        it[KEY_OVERLAY_CONFIG]?.let { s ->
            runCatching { json.decodeFromString(OverlayConfig.serializer(), s) }.getOrNull()
        } ?: OverlayConfig()
    }
    val coursePresets: Flow<List<CoursePreset>> = store.data.map {
        it[KEY_COURSE_PRESETS]?.let { s ->
            runCatching { json.decodeFromString(ListSerializer(CoursePreset.serializer()), s) }.getOrNull()
        } ?: emptyList()
    }
    val themeId: Flow<String> = store.data.map { it[KEY_THEME] ?: ThemeKey.INK.key }
    val customThemes: Flow<List<CustomTheme>> = store.data.map {
        it[KEY_CUSTOM_THEMES]?.let { s ->
            runCatching { json.decodeFromString(ListSerializer(CustomTheme.serializer()), s) }.getOrNull()
        } ?: emptyList()
    }

    suspend fun setTheme(key: ThemeKey) = store.edit { it[KEY_THEME] = key.key }
    suspend fun setUiMode(mode: UiMode) = store.edit { it[KEY_UI_MODE] = mode.name }
    suspend fun setGpaFormula(formula: GpaFormula) =
        store.edit { it[KEY_GPA_FORMULA] = json.encodeToString(GpaFormula.serializer(), formula) }
    suspend fun setPomodoroPresets(presets: List<PomodoroPreset>) =
        store.edit { it[KEY_POMODORO_PRESETS] = json.encodeToString(ListSerializer(PomodoroPreset.serializer()), presets) }
    suspend fun setKeepScreenOn(v: Boolean) = store.edit { it[KEY_KEEP_SCREEN_ON] = v }
    suspend fun setImmersiveBrightness(v: Float) = store.edit { it[KEY_IMMERSIVE_BRIGHTNESS] = v }
    suspend fun setCurrentWeek(week: Int) = store.edit { it[KEY_CURRENT_WEEK] = week }
    suspend fun setSemesterWeeks(weeks: Int) = store.edit { it[KEY_SEMESTER_WEEKS] = weeks }
    suspend fun setSemesterStartDate(date: String) = store.edit { it[KEY_SEMESTER_START] = date }
    suspend fun setFirstLaunchDone() = store.edit { it[KEY_FIRST_LAUNCH] = false }
    suspend fun setHomeCards(cards: List<String>) =
        store.edit { it[KEY_HOME_CARDS] = json.encodeToString(ListSerializer(String.serializer()), cards) }
    suspend fun setQuickTools(tools: List<String>) =
        store.edit { it[KEY_QUICK_TOOLS] = json.encodeToString(ListSerializer(String.serializer()), tools) }
    suspend fun setThemeId(id: String) = store.edit { it[KEY_THEME] = id }
    suspend fun setCustomThemes(list: List<CustomTheme>) =
        store.edit { it[KEY_CUSTOM_THEMES] = json.encodeToString(ListSerializer(CustomTheme.serializer()), list) }
    suspend fun setProfile(profile: PersonalProfile) =
        store.edit { it[KEY_PROFILE] = json.encodeToString(PersonalProfile.serializer(), profile) }
    suspend fun setOverlayConfig(config: OverlayConfig) =
        store.edit { it[KEY_OVERLAY_CONFIG] = json.encodeToString(OverlayConfig.serializer(), config) }
    suspend fun setCoursePresets(list: List<CoursePreset>) =
        store.edit { it[KEY_COURSE_PRESETS] = json.encodeToString(ListSerializer(CoursePreset.serializer()), list) }
}
