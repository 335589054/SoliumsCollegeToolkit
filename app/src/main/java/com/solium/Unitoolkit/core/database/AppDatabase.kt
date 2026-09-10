package com.solium.Unitoolkit.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CourseEntity::class,
        NoteEntity::class,
        TodoEntity::class,
        PomodoroRecordEntity::class,
        GpaCourseEntity::class,
        LinkEntity::class,
        LedgerEntryEntity::class,
        LedgerCategoryEntity::class,
        ScheduleEntity::class,
        OverlayPresetEntity::class,
        ShoppingActivityEntity::class,
        ShoppingItemEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun noteDao(): NoteDao
    abstract fun todoDao(): TodoDao
    abstract fun pomodoroDao(): PomodoroDao
    abstract fun gpaDao(): GpaDao
    abstract fun linkDao(): LinkDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun overlayDao(): OverlayDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun backupDao(): BackupDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ledger_entries ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `shopping_activities` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `shopping_items` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`activityId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`isPostponed` INTEGER NOT NULL, " +
                        "`isDone` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE courses ADD COLUMN weekType TEXT NOT NULL DEFAULT 'every'")
                db.execSQL("ALTER TABLE courses ADD COLUMN onceWeek INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE courses ADD COLUMN selectedWeeks TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE courses ADD COLUMN selectedDates TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE courses ADD COLUMN weekdaysText TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "unitoolkit.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build().also { instance = it }
            }
    }
}
