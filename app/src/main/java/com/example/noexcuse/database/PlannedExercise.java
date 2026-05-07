package com.example.noexcuse.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "planned_exercises")
public class PlannedExercise {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int    planId;
    public String exerciseName;
    public int    setsTarget;   // nbr dyal series — gha had li kaydir user f planning
}