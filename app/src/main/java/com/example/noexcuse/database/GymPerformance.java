package com.example.noexcuse.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "gym_performance")
public class GymPerformance {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int    plannedExerciseId;      // FK → PlannedExercise.id
    public String exerciseNameSnapshot;   // smiya dyal exercise nhar el tsejil

    public int   setNumber;   // 1, 2, 3...
    public float weight;      // poids f kg
    public int   reps;        // reps

    // ⭐ Date dyal tsejil (timestamp f milliseconds) — bach nfilter b semana
    // exemple: System.currentTimeMillis()
    public long date;
}