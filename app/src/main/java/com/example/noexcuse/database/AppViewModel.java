package com.example.noexcuse.database;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    // ─── DAILY TASKS ─────────────────────────
    public void addTask(DailyTask task) {
        repository.insertTask(task);
    }

    public void updateTask(DailyTask task) {
        repository.updateTask(task);
    }

    public void deleteTask(DailyTask task) {
        repository.deleteTask(task);
    }

    // ─── EDUCATION ───────────────────────────
    public void addEducation(EducationTask task) {
        repository.insertEducation(task);
    }

    public void updateEducation(EducationTask task) {
        repository.updateEducation(task);
    }

    public void deleteEducation(EducationTask task) {
        repository.deleteEducation(task);
    }

    /**
     * Blocking call - tsta3mlha f background thread (Executors) bas
     * EducationDetailActivity katsta3mlha hekda
     */
    public EducationTask getEducationById(int id) {
        return repository.getEducationById(id);
    }

    // ─── GYM ─────────────────────────────────
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