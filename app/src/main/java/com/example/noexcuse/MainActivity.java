package com.example.noexcuse;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.noexcuse.database.AppViewModel;
import com.example.noexcuse.database.DailyTask;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private FloatingActionButton fabAdd;
    private RecyclerView recyclerView;
    private TextView tvDate, tvMotivation;
    private DrawerLayout drawerLayout;
    private ImageView btnMenu;
    private Button btnDash, btnSettings, btnAI;
    private Switch swGym, swEdu;
    private TaskAdapter taskAdapter;

    public boolean isGymModeEnabled = false;
    public boolean isEducationModeEnabled = false;
    private AppViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init UI
        drawerLayout = findViewById(R.id.drawer_layout);
        btnMenu      = findViewById(R.id.btnMenu);
        fabAdd       = findViewById(R.id.fabAdd);
        recyclerView = findViewById(R.id.recyclerView);
        tvDate       = findViewById(R.id.tvDate);
        tvMotivation = findViewById(R.id.tvMotivation);
        btnDash      = findViewById(R.id.btnDash);
        btnSettings  = findViewById(R.id.btnSettings);
        btnAI        = findViewById(R.id.btnAI);
        swGym        = findViewById(R.id.swGym);
        swEdu        = findViewById(R.id.swEdu);

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter();
        recyclerView.setAdapter(taskAdapter);

        // Apply Falling Animation
        LayoutAnimationController anim = AnimationUtils.loadLayoutAnimation(this, R.anim.recycler_fall_layout);
        recyclerView.setLayoutAnimation(anim);

        // Observe Data
        viewModel.pendingTasks.observe(this, tasks -> {
            taskAdapter.setTasks(tasks);
            recyclerView.scheduleLayoutAnimation();
        });

        // Date & Motivation
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(new Date()));
        String[] quotes = getResources().getStringArray(R.array.motivation_quotes);
        if (quotes.length > 0) {
            tvMotivation.setText(quotes[new Random().nextInt(quotes.length)]);
        }

        // Listeners
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));
        btnDash.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.END));
        btnSettings.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.END));
        btnAI.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.END));

        swGym.setOnCheckedChangeListener((buttonView, isChecked) -> isGymModeEnabled = isChecked);
        swEdu.setOnCheckedChangeListener((buttonView, isChecked) -> isEducationModeEnabled = isChecked);

        fabAdd.setOnClickListener(v -> openSmartAddMenu());
    }

    private void openSmartAddMenu() {
        if (!isGymModeEnabled && !isEducationModeEnabled) {
            openDailyTaskDialog();
            return;
        }

        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_choose_mode, null);
        sheet.setContentView(view);

        sheet.setOnShowListener(dialog -> {
            View bottomSheet = (View) view.getParent();
            if (bottomSheet != null) {
                ((View) bottomSheet.getParent()).setBackgroundColor(Color.TRANSPARENT);
                bottomSheet.setBackgroundColor(Color.parseColor("#111111"));
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });

        MaterialButton btnDaily = view.findViewById(R.id.btnModeDaily);
        MaterialButton btnGym   = view.findViewById(R.id.btnModeGym);
        MaterialButton btnEdu   = view.findViewById(R.id.btnModeEdu);

        btnGym.setVisibility(isGymModeEnabled  ? View.VISIBLE : View.GONE);
        btnEdu.setVisibility(isEducationModeEnabled ? View.VISIBLE : View.GONE);

        btnDaily.setOnClickListener(v -> { sheet.dismiss(); openDailyTaskDialog(); });
        btnGym.setOnClickListener(v -> { sheet.dismiss(); Toast.makeText(this, "Gym Form Coming Next 🔥", Toast.LENGTH_SHORT).show(); });
        btnEdu.setOnClickListener(v -> { sheet.dismiss(); Toast.makeText(this, "Education Form Coming Next 🔥", Toast.LENGTH_SHORT).show(); });

        sheet.show();
    }

    private void openDailyTaskDialog() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        sheet.setContentView(view);

        sheet.setOnShowListener(dialog -> {
            View bottomSheet = (View) view.getParent();
            if (bottomSheet != null) {
                ((View) bottomSheet.getParent()).setBackgroundColor(Color.TRANSPARENT);
                bottomSheet.setBackgroundColor(Color.parseColor("#111111"));
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });

        TextInputEditText etTask  = view.findViewById(R.id.etTaskName);
        TextInputEditText etDesc  = view.findViewById(R.id.etTaskDescription);
        TextInputEditText etTime  = view.findViewById(R.id.etTaskTime);
        MaterialButton    btnSave = view.findViewById(R.id.btnSaveTask);

        Calendar calendar = Calendar.getInstance();

        etTime.setOnClickListener(v ->
                new TimePickerDialog(this, (tp, hour, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                    calendar.set(Calendar.SECOND, 0);
                    etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        );

        btnSave.setOnClickListener(v -> {
            String taskName = etTask.getText() != null ? etTask.getText().toString().trim() : "";
            String taskDesc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
            String taskTime = etTime.getText() != null ? etTime.getText().toString().trim() : "";

            if (taskName.isEmpty() || taskTime.isEmpty()) {
                Toast.makeText(this, "Fill Name and Time", Toast.LENGTH_SHORT).show();
                return;
            }

            DailyTask task = new DailyTask();
            task.title       = taskName;
            task.description = taskDesc;
            task.taskTime    = calendar.getTimeInMillis();
            task.isDone      = false;
            task.isSynced    = false;
            viewModel.addTask(task);

            Toast.makeText(this, "Task Added! 🔥", Toast.LENGTH_SHORT).show();
            sheet.dismiss();
        });

        sheet.show();
    }
}