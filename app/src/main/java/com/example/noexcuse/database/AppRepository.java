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

    /**
     * Update bodyPart + startTime bssah — mabdelnach dayOfWeek.
     * Safe 100% — makaynch unique constraint risk.
     */
    public void updateGymPlanBodyAndTime(GymPlan plan) {
        executor.execute(() -> gymDao.updatePlanBodyAndTime(
                plan.id,
                plan.bodyPart,
                plan.startTime));
    }

    /**
     * Move plan l nhar jdid — delete + insert f nafs l executor thread (sequential).
     * Hada lhaqiqi lli kayhal unique constraint: ndirou delete l row l9dima,
     * insert b nafs l id u dayOfWeek jdid — SQLite makayshufch conflict.
     */
    public void movePlanToDay(GymPlan plan) {
        executor.execute(() -> {
            gymDao.deletePlanById(plan.id);
            gymDao.insertPlanWithId(plan);
        });
    }

    /**
     * Swap dayOfWeek bين planA u planB — f nafs l executor thread, sequential.
     * Delete les 2, insert les 2 b days mqdouba — zero unique constraint risk.
     */
    public void swapPlans(GymPlan planA, GymPlan planB) {
        executor.execute(() -> {
            // Delete les 2 d'abord
            gymDao.deletePlanById(planA.id);
            gymDao.deletePlanById(planB.id);
            // Insert les 2 b days mqdouba
            gymDao.insertPlanWithId(planA);
            gymDao.insertPlanWithId(planB);
        });
    }

    /**
     * @deprecated Stamo movePlanToDay wla updateGymPlanBodyAndTime.
     * Hadi kadir crash b UNIQUE constraint ila tbdel dayOfWeek.
     */
    @Deprecated
    public void updateGymPlanFields(GymPlan plan) {
        // Redirect l movePlanToDay ila dayOfWeek tbdel — wla updateBodyAndTime ila mabdelnach
        executor.execute(() -> {
            gymDao.deletePlanById(plan.id);
            gymDao.insertPlanWithId(plan);
        });
    }

    public void deleteGymPlan(GymPlan plan) {
        executor.execute(() -> gymDao.deletePlan(plan));
    }

    // Kol plans (sans filtre semana)
    public LiveData<List<GymPlan>> getAllGymPlans() {
        return gymDao.getAllPlansLive();
    }

    // Plans dyal semana m3ayyana — utilisé pour afficher la semaine courante
    // weekStart = "2025-05-05" (lundi dyal had semana)
    public LiveData<List<GymPlan>> getPlansForWeek(String weekStart) {
        return gymDao.getPlansForWeekLive(weekStart);
    }

    // Jib plan dyal nhar + semana (synchrone — lil background)
    // day = "MONDAY", weekStart = "2025-05-05"
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

    // ⭐ Zid performance — MUHIM: dima set exerciseNameSnapshot 9bel ma tsift!
    // GymPerformance perf = new GymPerformance();
    // perf.exerciseNameSnapshot = exercise.exerciseName;  ← dima!
    public void insertPerformance(GymPerformance performance) {
        executor.execute(() -> gymDao.insertPerformance(performance));
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