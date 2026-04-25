package com.example.noexcuse;

import android.content.*;
import android.content.res.Configuration;
import android.os.*;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import java.util.*;

public class RegisterActivity extends AppCompatActivity {

    EditText etFirstName, etLastName, etEmail, etPassword, etConfirmPassword;
    Spinner spDay, spMonth, spYear;
    Button btnSave, btnActualiser;
    LinearLayout layoutOffline;
    ScrollView mainContent;
    TextView textLink, tvOfflineMsg;
    RelativeLayout loadingLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // تطبيق اللغة المخزنة
        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        setLocale(prefs.getString("lang", "en"));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // الربط مع الـ Views
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spDay = findViewById(R.id.spDay);
        spMonth = findViewById(R.id.spMonth);
        spYear = findViewById(R.id.spYear);
        btnSave = findViewById(R.id.btnSave);
        btnActualiser = findViewById(R.id.btnActualiser);
        layoutOffline = findViewById(R.id.layoutOffline);
        mainContent = findViewById(R.id.mainContent);
        textLink = findViewById(R.id.textLink);
        tvOfflineMsg = findViewById(R.id.tvOfflineMsg);
        loadingLayout = findViewById(R.id.loadingLayout);

        // الإجراءات
        textLink.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));

        // زر التحديث (مع Loading)
        btnActualiser.setOnClickListener(v -> {
            loadingLayout.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> {
                loadingLayout.setVisibility(View.GONE);
                checkAndShowContent();
            }, 1000);
        });

        // زر التسجيل (مع Loading)
        btnSave.setOnClickListener(v -> attemptRegistration());

        setupSpinners();
        checkAndShowContent();
    }

    private void attemptRegistration() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || !password.equals(confirm)) {
            Toast.makeText(this, getString(R.string.error_pass_match), Toast.LENGTH_SHORT).show();
            return;
        }

        loadingLayout.setVisibility(View.VISIBLE);

        // 1. التسجيل في Firebase
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 2. هنا يتم استدعاء Retrofit لإرسال البيانات لـ FastAPI
                        // بعد استلام رد النجاح من FastAPI قم بإنهاء الـ Loading
                        Toast.makeText(this, getString(R.string.success_message), Toast.LENGTH_SHORT).show();
                        loadingLayout.setVisibility(View.GONE);
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        loadingLayout.setVisibility(View.GONE);
                        Toast.makeText(this, getString(R.string.error_message), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndShowContent();
    }

    private void checkAndShowContent() {
        if (NetworkUtils.isConnected(this)) {
            mainContent.setVisibility(View.VISIBLE);
            layoutOffline.setVisibility(View.GONE);
        } else {
            mainContent.setVisibility(View.GONE);
            layoutOffline.setVisibility(View.VISIBLE);
        }
    }

    private void setupSpinners() {
        Integer[] days = new Integer[31]; for(int i=0; i<31; i++) days[i] = i+1;
        spDay.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, days));
        Integer[] months = new Integer[12]; for(int i=0; i<12; i++) months[i] = i+1;
        spMonth.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, months));
        Integer[] years = new Integer[100];
        int curr = Calendar.getInstance().get(Calendar.YEAR);
        for(int i=0; i<100; i++) years[i] = curr - i;
        spYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, years));
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }
}