package com.example.noexcuse;

import android.content.*;
import android.content.res.Configuration;
import android.os.*;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.*;
import java.util.Locale;
import java.util.Map;

import retrofit2.*;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin, btnActualiser;
    TextView textLinkRegister;
    RelativeLayout loadingLayout;
    LinearLayout layoutOffline;
    ScrollView mainContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. تطبيق اللغة المختارة
        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        setLocale(prefs.getString("lang", "en"));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 2. ربط عناصر الواجهة
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnActualiser = findViewById(R.id.btnActualiser);
        textLinkRegister = findViewById(R.id.textLinkRegister);
        loadingLayout = findViewById(R.id.loadingLayout);
        layoutOffline = findViewById(R.id.layoutOffline);
        mainContent = findViewById(R.id.mainContent);

        // 3. التنقل لصفحة التسجيل
        textLinkRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });

        // 4. منطق تسجيل الدخول
        btnLogin.setOnClickListener(v -> attemptLogin());

        // 5. زر التحديث للإنترنت
        btnActualiser.setOnClickListener(v -> {
            loadingLayout.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> {
                loadingLayout.setVisibility(View.GONE);
                checkAndShowContent();
            }, 1000);
        });

        checkAndShowContent();
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingLayout.setVisibility(View.VISIBLE);

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = task.getResult().getUser().getUid();

                        // هنا غانخدمو بـ Retrofit باش نتأكدوا أن الـ User كاين في الـ Database ديالنا
                        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
                        apiService.getUserData(uid).enqueue(new Callback<Map<String, Object>>() {
                            @Override
                            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                                loadingLayout.setVisibility(View.GONE);
                                if (response.isSuccessful() && response.body() != null) {
                                    getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                            .edit().putString("uid", uid).apply();

                                    startActivity(new Intent(LoginActivity.this, WelcomeActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(LoginActivity.this, "User not found in database", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                                loadingLayout.setVisibility(View.GONE);
                                Toast.makeText(LoginActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {
                        loadingLayout.setVisibility(View.GONE);
                        Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkAndShowContent() {
        boolean isConnected = NetworkUtils.isConnected(this);
        mainContent.setVisibility(isConnected ? View.VISIBLE : View.GONE);
        layoutOffline.setVisibility(isConnected ? View.GONE : View.VISIBLE);
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }
}