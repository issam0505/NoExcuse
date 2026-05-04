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

    // LiveData → الـ RecyclerView يتحدث تلقائياً لما تتزاد task جديدة
    @Query("SELECT * FROM daily_tasks WHERE isDone = 0 ORDER BY taskTime ASC")
    LiveData<List<DailyTask>> getPendingTasksLive();

    @Update
    void updateTask(DailyTask task);
    @Delete
    void deleteTask(DailyTask task);
}