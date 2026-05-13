package com.example.noexcuse.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "gym_performance")
public class GymPerformance {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int    plannedExerciseId;      // FK → PlannedExercise.id
    public String exerciseNameSnapshot;   // exercise name at time of recording

    public int   setNumber;   // 1, 2, 3...
    public float weight;      // weight in kg (0 for cardio)
    public int   reps;        // reps (0 for cardio)

    // Timestamp of the recording (milliseconds)
    public long date;

    // ⭐ Cardio only — actual duration recorded by the stopwatch (in seconds)
    // 0 for regular (non-cardio) exercises
    public int durationRecordedSeconds;
}