package com.example.noexcuse.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface EducationDao {

    @Insert
    void insertEducation(EducationTask educationTask);

    @Update
    void updateEducation(EducationTask educationTask);

    @Delete
    void deleteEducation(EducationTask educationTask);

    // Rj3 kol sessions (pending + done) — sorting kaydir f MainActivity
    @Query("SELECT * FROM education_tasks ORDER BY startTime ASC")
    LiveData<List<EducationTask>> getPendingEducationLive();

    @Query("SELECT * FROM education_tasks WHERE id = :id LIMIT 1")
    EducationTask getById(int id);

    @Query("SELECT * FROM education_tasks WHERE moduleName = :name LIMIT 1")
    EducationTask getByModuleName(String name);

    @Query("SELECT * FROM education_tasks WHERE isFocusMode = 1 AND isDone = 0")
    List<EducationTask> getActiveFocusSessions();
}