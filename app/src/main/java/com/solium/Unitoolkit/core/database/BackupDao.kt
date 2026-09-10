package com.solium.Unitoolkit.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * 备份/导入/清空专用的数据访问接口。
 * 提供各表的一次性全量读取、批量写入（REPLACE，便于按主键合并）与清空能力。
 */
@Dao
interface BackupDao {
    @Query("SELECT * FROM courses")
    suspend fun allCourses(): List<CourseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(list: List<CourseEntity>)

    @Query("DELETE FROM courses")
    suspend fun clearCourses()

    @Query("SELECT * FROM notes")
    suspend fun allNotes(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(list: List<NoteEntity>)

    @Query("DELETE FROM notes")
    suspend fun clearNotes()

    @Query("SELECT * FROM todos")
    suspend fun allTodos(): List<TodoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodos(list: List<TodoEntity>)

    @Query("DELETE FROM todos")
    suspend fun clearTodos()

    @Query("SELECT * FROM pomodoro_records")
    suspend fun allPomodoroRecords(): List<PomodoroRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPomodoroRecords(list: List<PomodoroRecordEntity>)

    @Query("DELETE FROM pomodoro_records")
    suspend fun clearPomodoroRecords()

    @Query("SELECT * FROM gpa_courses")
    suspend fun allGpaCourses(): List<GpaCourseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGpaCourses(list: List<GpaCourseEntity>)

    @Query("DELETE FROM gpa_courses")
    suspend fun clearGpaCourses()

    @Query("SELECT * FROM links")
    suspend fun allLinks(): List<LinkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinks(list: List<LinkEntity>)

    @Query("DELETE FROM links")
    suspend fun clearLinks()

    @Query("SELECT * FROM ledger_entries")
    suspend fun allLedgerEntries(): List<LedgerEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntries(list: List<LedgerEntryEntity>)

    @Query("DELETE FROM ledger_entries")
    suspend fun clearLedgerEntries()

    @Query("SELECT * FROM ledger_categories")
    suspend fun allLedgerCategories(): List<LedgerCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerCategories(list: List<LedgerCategoryEntity>)

    @Query("DELETE FROM ledger_categories")
    suspend fun clearLedgerCategories()

    @Query("SELECT * FROM schedules")
    suspend fun allSchedules(): List<ScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(list: List<ScheduleEntity>)

    @Query("DELETE FROM schedules")
    suspend fun clearSchedules()

    @Query("SELECT * FROM overlay_presets")
    suspend fun allOverlayPresets(): List<OverlayPresetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverlayPresets(list: List<OverlayPresetEntity>)

    @Query("DELETE FROM overlay_presets")
    suspend fun clearOverlayPresets()

    @Query("SELECT * FROM shopping_activities")
    suspend fun allShoppingActivities(): List<ShoppingActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingActivities(list: List<ShoppingActivityEntity>)

    @Query("DELETE FROM shopping_activities")
    suspend fun clearShoppingActivities()

    @Query("SELECT * FROM shopping_items")
    suspend fun allShoppingItems(): List<ShoppingItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItems(list: List<ShoppingItemEntity>)

    @Query("DELETE FROM shopping_items")
    suspend fun clearShoppingItems()
}