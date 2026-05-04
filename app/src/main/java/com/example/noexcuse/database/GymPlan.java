package com.example.noexcuse.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "gym_plans")
public class GymPlan {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String dayOfWeek;
    public String startTime;
    public String bodyPart;
    public int restTimeSeconds;
    public boolean isSynced;
}