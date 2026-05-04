package com.example.noexcuse.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface EducationDao {
    @Insert
    void insertEducation(EducationTask educationTask);

    // كنجيبو كاع الحصص اللي مازال ما سالاو
    @Query("SELECT * FROM education_tasks WHERE isDone = 0")
    List<EducationTask> getOngoingEducation();
    @Query("SELECT * FROM education_tasks WHERE isFocusMode = 1 AND isDone = 0")
    List<EducationTask> getActiveFocusSessions();

    @Update
    void updateEducation(EducationTask educationTask);
}