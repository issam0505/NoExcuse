package com.example.noexcuse.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface GymDao {

    // ─────────────────────────────────────────────────────────────────────
    //  GYM PLAN
    // ─────────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPlanReturnId(GymPlan plan);

    @Update
    void updatePlan(GymPlan plan);

    @Query("DELETE FROM gym_plans WHERE id=:id")
    void deletePlanById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPlanWithId(GymPlan plan);

    @Query("UPDATE gym_plans SET bodyPart=:bodyPart, startTime=:startTime WHERE id=:id")
    void updatePlanBodyAndTime(int id, String bodyPart, String startTime);

    @Delete
    void deletePlan(GymPlan plan);

    @Query("SELECT * FROM gym_plans ORDER BY id ASC")
    LiveData<List<GymPlan>> getAllPlansLive();

    @Query("SELECT * FROM gym_plans WHERE weekStartDate = :weekStart ORDER BY id ASC")
    LiveData<List<GymPlan>> getPlansForWeekLive(String weekStart);

    @Query("SELECT * FROM gym_plans WHERE dayOfWeek = :day AND weekStartDate = :weekStart LIMIT 1")
    GymPlan getPlanForDayAndWeek(String day, String weekStart);

    // ── JDID: jib plans dyal semana (synchrone — lil background copy) ──────
    @Query("SELECT * FROM gym_plans WHERE weekStartDate = :weekStart ORDER BY id ASC")
    List<GymPlan> getPlansForWeekSync(String weekStart);

    // ── JDID: count plans dyal semana (bach n3rfo wach khawya wla la) ───────
    @Query("SELECT COUNT(*) FROM gym_plans WHERE weekStartDate = :weekStart")
    int countPlansForWeek(String weekStart);

    // ─────────────────────────────────────────────────────────────────────
    //  PLANNED EXERCISE
    // ─────────────────────────────────────────────────────────────────────

    @Insert
    long insertExerciseReturnId(PlannedExercise exercise);

    @Update
    void updateExercise(PlannedExercise exercise);

    @Delete
    void deleteExercise(PlannedExercise exercise);

    @Query("SELECT * FROM planned_exercises WHERE planId = :planId ORDER BY id ASC")
    LiveData<List<PlannedExercise>> getExercisesForPlanLive(int planId);

    @Query("SELECT * FROM planned_exercises WHERE planId = :planId ORDER BY id ASC")
    List<PlannedExercise> getExercisesForPlan(int planId);

    // ─────────────────────────────────────────────────────────────────────
    //  GYM PERFORMANCE
    // ─────────────────────────────────────────────────────────────────────

    @Insert
    void insertPerformance(GymPerformance performance);

}