package com.example.noexcuse.database;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppRepository {

    private final TaskDao taskDao;
    private final EducationDao educationDao;
    private final GymDao gymDao;
    private final SleepDao sleepDao;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public AppRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        taskDao      = db.taskDao();
        educationDao = db.educationDao();
        gymDao       = db.gymDao();
        sleepDao     = db.sleepDao();
    }

    // ─────────────────────────────────────────
    //  DAILY TASK
    // ─────────────────────────────────────────
    public void insertTask(DailyTask task) {
        executor.execute(() -> taskDao.insertTask(task));
    }

    public LiveData<List<DailyTask>> getPendingTasks() {
        return taskDao.getPendingTasksLive();
    }

    public void updateTask(DailyTask task) {
        executor.execute(() -> taskDao.updateTask(task));
    }

    // ─────────────────────────────────────────
    //  EDUCATION
    // ─────────────────────────────────────────
    public void insertEducation(EducationTask task) {
        executor.execute(() -> educationDao.insertEducation(task));
    }

    // ─────────────────────────────────────────
    //  GYM — الجزء المهم: 3 جداول مترابطة
    //
    //  GymPlan ─── planId ──► PlannedExercise ─── plannedExerciseId ──► GymPerformance
    //
    //  الـ flow:
    //  1. insertGymPlan()  → يرجع planId الجديد
    //  2. insertPlannedExercise(exercise مع planId)  → لكل تمرين
    //  3. insertPerformance(performance مع plannedExerciseId) → لكل set يكمّلو المستخدم
    // ─────────────────────────────────────────

    /**
     * يحفظ GymPlan ويرجع الـ planId المولّد — ضروري باش نربط PlannedExercises بيه
     * callback لأن الـ insert يخدم في background thread
     */
    public void insertGymPlan(GymPlan plan, OnIdGeneratedCallback callback) {
        executor.execute(() -> {
            long planId = gymDao.insertPlanReturnId(plan);
            if (callback != null) callback.onIdGenerated((int) planId);
        });
    }

    /**
     * يحفظ تمرين واحد مربوط بـ planId
     * callback يرجع الـ exerciseId — ضروري لـ GymPerformance
     */
    public void insertPlannedExercise(PlannedExercise exercise, OnIdGeneratedCallback callback) {
        executor.execute(() -> {
            long exId = gymDao.insertExerciseReturnId(exercise);
            if (callback != null) callback.onIdGenerated((int) exId);
        });
    }

    /**
     * يحفظ أداء set واحد مربوط بـ plannedExerciseId
     */
    public void insertPerformance(GymPerformance performance) {
        executor.execute(() -> gymDao.insertPerformance(performance));
    }

    public LiveData<List<GymPlan>> getAllGymPlans() {
        return gymDao.getAllPlansLive();
    }

    public LiveData<List<PlannedExercise>> getExercisesForPlan(int planId) {
        return gymDao.getExercisesForPlanLive(planId);
    }

    public void deleteOldPerformance() {
        executor.execute(() -> gymDao.deleteOldPerformance(System.currentTimeMillis()));
    }

    public interface OnIdGeneratedCallback {
        void onIdGenerated(int id);
    }
    public void deleteTask(DailyTask task) {
        executor.execute(() -> taskDao.deleteTask(task));
    }
}