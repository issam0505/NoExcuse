package com.example.noexcuse;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

        // Data kamilat ji men intent - DailyTask ma3endna ma njibu mn DB
        int     taskId   = getIntent().getIntExtra("TASK_ID", -1);
        String  title    = getIntent().getStringExtra("TASK_TITLE");
        String  desc     = getIntent().getStringExtra("TASK_DESC");
        long    taskTime = getIntent().getLongExtra("TASK_TIME", 0);
        boolean isDone   = getIntent().getBooleanExtra("TASK_IS_DONE", false);

        tvTitle.setText(title);
        tvDesc.setText(desc != null && !desc.isEmpty() ? desc : "No description provided");

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvTime.setText(sdf.format(new Date(taskTime)));

        if (isDone) {
            tvStatus.setText("Done");
            tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            tvStatus.setBackgroundResource(R.drawable.bg_status_done);
        } else {
            tvStatus.setText("Pending");
            tvStatus.setTextColor(Color.parseColor("#F59E0B"));
            tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
        }

        AppViewModel viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        btnBack.setOnClickListener(v -> finish());

        // ── Mark as Done - update isDone=true f daily_tasks ─────────────
        btnDone.setOnClickListener(v -> {
            if (taskId == -1) { finish(); return; }
            DailyTask task   = new DailyTask();
            task.id          = taskId;
            task.title       = title;
            task.description = desc;
            task.taskTime    = taskTime;
            task.isDone      = true;            // ← true = kaydher f pending list mchi
            viewModel.updateTask(task);
            contentLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            new Handler(Looper.getMainLooper()).postDelayed(this::finish, 800);
        });

        // ── Delete - delete mn daily_tasks direkt ────────────────────────
        btnDelete.setOnClickListener(v -> {
            if (taskId == -1) { finish(); return; }
            DailyTask task   = new DailyTask();
            task.id          = taskId;
            task.title       = title;
            task.description = desc;
            task.taskTime    = taskTime;
            task.isDone      = isDone;
            viewModel.deleteTask(task);
            contentLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            new Handler(Looper.getMainLooper()).postDelayed(this::finish, 800);
        });
    }
}