package com.example.noexcuse.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface SleepDao {
    @Insert
    void insertSleepSettings(SleepSettings settings);

    @Query("SELECT * FROM sleep_settings LIMIT 1")
    SleepSettings getSleepSettings();
}