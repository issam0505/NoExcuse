package com.example.noexcuse.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "education_tasks")
public class EducationTask {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String moduleName;      // مثلاً React
    public String studyPlan;       // 2h Chapter, 2h Practice
    public long startTime;
    public long endTime;
    public boolean isFocusMode;    // واش الـ Focus Mode خدام
    public boolean isDone;
}