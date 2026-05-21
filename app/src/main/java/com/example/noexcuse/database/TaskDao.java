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

    @Query("SELECT * FROM daily_tasks WHERE taskTime BETWEEN :startOfDay AND :endOfDay ORDER BY taskTime ASC")
    List<DailyTask> getTasksForDay(long startOfDay, long endOfDay);

    @Update
    void updateTask(DailyTask task);

    @Delete
    void deleteTask(DailyTask task);

    /**
     * Hiyyed kol tasks li fat nharha — taskTime < startOfToday (minuit)
     * Par exemple: task d5lat 17:00 — ila jat gheda 00:00, titmsha
     */
    @Query("DELETE FROM daily_tasks WHERE taskTime < :startOfToday")
    void deleteTasksBefore(long startOfToday);
}
