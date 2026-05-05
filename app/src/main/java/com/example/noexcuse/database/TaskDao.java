package com.example.noexcuse.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface TaskDao {

    @Insert
    void insertTask(DailyTask task);

    // Rj3 kol tasks (pending + done) — sorting kaydir f MainActivity
    @Query("SELECT * FROM daily_tasks ORDER BY taskTime ASC")
    LiveData<List<DailyTask>> getPendingTasksLive();

    @Update
    void updateTask(DailyTask task);

    @Delete
    void deleteTask(DailyTask task);
}