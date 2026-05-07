package com.example.noexcuse.database;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "gym_plans",
        indices = {@Index(value = {"dayOfWeek", "weekStartDate"}, unique = true)}
)
public class GymPlan {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    public String dayOfWeek;

    // "2025-05-05" — dima lundi dyal had semana (ISO format yyyy-MM-dd)
    // unique constraint ma3 dayOfWeek → machi momkin ykun MONDAY+semana1 mrratayn
    public String weekStartDate;

    // "07:30"
    public String startTime;

    // "Chest & Triceps", "Legs", etc.
    public String bodyPart;

    public boolean isSynced;

    // restTimeSeconds → SharedPreferences (machi DB)
}