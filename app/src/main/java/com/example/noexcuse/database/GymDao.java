package com.example.noexcuse.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface GymDao {

    // ─── INSERT مع return ID ───────────────────────────────
    // ضروري باش PlannedExercise تعرف planId ديالها
    @Insert
    long insertPlanReturnId(GymPlan plan);

    // ضروري باش GymPerformance تعرف plannedExerciseId ديالها
    @Insert
    long insertExerciseReturnId(PlannedExercise exercise);

    @Insert
    void insertPerformance(GymPerformance performance);

    // ─── QUERIES ───────────────────────────────────────────

    // كاع الـ plans — LiveData باش الـ UI يتحدث تلقائياً
    @Query("SELECT * FROM gym_plans ORDER BY id DESC")
    LiveData<List<GymPlan>> getAllPlansLive();

    // التمارين ديال plan معيّن — LiveData
    @Query("SELECT * FROM planned_exercises WHERE planId = :planId")
    LiveData<List<PlannedExercise>> getExercisesForPlanLive(int planId);

    // نفس الشيء بدون LiveData — للاستخدام الداخلي
    @Query("SELECT * FROM planned_exercises WHERE planId = :planId")
    List<PlannedExercise> getExercisesForPlan(int planId);

    // الأداء ديال تمرين معيّن
    @Query("SELECT * FROM gym_performance WHERE plannedExerciseId = :exerciseId ORDER BY timestamp DESC")
    List<GymPerformance> getPerformanceForExercise(int exerciseId);

    // حذف البيانات القديمة (+30 يوم)
    @Query("DELETE FROM gym_performance WHERE timestamp < (:currentTime - 2592000000)")
    void deleteOldPerformance(long currentTime);
}