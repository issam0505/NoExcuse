package com.example.noexcuse.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "gym_performance")
public class GymPerformance {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int plannedExerciseId;
    public int setNumber;
    public float weight;
    public int reps;
    public long timestamp;
    public boolean isSynced;
}