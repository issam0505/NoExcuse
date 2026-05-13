package com.example.noexcuse.database;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class AppViewModel extends AndroidViewModel {

    private final AppRepository repository;

    public final LiveData<List<DailyTask>>    pendingTasks;
    public final LiveData<List<EducationTask>> pendingEducation;

    public AppViewModel(@NonNull Application application) {
        super(application);
        repository       = new AppRepository(application);
        pendingTasks     = repository.getPendingTasks();
        pendingEducation = repository.getPendingEducation();
    }

    // ─── DAILY TASKS ──────────────────────────────────────────────────────

    public void addTask(DailyTask task)    { repository.insertTask(task); }
    public void updateTask(DailyTask task) { repository.updateTask(task); }
    public void deleteTask(DailyTask task) { repository.deleteTask(task); }

    public void deleteExpiredTasks() {
        repository.deleteExpiredDailyTasks();
        repository.deleteExpiredEducationSessions();
    }

    // ─── EDUCATION ────────────────────────────────────────────────────────

    public void addEducation(EducationTask task)    { repository.insertEducation(task); }
    public void updateEducation(EducationTask task) { repository.updateEducation(task); }
    public void deleteEducation(EducationTask task) { repository.deleteEducation(task); }

    public EducationTask getEducationById(int id) {
        return repository.getEducationById(id);
    }

    // ─── GYM PLAN ─────────────────────────────────────────────────────────

    public void addGymPlan(GymPlan plan, AppRepository.OnIdGeneratedCallback callback) {
        repository.insertGymPlan(plan, callback);
    }

    public void updateGymPlan(GymPlan plan) { repository.updateGymPlan(plan); }
    public void deleteGymPlan(GymPlan plan) { repository.deleteGymPlan(plan); }

    public void updateGymPlanBodyAndTime(GymPlan plan) {
        repository.updateGymPlanBodyAndTime(plan);
    }

    public void movePlanToDay(GymPlan plan) {
        repository.movePlanToDay(plan);
    }

    public void swapPlans(GymPlan planA, GymPlan planB) {
        repository.swapPlans(planA, planB);
    }

    public LiveData<List<GymPlan>> getAllGymPlans() {
        return repository.getAllGymPlans();
    }

    public LiveData<List<GymPlan>> getPlansForWeek(String weekStart) {
        return repository.getPlansForWeek(weekStart);
    }

    public void getGymPlanForDayAndWeek(String day, String weekStart,
                                        AppRepository.OnPlanLoadedCallback callback) {
        repository.getGymPlanForDayAndWeek(day, weekStart, callback);
    }

    // ─── PLANNED EXERCISE ─────────────────────────────────────────────────

    public void addPlannedExercise(PlannedExercise exercise,
                                   AppRepository.OnIdGeneratedCallback callback) {
        repository.insertPlannedExercise(exercise, callback);
    }

    public void updatePlannedExercise(PlannedExercise exercise) {
        repository.updatePlannedExercise(exercise);
    }

    public void deletePlannedExercise(PlannedExercise exercise) {
        repository.deletePlannedExercise(exercise);
    }

    public LiveData<List<PlannedExercise>> getExercisesForPlan(int planId) {
        return repository.getExercisesForPlan(planId);
    }

    // ─── GYM PERFORMANCE ──────────────────────────────────────────────────

    public void addPerformance(GymPerformance performance) {
        repository.insertPerformance(performance);
    }

    // ⭐ Jib kol performances dyal plan (3br exerciseIds) — f background, callback f UI thread
    public void getPerformancesForExercises(List<Integer> exerciseIds,
                                            AppRepository.OnPerformanceLoadedCallback callback) {
        repository.getPerformancesForExercises(exerciseIds, callback);
    }
}