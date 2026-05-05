package com.example.noexcuse;

import com.example.noexcuse.database.DailyTask;
import com.example.noexcuse.database.EducationTask;

/**
 * Wrapper propre - kol item f RecyclerView ykon soit DailyTask soit EducationTask.
 * Machi haja laseqa - kol wahda f table dyalha.
 */
public class TaskItem {

    public enum Type { DAILY, EDUCATION }

    public final Type         type;
    public final DailyTask    dailyTask;    // non-null ila type == DAILY
    public final EducationTask eduTask;     // non-null ila type == EDUCATION

    public TaskItem(DailyTask task) {
        this.type      = Type.DAILY;
        this.dailyTask = task;
        this.eduTask   = null;
    }

    public TaskItem(EducationTask task) {
        this.type      = Type.EDUCATION;
        this.dailyTask = null;
        this.eduTask   = task;
    }

    /** Time bach ntertibo f RecyclerView */
    public long getSortTime() {
        return type == Type.DAILY ? dailyTask.taskTime : eduTask.startTime;
    }
}