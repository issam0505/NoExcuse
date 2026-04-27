package com.example.noexcuse;

import android.content.*;
import android.content.res.Configuration;
import android.os.*;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.*;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class RegisterActivity extends AppCompatActivity {

    EditText etFirstName, etLastName, etEmail, etPassword, etConfirmPassword;
    Spinner spDay, spMonth, spYear;
    Button btnSave, btnActualiser;
    TextView textLink;
    LinearLayout layoutOffline;
    ScrollView mainContent;
    RelativeLayout loadingLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        setLocale(prefs.getString("lang", "en"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // ربط العناصر
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
        textLink = findViewById(R.id.textLink);
        layoutOffline = findViewById(R.id.layoutOffline);
        mainContent = findViewById(R.id.mainContent);
        loadingLayout = findViewById(R.id.loadingLayout);

        // التنقل لصفحة Login
        textLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        btnActualiser.setOnClickListener(v -> {
            loadingLayout.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> {
                loadingLayout.setVisibility(View.GONE);
                checkAndShowContent();
            }, 1000);
        });

        btnSave.setOnClickListener(v -> attemptRegistration());

        setupSpinners();
        checkAndShowContent();
    }

    private void attemptRegistration() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();
        String fName = etFirstName.getText().toString().trim();
        String lName = etLastName.getText().toString().trim();
        String birth = spDay.getSelectedItem().toString() + "/" + spMonth.getSelectedItem().toString() + "/" + spYear.getSelectedItem().toString();

        if (email.isEmpty() || password.isEmpty() || !password.equals(confirm)) {
            Toast.makeText(this, getString(R.string.error_pass_match), Toast.LENGTH_SHORT).show();
            return;
        }

        loadingLayout.setVisibility(View.VISIBLE);

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        sendToFastAPI(task.getResult().getUser(), email, fName, lName, birth);
                    } else {
                        loadingLayout.setVisibility(View.GONE);
                        Toast.makeText(this, "Auth Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // داخل RegisterActivity - تحديث ميثود sendToFastAPI فقط
    private void sendToFastAPI(FirebaseUser firebaseUser, String email, String fName, String lName, String birth) {
        // استعمال الـ Singleton اللي صايبنا في RetrofitClient
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", firebaseUser.getUid());
        userData.put("email", email);
        userData.put("firstName", fName);
        userData.put("lastName", lName);
        userData.put("birthDate", birth);

        apiService.registerUser(userData).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    loadingLayout.setVisibility(View.GONE);
                    getSharedPreferences("UserPrefs", MODE_PRIVATE)
                            .edit().putString("uid", firebaseUser.getUid()).apply();

                    Toast.makeText(RegisterActivity.this, getString(R.string.success_message), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, WelcomeActivity.class));
                    finish();
                } else {
                    handleRollback(firebaseUser, "API Server Error");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                handleRollback(firebaseUser, "Network Error: " + t.getMessage());
            }
        });
    }

    private void handleRollback(FirebaseUser user, String error) {
        user.delete().addOnCompleteListener(task -> {
            loadingLayout.setVisibility(View.GONE);
            Toast.makeText(RegisterActivity.this, error + " - User Registration Cancelled.", Toast.LENGTH_LONG).show();
        });
    }

    private void checkAndShowContent() {
        boolean isConnected = NetworkUtils.isConnected(this);
        mainContent.setVisibility(isConnected ? View.VISIBLE : View.GONE);
        layoutOffline.setVisibility(isConnected ? View.GONE : View.VISIBLE);
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