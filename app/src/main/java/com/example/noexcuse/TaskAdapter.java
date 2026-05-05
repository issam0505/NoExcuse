package com.example.noexcuse;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.noexcuse.database.DailyTask;
import com.example.noexcuse.database.EducationTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private static final int TYPE_DAILY     = 0;
    private static final int TYPE_EDUCATION = 1;

    private List<TaskItem> items = new ArrayList<>();

    /** MainActivity tsta3mlo bach t3ti les deux listes */
    public void setItems(List<TaskItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type == TaskItem.Type.DAILY ? TYPE_DAILY : TYPE_EDUCATION;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskItem item = items.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        if (item.type == TaskItem.Type.EDUCATION) {
            EducationTask edu = item.eduTask;

            holder.tvTitle.setText(edu.moduleName);
            holder.tvTime.setText(sdf.format(new Date(edu.startTime)));
            holder.accentBar.setBackgroundColor(Color.parseColor("#2196F3"));
            holder.tvTaskLabel.setText("EDUCATION");
            holder.tvTaskLabel.setTextColor(Color.parseColor("#2196F3"));
            holder.tvTime.setTextColor(Color.parseColor("#9CA3AF"));

            holder.itemView.setOnClickListener(v -> {
                Context ctx = v.getContext();
                Intent intent = new Intent(ctx, EducationDetailActivity.class);
                // Npassiw bas l'id - detail tji tjib data mn DB
                intent.putExtra("EDU_ID", edu.id);
                ctx.startActivity(intent);
            });

        } else {
            DailyTask task = item.dailyTask;

            holder.tvTitle.setText(task.title);
            holder.tvTime.setText(sdf.format(new Date(task.taskTime)));
            holder.accentBar.setBackgroundColor(Color.parseColor("#7C3AED"));
            holder.tvTaskLabel.setText("TASK");
            holder.tvTaskLabel.setTextColor(Color.parseColor("#7C3AED"));
            holder.tvTime.setTextColor(Color.parseColor("#9CA3AF"));

            holder.itemView.setOnClickListener(v -> {
                Context ctx = v.getContext();
                Intent intent = new Intent(ctx, TaskDetailActivity.class);
                // DailyTask data kamilat f intent - machi haja f DB khassna njibo
                intent.putExtra("TASK_ID",      task.id);
                intent.putExtra("TASK_TITLE",   task.title);
                intent.putExtra("TASK_DESC",    task.description);
                intent.putExtra("TASK_TIME",    task.taskTime);
                intent.putExtra("TASK_IS_DONE", task.isDone);
                ctx.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvTaskLabel;
        View     accentBar;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle     = itemView.findViewById(R.id.tvItemTitle);
            tvTime      = itemView.findViewById(R.id.tvItemTime);
            tvTaskLabel = itemView.findViewById(R.id.tvTaskLabel);
            accentBar   = itemView.findViewById(R.id.accentBar);
        }
    }
}