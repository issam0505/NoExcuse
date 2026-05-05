package com.example.noexcuse;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.noexcuse.database.AppViewModel;
import com.example.noexcuse.database.DailyTask;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskDetailActivity extends AppCompatActivity {

    public static final String EXTRA_VERIFIED_ID = "VERIFIED_TASK_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        TextView       tvTitle       = findViewById(R.id.tvTitle);
        TextView       tvDesc        = findViewById(R.id.tvDesc);
        TextView       tvTime        = findViewById(R.id.tvTime);
        TextView       tvStatus      = findViewById(R.id.tvStatus);
        FrameLayout    btnBack       = findViewById(R.id.btnBack);
        MaterialButton btnDone       = findViewById(R.id.btnDone);
        MaterialButton btnDelete     = findViewById(R.id.btnDelete);
        ProgressBar    progressBar   = findViewById(R.id.progressBar);
        LinearLayout   contentLayout = findViewById(R.id.contentLayout);

        int     taskId   = getIntent().getIntExtra("TASK_ID", -1);
        String  title    = getIntent().getStringExtra("TASK_TITLE");
        String  desc     = getIntent().getStringExtra("TASK_DESC");
        long    taskTime = getIntent().getLongExtra("TASK_TIME", 0);
        boolean isDone   = getIntent().getBooleanExtra("TASK_IS_DONE", false);

        final boolean[] currentDone = {isDone};

        tvTitle.setText(title);
        tvDesc.setText(desc != null && !desc.isEmpty() ? desc : "No description provided");

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvTime.setText(sdf.format(new Date(taskTime)));

        Runnable refreshStatus = () -> {
            if (currentDone[0]) {
                tvStatus.setText("Done");
                tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                tvStatus.setBackgroundResource(R.drawable.bg_status_done);
                btnDone.setEnabled(false);
                btnDone.setAlpha(0.4f);
            } else {
                tvStatus.setText("Pending");
                tvStatus.setTextColor(Color.parseColor("#F59E0B"));
                tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                btnDone.setEnabled(true);
                btnDone.setAlpha(1f);
            }
        };
        refreshStatus.run();

        AppViewModel viewModel = new ViewModelProvider(this).get(AppViewModel.class);
        btnBack.setOnClickListener(v -> finish());

        // ── Mark as Done ─────────────────────────────────────────────────
        btnDone.setOnClickListener(v -> {
            if (taskId == -1) return;

            DailyTask task   = new DailyTask();
            task.id          = taskId;
            task.title       = title;
            task.description = desc;
            task.taskTime    = taskTime;
            task.isDone      = true;
            viewModel.updateTask(task);

            currentDone[0] = true;
            refreshStatus.run();

            // Notify MainActivity bach item yji f le bas b green card
            Intent result = new Intent();
            result.putExtra(EXTRA_VERIFIED_ID, taskId);
            setResult(Activity.RESULT_OK, result);
        });

        // ── Delete ───────────────────────────────────────────────────────
        btnDelete.setOnClickListener(v -> {
            if (taskId == -1) { finish(); return; }
            contentLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);

            DailyTask task   = new DailyTask();
            task.id          = taskId;
            task.title       = title;
            task.description = desc;
            task.taskTime    = taskTime;
            task.isDone      = currentDone[0];
            viewModel.deleteTask(task);

            new android.os.Handler(android.os.Looper.getMainLooper())
                    .postDelayed(this::finish, 800);
        });
    }
}