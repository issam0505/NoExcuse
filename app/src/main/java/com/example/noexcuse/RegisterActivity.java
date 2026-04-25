package com.example.noexcuse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import java.util.*;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class RegisterActivity extends AppCompatActivity {
    EditText etFirstName, etLastName, etEmail, etPassword, etConfirmPassword;
    Spinner spDay, spMonth, spYear;
    Button btnSave;
    ProgressBar progressBar;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // تطبيق اللغة قبل كل شيء
        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        setLocale(prefs.getString("lang", "en"));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spDay = findViewById(R.id.spDay);
        spMonth = findViewById(R.id.spMonth);
        spYear = findViewById(R.id.spYear);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
        mAuth = FirebaseAuth.getInstance();

        // تعيين الـ Hints مترجمة (هنا كيتحقق طلبك بالتبديل حسب اللغة)
        // قاد الأسماء هنا باش يطابقوا الـ XML اللي عندك
        etFirstName.setHint(getString(R.string.first_name));
        etLastName.setHint(getString(R.string.last_name));
        etEmail.setHint(getString(R.string.email));
        etPassword.setHint(getString(R.string.password));
        etConfirmPassword.setHint(getString(R.string.hint_confirm));
        btnSave.setText(getString(R.string.btn_save));
        etConfirmPassword.setHint(getString(R.string.hint_confirm));
        btnSave.setText(getString(R.string.btn_save));

        setupSpinners();
        btnSave.setOnClickListener(v -> registerUser());
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

    private void registerUser() {
        String pass = etPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (!pass.equals(confirmPass)) {
            etConfirmPassword.setError(getString(R.string.error_pass_match));
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);
        // ... (كود Firebase والـ Retrofit ديالك)
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}