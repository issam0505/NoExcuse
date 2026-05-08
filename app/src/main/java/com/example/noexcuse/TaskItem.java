package com.example.noexcuse;

import com.example.noexcuse.database.DailyTask;
import com.example.noexcuse.database.EducationTask;
import com.example.noexcuse.database.GymPlan;

/**
 * Wrapper propre - kol item f RecyclerView ykon soit DailyTask, EducationTask, ou GymPlan.
 */
public class TaskItem {

    public enum Type { DAILY, EDUCATION, GYM }

    public final Type          type;
    public final DailyTask     dailyTask;   // non-null ila type == DAILY
    public final EducationTask eduTask;     // non-null ila type == EDUCATION
    public final GymPlan       gymPlan;     // non-null ila type == GYM

    public TaskItem(DailyTask task) {
        this.type      = Type.DAILY;
        this.dailyTask = task;
        this.eduTask   = null;
        this.gymPlan   = null;
    }

    public TaskItem(EducationTask task) {
        this.type      = Type.EDUCATION;
        this.dailyTask = null;
        this.eduTask   = task;
        this.gymPlan   = null;
    }

    public TaskItem(GymPlan plan) {
        this.type      = Type.GYM;
        this.dailyTask = null;
        this.eduTask   = null;
        this.gymPlan   = plan;
    }

    /** Time bach ntertibo f RecyclerView */
    public long getSortTime() {
        if (type == Type.DAILY)     return dailyTask.taskTime;
        if (type == Type.EDUCATION) return eduTask.startTime;
        // GYM — startTime hiya String "07:30", nconvertiwha l millis dyal lyoam
        return parseGymStartTime(gymPlan.startTime);
    }

    /** Convert "HH:mm" string l timestamp dyal lyoam bach nqdroh ntertibo */
    private long parseGymStartTime(String startTime) {
        if (startTime == null || startTime.isEmpty()) return 0;
        try {
            String[] parts = startTime.split(":");
            int hour   = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.HOUR_OF_DAY, hour);
            cal.set(java.util.Calendar.MINUTE, minute);
            cal.set(java.util.Calendar.SECOND, 0);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            return 0;
        }
    }
}