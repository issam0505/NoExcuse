package com.example.noexcuse;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class TaskDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        // ربط العناصر
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvDesc = findViewById(R.id.tvDesc);
        Button btnBack = findViewById(R.id.btnBack);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        LinearLayout contentLayout = findViewById(R.id.contentLayout);

        // جلب البيانات
        String title = getIntent().getStringExtra("TASK_TITLE");
        String desc = getIntent().getStringExtra("TASK_DESC");

        tvTitle.setText(title);
        tvDesc.setText(desc != null && !desc.isEmpty() ? desc : "No description provided");

        // Action ديال البوتون
        btnBack.setOnClickListener(v -> {
            // إخفاء المحتوى وإظهار الـ Loading فالوسط
            contentLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);

            // تأخير 1 ثانية باش يبان الـ Loading
            new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1000);
        });
    }
}