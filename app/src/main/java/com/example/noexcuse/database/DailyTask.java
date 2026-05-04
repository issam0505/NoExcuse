package com.example.noexcuse.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "daily_tasks")
public class DailyTask {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String title;
    public String description;
    public long taskTime;
    public boolean isDone;

}