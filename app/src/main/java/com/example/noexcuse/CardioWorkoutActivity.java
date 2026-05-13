package com.example.noexcuse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.noexcuse.database.AppViewModel;
import com.example.noexcuse.database.PlannedExercise;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class CardioWorkoutActivity extends AppCompatActivity {

    private AppViewModel viewModel;
    private int          planId;
    private String       bodyPart;
    private LinearLayout exercisesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cardio_workout);

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        planId   = getIntent().getIntExtra("PLAN_ID", -1);
        bodyPart = getIntent().getStringExtra("PLAN_BODY_PART");
        if (planId == -1) { finish(); return; }

        // Views
        exercisesContainer = findViewById(R.id.cardioExercisesContainer);

        TextView tvTitle = findViewById(R.id.tvCardioBodyPart);
        tvTitle.setText(bodyPart != null ? bodyPart : "Cardio");

        FrameLayout btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        MaterialButton btnDone = findViewById(R.id.btnCardioDone);
        btnDone.setOnClickListener(v -> {
            Toast.makeText(this, "Cardio done! 🔥", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        });

        // Load exercises
        viewModel.getExercisesForPlan(planId).observe(this, this::buildCardioCards);
    }

    // ─── Build one card per cardio exercise ──────────────────────────────────

    private void buildCardioCards(List<PlannedExercise> list) {
        if (list == null || list.isEmpty()) return;
        exercisesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (PlannedExercise ex : list) {
            View card = inflater.inflate(R.layout.item_cardio_exercise, exercisesContainer, false);
            card.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));

            TextView tvName     = card.findViewById(R.id.tvCardioExerciseName);
            TextView tvDuration = card.findViewById(R.id.tvCardioDuration);
            TextView tvCheck    = card.findViewById(R.id.tvCardioDoneCheck);

            tvName.setText(ex.exerciseName != null ? ex.exerciseName : "—");
            tvDuration.setText(ex.durationMinutes > 0 ? ex.durationMinutes + " min" : "—");

            // Tap card → toggle done
            card.setOnClickListener(v -> {
                boolean isDone = tvCheck.getVisibility() == View.VISIBLE;
                tvCheck.setVisibility(isDone ? View.GONE : View.VISIBLE);
                card.setAlpha(isDone ? 1f : 0.55f);
            });

            exercisesContainer.addView(card);
        }
    }
}