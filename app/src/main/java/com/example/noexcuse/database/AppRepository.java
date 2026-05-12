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

    @Deprecated
    public void updateGymPlanFields(GymPlan plan) {
        executor.execute(() -> {
            gymDao.deletePlanById(plan.id);
            gymDao.insertPlanWithId(plan);
        });
    }

    public void deleteGymPlan(GymPlan plan) {
        executor.execute(() -> gymDao.deletePlan(plan));
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
    //  AUTO-COPY: semana jdida → copy plans + exercises mn semana li fazat
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Ila had semana (currentWeek) makanch fiha plans,
     * katcopy plans + exercises mn semana li fazat (previousWeek) l currentWeek.
     *
     * GymPerformance mkatcopy-ch — hiya tarikh dyal performance, tbqa bhal kima hiya.
     *
     * Callback katji f background thread — ila bghiti t3mel UI 3liha dir runOnUiThread.
     *
     * Exemple:
     *   viewModel.copyPlansIfNewWeek(
     *       WeekUtils.getCurrentWeekStart(),
     *       WeekUtils.getPreviousWeekStart(),
     *       copied -> runOnUiThread(() -> {
     *           if (copied) Toast.makeText(this, "Plan dyal semana jdida t-copy ✅", ...).show();
     *       })
     *   );
     */
    public void copyPlansIfNewWeek(String currentWeek, String previousWeek,
                                   OnCopyDoneCallback callback) {
        executor.execute(() -> {
            // Check wach had semana 3andha plans wla la
            int currentCount = gymDao.countPlansForWeek(currentWeek);
            if (currentCount > 0) {
                // Mashi semana jdida — plans kaynin deja
                if (callback != null) callback.onDone(false);
                return;
            }

            // Jib plans dyal semana li fazat
            List<GymPlan> previousPlans = gymDao.getPlansForWeekSync(previousWeek);
            if (previousPlans == null || previousPlans.isEmpty()) {
                // Semana li fazat makanch plans fiha
                if (callback != null) callback.onDone(false);
                return;
            }

            // Copy kol plan + exercises dyalha
            for (GymPlan oldPlan : previousPlans) {
                // Sift exercises l9dim 9bal ma nbdlo l plan id
                List<PlannedExercise> oldExercises = gymDao.getExercisesForPlan(oldPlan.id);

                // Sana plan jdid b weekStartDate jdida
                GymPlan newPlan = new GymPlan();
                newPlan.dayOfWeek     = oldPlan.dayOfWeek;
                newPlan.weekStartDate = currentWeek;   // ← semana jdida
                newPlan.startTime     = oldPlan.startTime;
                newPlan.bodyPart      = oldPlan.bodyPart;
                newPlan.isSynced      = false;

                // Insert l plan jdid w jib id dyalho
                long newPlanId = gymDao.insertPlanReturnId(newPlan);

                // Copy exercises — b planId jdid
                if (oldExercises != null) {
                    for (PlannedExercise oldEx : oldExercises) {
                        PlannedExercise newEx = new PlannedExercise();
                        newEx.planId          = (int) newPlanId;  // ← id dyal plan jdid
                        newEx.exerciseName    = oldEx.exerciseName;
                        newEx.setsTarget      = oldEx.setsTarget;
                        newEx.durationMinutes = oldEx.durationMinutes;
                        newEx.isCardio        = oldEx.isCardio;
                        gymDao.insertExerciseReturnId(newEx);
                    }
                }
            }

            if (callback != null) callback.onDone(true);
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

    public interface OnCopyDoneCallback {
        void onDone(boolean copied);
    }
}