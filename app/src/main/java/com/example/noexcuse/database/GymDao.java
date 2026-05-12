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

    // Step 1: delete l row b id (f swap transaction)
    @androidx.room.Query("DELETE FROM gym_plans WHERE id=:id")
    void deletePlanById(int id);

    // Step 2: insert b nafs l id (REPLACE ila kayn conflict)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPlanWithId(GymPlan plan);

    // Update bodyPart u startTime BSSAH (bla ma tbdel dayOfWeek — safe, no unique risk)
    @androidx.room.Query("UPDATE gym_plans SET bodyPart=:bodyPart, startTime=:startTime WHERE id=:id")
    void updatePlanBodyAndTime(int id, String bodyPart, String startTime);

    @Delete
    void deletePlan(GymPlan plan);

    // Jib plans kolhom (lil liste dyal semana)
    @Query("SELECT * FROM gym_plans ORDER BY id ASC")
    LiveData<List<GymPlan>> getAllPlansLive();

    // Jib plans dyal semana m3ayyana (b weekStartDate)
    @Query("SELECT * FROM gym_plans WHERE weekStartDate = :weekStart ORDER BY id ASC")
    LiveData<List<GymPlan>> getPlansForWeekLive(String weekStart);

    // Jib plan dyal nhar m3ayyen f semana m3ayyana
    // exemple: getPlanForDayAndWeek("MONDAY", "2025-05-05")
    @Query("SELECT * FROM gym_plans WHERE dayOfWeek = :day AND weekStartDate = :weekStart LIMIT 1")
    GymPlan getPlanForDayAndWeek(String day, String weekStart);

    // ─────────────────────────────────────────────────────────────────────
    //  PLANNED EXERCISE
    // ─────────────────────────────────────────────────────────────────────

    @Insert
    long insertExerciseReturnId(PlannedExercise exercise);

    @Update
    void updateExercise(PlannedExercise exercise);

    @Delete
    void deleteExercise(PlannedExercise exercise);

    // Jib exercises dyal plan (LiveData lil UI)
    @Query("SELECT * FROM planned_exercises WHERE planId = :planId ORDER BY id ASC")
    LiveData<List<PlannedExercise>> getExercisesForPlanLive(int planId);

    // Jib exercises dyal plan (synchrone — lil background work)
    @Query("SELECT * FROM planned_exercises WHERE planId = :planId ORDER BY id ASC")
    List<PlannedExercise> getExercisesForPlan(int planId);

    // ─────────────────────────────────────────────────────────────────────
    //  GYM PERFORMANCE
    // ─────────────────────────────────────────────────────────────────────

    @Insert
    void insertPerformance(GymPerformance performance);

}