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

    // Kol plans
    public LiveData<List<GymPlan>> getAllGymPlans() {
        return repository.getAllGymPlans();
    }

    // Plans dyal semana m3ayyana
    // weekStart = "2025-05-05" (lundi dyal had semana)
    public LiveData<List<GymPlan>> getPlansForWeek(String weekStart) {
        return repository.getPlansForWeek(weekStart);
    }

    // Jib plan dyal nhar + semana
    // exemple: getGymPlanForDayAndWeek("MONDAY", "2025-05-05", callback)
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

    // ⭐ MUHIM — dima set exerciseNameSnapshot 9bel ma tsift performance!
    // exemple:
    //   GymPerformance perf = new GymPerformance();
    //   perf.plannedExerciseId   = exercise.id;
    //   perf.exerciseNameSnapshot = exercise.exerciseName;  ← dima!
    //   perf.setNumber           = currentSet;
    //   perf.weight              = weightEntered;
    //   perf.reps                = repsEntered;
    //   perf.timestamp           = System.currentTimeMillis();
    //   viewModel.addPerformance(perf);
    public void addPerformance(GymPerformance performance) {
        repository.insertPerformance(performance);
    }

}