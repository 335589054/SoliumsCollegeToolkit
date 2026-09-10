package com.unitoolkit.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY weekday, startTime")
    fun all(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE weekday = :weekday ORDER BY startTime")
    fun byWeekday(weekday: Int): Flow<List<CourseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(course: CourseEntity): Long

    @Delete
    suspend fun delete(course: CourseEntity)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun all(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :q || '%' OR content LIKE '%' || :q || '%' ORDER BY updatedAt DESC")
    fun search(q: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun byId(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity): Long

    @Delete
    suspend fun delete(note: NoteEntity)
}

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY isDone, priority DESC, sortOrder, createdAt DESC")
    fun all(): Flow<List<TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(todo: TodoEntity): Long

    @Update
    suspend fun update(todo: TodoEntity)

    @Delete
    suspend fun delete(todo: TodoEntity)
}

data class PomodoroStats(val count: Long, val totalMinutes: Long)

@Dao
interface PomodoroDao {
    @Query("SELECT * FROM pomodoro_records ORDER BY completedAt DESC")
    fun all(): Flow<List<PomodoroRecordEntity>>

    @Insert
    suspend fun insert(record: PomodoroRecordEntity): Long

    @Query("SELECT COUNT(*) AS count, COALESCE(SUM(focusMinutes), 0) AS totalMinutes FROM pomodoro_records WHERE completedAt >= :since")
    suspend fun todayStats(since: Long): PomodoroStats
}

@Dao
interface GpaDao {
    @Query("SELECT * FROM gpa_courses ORDER BY semester DESC, id")
    fun all(): Flow<List<GpaCourseEntity>>

    @Query("SELECT DISTINCT semester FROM gpa_courses ORDER BY semester DESC")
    fun semesters(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(course: GpaCourseEntity): Long

    @Delete
    suspend fun delete(course: GpaCourseEntity)
}

@Dao
interface LinkDao {
    @Query("SELECT * FROM links ORDER BY sortOrder, id")
    fun all(): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE isHomeCard = 1 ORDER BY sortOrder, id")
    fun homeCards(): Flow<List<LinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: LinkEntity): Long

    @Delete
    suspend fun delete(link: LinkEntity)
}

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_entries ORDER BY date DESC, id DESC")
    fun entries(): Flow<List<LedgerEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LedgerEntryEntity): Long

    @Delete
    suspend fun delete(entry: LedgerEntryEntity)

    @Query("DELETE FROM ledger_entries")
    suspend fun clearAll()

    @Query("SELECT * FROM ledger_categories WHERE type = :type ORDER BY isCustom, id")
    fun categories(type: String): Flow<List<LedgerCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(cat: LedgerCategoryEntity): Long

    @Delete
    suspend fun deleteCategory(cat: LedgerCategoryEntity)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY startTime")
    fun all(): Flow<List<ScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(schedule: ScheduleEntity): Long

    @Delete
    suspend fun delete(schedule: ScheduleEntity)
}

@Dao
interface OverlayDao {
    @Query("SELECT * FROM overlay_presets ORDER BY id")
    fun all(): Flow<List<OverlayPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preset: OverlayPresetEntity): Long

    @Delete
    suspend fun delete(preset: OverlayPresetEntity)
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_activities ORDER BY createdAt DESC, id DESC")
    fun activities(): Flow<List<ShoppingActivityEntity>>

    @Query("SELECT * FROM shopping_items WHERE activityId = :activityId ORDER BY isDone, isPostponed, createdAt")
    fun items(activityId: Long): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE isPostponed = 1 ORDER BY createdAt")
    fun postponedItems(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT i.*, a.type AS activityType, a.name AS activityName FROM shopping_items i JOIN shopping_activities a ON i.activityId = a.id WHERE i.isPostponed = 1 ORDER BY i.createdAt")
    fun postponedRows(): Flow<List<PostponedItemRow>>

    @Query("SELECT i.* FROM shopping_items i JOIN shopping_activities a ON i.activityId = a.id WHERE i.isPostponed = 1 AND a.type = :type ORDER BY i.createdAt")
    suspend fun postponedForType(type: String): List<ShoppingItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ShoppingActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItemEntity): Long

    @Update
    suspend fun updateItem(item: ShoppingItemEntity)

    @Delete
    suspend fun deleteActivity(activity: ShoppingActivityEntity)

    @Delete
    suspend fun deleteItem(item: ShoppingItemEntity)

    @Query("DELETE FROM shopping_items WHERE activityId = :activityId")
    suspend fun deleteItemsByActivity(activityId: Long)
}

/** 暂缓采购项目（含所属活动类型 / 名称），用于主界面「暂缓采购区」 */
data class PostponedItemRow(
    @Embedded val item: ShoppingItemEntity,
    val activityType: String,
    val activityName: String,
)