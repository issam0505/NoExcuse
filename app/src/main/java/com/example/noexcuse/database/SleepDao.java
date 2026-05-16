package com.example.noexcuse.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface SleepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSleepSettings(SleepSettings settings);

    @Query("SELECT * FROM sleep_settings ORDER BY id DESC LIMIT 1")
    SleepSettings getSleepSettings();
}