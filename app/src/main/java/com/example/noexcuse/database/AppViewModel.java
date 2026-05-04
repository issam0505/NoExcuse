package com.example.noexcuse.database;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class AppViewModel extends AndroidViewModel {

    private final AppRepository repository;

    // LiveData مباشرة — الـ UI يـ observe بدون ما يعرف شي على الـ DB
    public final LiveData<List<DailyTask>> pendingTasks;

    public AppViewModel(@NonNull Application application) {
        super(application);
        repository   = new AppRepository(application);
        pendingTasks = repository.getPendingTasks();

    }

    // ─── TASKS ───────────────────────────────
    public void addTask(DailyTask task) {
        repository.insertTask(task);
    }

    public void updateTask(DailyTask task) {
        repository.updateTask(task);
    }

    // ─── EDUCATION ───────────────────────────
    public void addEducation(EducationTask task) {
        repository.insertEducation(task);
    }

    // ─── GYM ─────────────────────────────────
    /**
     * الـ flow الصحيح:
     * 1. addGymPlan → callback يرجع planId
     * 2. addPlannedExercise(exercise بـ planId) → callback يرجع exerciseId
     * 3. addPerformance(performance بـ exerciseId)
     */
    public void addGymPlan(GymPlan plan, AppRepository.OnIdGeneratedCallback callback) {
        repository.insertGymPlan(plan, callback);
    }

    public void addPlannedExercise(PlannedExercise exercise, AppRepository.OnIdGeneratedCallback callback) {
        repository.insertPlannedExercise(exercise, callback);
    }

    public void addPerformance(GymPerformance performance) {
        repository.insertPerformance(performance);
    }

    public LiveData<List<GymPlan>> getAllGymPlans() {
        return repository.getAllGymPlans();
    }

    public LiveData<List<PlannedExercise>> getExercisesForPlan(int planId) {
        return repository.getExercisesForPlan(planId);
    }


}