package com.example.noexcuse.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {
        DailyTask.class,
        GymPlan.class,
        PlannedExercise.class,
        GymPerformance.class,
        EducationTask.class,
        SleepSettings.class,
           // ← جديد: settings ديال المستخدم
}, version = 4)                 // ← version ارتفعت من 1 لـ 2
public abstract class AppDatabase extends RoomDatabase {

    public abstract TaskDao taskDao();
    public abstract EducationDao educationDao();
    public abstract GymDao gymDao();
    public abstract SleepDao sleepDao();


    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "no_excuse_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}