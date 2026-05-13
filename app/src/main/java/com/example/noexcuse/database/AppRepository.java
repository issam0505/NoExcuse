package com.example.noexcuse.database;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppRepository {

    private final TaskDao      taskDao;
    private final EducationDao educationDao;
    private final GymDao       gymDao;
    private final SleepDao     sleepDao;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public AppRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        taskDao      = db.taskDao();
        educationDao = db.educationDao();
        gymDao       = db.gymDao();
        sleepDao     = db.sleepDao();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  DAILY TASK
    // ─────────────────────────────────────────────────────────────────────

    public void insertTask(DailyTask task) {
        executor.execute(() -> taskDao.insertTask(task));
    }

    public LiveData<List<DailyTask>> getPendingTasks() {
        return taskDao.getPendingTasksLive();
    }

    public void updateTask(DailyTask task) {
        executor.execute(() -> taskDao.updateTask(task));
    }

    public void deleteTask(DailyTask task) {
        executor.execute(() -> taskDao.deleteTask(task));
    }

    public void deleteExpiredDailyTasks() {
        executor.execute(() -> {
            long startOfToday = WeekUtils.getStartOfToday();
            taskDao.deleteTasksBefore(startOfToday);
        });
    }

    public void deleteExpiredEducationSessions() {
        executor.execute(() -> {
            long now = System.currentTimeMillis();
            educationDao.deleteSessionsBefore(now);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    //  EDUCATION
    // ─────────────────────────────────────────────────────────────────────

    public void insertEducation(EducationTask task) {
        executor.execute(() -> educationDao.insertEducation(task));
    }

    public void updateEducation(EducationTask task) {
        executor.execute(() -> educationDao.updateEducation(task));
    }

    public void deleteEducation(EducationTask task) {
        executor.execute(() -> educationDao.deleteEducation(task));
    }

    public LiveData<List<EducationTask>> getPendingEducation() {
        return educationDao.getPendingEducationLive();
    }

    public EducationTask getEducationById(int id) {
        return educationDao.getById(id);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  GYM PLAN
    // ─────────────────────────────────────────────────────────────────────

    public void insertGymPlan(GymPlan plan, OnIdGeneratedCallback callback) {
        executor.execute(() -> {
            long planId = gymDao.insertPlanReturnId(plan);
            if (callback != null) callback.onIdGenerated((int) planId);
        });
    }

    public void updateGymPlan(GymPlan plan) {
        executor.execute(() -> gymDao.updatePlan(plan));
    }

    public void deleteGymPlan(GymPlan plan) {
        executor.execute(() -> gymDao.deletePlan(plan));
    }

    public void updateGymPlanBodyAndTime(GymPlan plan) {
        executor.execute(() -> gymDao.updatePlanBodyAndTime(
                plan.id,
                plan.bodyPart,
                plan.startTime));
    }

    public void movePlanToDay(GymPlan plan) {
        executor.execute(() -> {
            gymDao.deletePlanById(plan.id);
            gymDao.insertPlanWithId(plan);
        });
    }

    public void swapPlans(GymPlan planA, GymPlan planB) {
        executor.execute(() -> {
            gymDao.deletePlanById(planA.id);
            gymDao.deletePlanById(planB.id);
            gymDao.insertPlanWithId(planA);
            gymDao.insertPlanWithId(planB);
        });
    }

    public LiveData<List<GymPlan>> getAllGymPlans() {
        return gymDao.getAllPlansLive();
    }

    public LiveData<List<GymPlan>> getPlansForWeek(String weekStart) {
        return gymDao.getPlansForWeekLive(weekStart);
    }

    public void getGymPlanForDayAndWeek(String day, String weekStart, OnPlanLoadedCallback callback) {
        executor.execute(() -> {
            GymPlan plan = gymDao.getPlanForDayAndWeek(day, weekStart);
            if (callback != null) callback.onLoaded(plan);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PLANNED EXERCISE
    // ─────────────────────────────────────────────────────────────────────

    public void insertPlannedExercise(PlannedExercise exercise, OnIdGeneratedCallback callback) {
        executor.execute(() -> {
            long exId = gymDao.insertExerciseReturnId(exercise);
            if (callback != null) callback.onIdGenerated((int) exId);
        });
    }

    public void updatePlannedExercise(PlannedExercise exercise) {
        executor.execute(() -> gymDao.updateExercise(exercise));
    }

    public void deletePlannedExercise(PlannedExercise exercise) {
        executor.execute(() -> gymDao.deleteExercise(exercise));
    }

    public LiveData<List<PlannedExercise>> getExercisesForPlan(int planId) {
        return gymDao.getExercisesForPlanLive(planId);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  GYM PERFORMANCE
    // ─────────────────────────────────────────────────────────────────────

    public void insertPerformance(GymPerformance performance) {
        executor.execute(() -> gymDao.insertPerformance(performance));
    }

    // ⭐ Jib kol performances dyal liste dyal exercise IDs — f background thread
    // callback.onLoaded() kay3awwed f UI thread
    public void getPerformancesForExercises(List<Integer> exerciseIds,
                                            OnPerformanceLoadedCallback callback) {
        executor.execute(() -> {
            List<GymPerformance> perfs = gymDao.getPerformancesForExercises(exerciseIds);
            if (callback != null) callback.onLoaded(perfs);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    //  CALLBACKS
    // ─────────────────────────────────────────────────────────────────────

    public interface OnIdGeneratedCallback {
        void onIdGenerated(int id);
    }

    public interface OnPlanLoadedCallback {
        void onLoaded(GymPlan plan);
    }

    public interface OnPerformanceLoadedCallback {
        void onLoaded(List<GymPerformance> performances);
    }
}