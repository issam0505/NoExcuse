package com.example.noexcuse.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {
        DailyTask.class,
        GymPlan.class,
        PlannedExercise.class,
        GymPerformance.class,
        EducationTask.class,
        SleepSettings.class,
}, version = 10)   // ← 9 → 10
public abstract class AppDatabase extends RoomDatabase {

    public abstract TaskDao      taskDao();
    public abstract EducationDao educationDao();
    public abstract GymDao       gymDao();
    public abstract SleepDao     sleepDao();

    private static volatile AppDatabase INSTANCE;

    // 5→6
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE gym_plans ADD COLUMN weekStartDate TEXT");
            db.execSQL("ALTER TABLE gym_performance ADD COLUMN exerciseNameSnapshot TEXT");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_gym_plans_dayOfWeek_weekStartDate " +
                    "ON gym_plans (dayOfWeek, weekStartDate)");
        }
    };

    // 6→7
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE planned_exercises_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "planId INTEGER NOT NULL," +
                    "exerciseName TEXT," +
                    "setsTarget INTEGER NOT NULL DEFAULT 0" +
                    ")");
            db.execSQL("INSERT INTO planned_exercises_new (id, planId, exerciseName, setsTarget) " +
                    "SELECT id, planId, exerciseName, setsTarget FROM planned_exercises");
            db.execSQL("DROP TABLE planned_exercises");
            db.execSQL("ALTER TABLE planned_exercises_new RENAME TO planned_exercises");

            db.execSQL("CREATE TABLE gym_performance_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "plannedExerciseId INTEGER NOT NULL DEFAULT 0," +
                    "exerciseNameSnapshot TEXT," +
                    "setNumber INTEGER NOT NULL DEFAULT 0," +
                    "weight REAL NOT NULL DEFAULT 0," +
                    "reps INTEGER NOT NULL DEFAULT 0" +
                    ")");
            db.execSQL("INSERT INTO gym_performance_new (id, plannedExerciseId, exerciseNameSnapshot, setNumber, weight, reps) " +
                    "SELECT id, plannedExerciseId, exerciseNameSnapshot, setNumber, weight, reps FROM gym_performance");
            db.execSQL("DROP TABLE gym_performance");
            db.execSQL("ALTER TABLE gym_performance_new RENAME TO gym_performance");
        }
    };

    // 7→8
    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE planned_exercises ADD COLUMN durationMinutes INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE planned_exercises ADD COLUMN isCardio INTEGER NOT NULL DEFAULT 0");
        }
    };

    // 8→9: add date column to gym_performance
    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE gym_performance ADD COLUMN date INTEGER NOT NULL DEFAULT 0");
        }
    };

    // 9→10: add durationRecordedSeconds for cardio tracking
    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE gym_performance ADD COLUMN durationRecordedSeconds INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "no_excuse_db")
                            .addMigrations(
                                    MIGRATION_5_6,
                                    MIGRATION_6_7,
                                    MIGRATION_7_8,
                                    MIGRATION_8_9,
                                    MIGRATION_9_10)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}