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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<TaskItem> items       = new ArrayList<>();
    private Set<String>    verifiedIds = new HashSet<>();

    public void setItems(List<TaskItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setVerifiedIds(Set<String> ids) {
        this.verifiedIds = ids;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type == TaskItem.Type.DAILY ? 0 : 1;
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
            EducationTask edu  = item.eduTask;
            boolean verified   = verifiedIds.contains("EDU_" + edu.id);

            holder.tvTitle.setText(edu.moduleName);
            holder.tvTime.setText(sdf.format(new Date(edu.startTime)));
            holder.tvTaskLabel.setText("EDUCATION");

            if (verified) {
                applyVerifiedStyle(holder);
            } else {
                applyPendingStyle(holder, "#2196F3");
            }

            holder.itemView.setOnClickListener(v -> {
                Context ctx = v.getContext();
                Intent intent = new Intent(ctx, EducationDetailActivity.class);
                intent.putExtra("EDU_ID", edu.id);
                if (ctx instanceof MainActivity) {
                    ((MainActivity) ctx).startActivityForResult(intent, 102);
                } else {
                    ctx.startActivity(intent);
                }
            });

        } else {
            DailyTask task   = item.dailyTask;
            boolean verified = verifiedIds.contains("DAILY_" + task.id);

            holder.tvTitle.setText(task.title);
            holder.tvTime.setText(sdf.format(new Date(task.taskTime)));
            holder.tvTaskLabel.setText("TASK");

            if (verified) {
                applyVerifiedStyle(holder);
            } else {
                applyPendingStyle(holder, "#7C3AED");
            }

            holder.itemView.setOnClickListener(v -> {
                Context ctx = v.getContext();
                Intent intent = new Intent(ctx, TaskDetailActivity.class);
                intent.putExtra("TASK_ID",      task.id);
                intent.putExtra("TASK_TITLE",   task.title);
                intent.putExtra("TASK_DESC",    task.description);
                intent.putExtra("TASK_TIME",    task.taskTime);
                intent.putExtra("TASK_IS_DONE", task.isDone);
                if (ctx instanceof MainActivity) {
                    ((MainActivity) ctx).startActivityForResult(intent, 101);
                } else {
                    ctx.startActivity(intent);
                }
            });
        }
    }

    /** Pending — accent color dyal type, title white, no done badge */
    private void applyPendingStyle(TaskViewHolder h, String accentHex) {
        h.accentBar.setBackgroundColor(Color.parseColor(accentHex));
        h.tvTaskLabel.setTextColor(Color.parseColor(accentHex));
        h.tvTitle.setTextColor(Color.parseColor("#F9FAFB"));
        h.tvTitle.setPaintFlags(
                h.tvTitle.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        h.tvTime.setTextColor(Color.parseColor("#9CA3AF"));
        h.tvDoneCheck.setVisibility(View.GONE);
        h.cardRoot.setCardBackgroundColor(Color.parseColor("#1A1A1A"));
        h.cardRoot.setStrokeColor(Color.parseColor("#2A2A2A"));
    }

    /** Verified (just done this session) — green card + border, strikethrough, bright ✓ Done */
    private void applyVerifiedStyle(TaskViewHolder h) {
        h.accentBar.setBackgroundColor(Color.parseColor("#4CAF50"));
        h.tvTaskLabel.setTextColor(Color.parseColor("#4CAF50"));
        h.tvTitle.setTextColor(Color.parseColor("#6B7280"));
        h.tvTitle.setPaintFlags(
                h.tvTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        h.tvTime.setTextColor(Color.parseColor("#4B5563"));
        h.tvDoneCheck.setVisibility(View.VISIBLE);
        h.tvDoneCheck.setTextColor(Color.parseColor("#81C784"));
        h.cardRoot.setCardBackgroundColor(Color.parseColor("#0D1F0D"));
        h.cardRoot.setStrokeColor(Color.parseColor("#2E7D32"));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvTaskLabel, tvDoneCheck;
        View     accentBar;
        com.google.android.material.card.MaterialCardView cardRoot;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle     = itemView.findViewById(R.id.tvItemTitle);
            tvTime      = itemView.findViewById(R.id.tvItemTime);
            tvTaskLabel = itemView.findViewById(R.id.tvTaskLabel);
            accentBar   = itemView.findViewById(R.id.accentBar);
            tvDoneCheck = itemView.findViewById(R.id.tvDoneCheck);
            cardRoot    = (com.google.android.material.card.MaterialCardView) itemView;
        }
    }
}