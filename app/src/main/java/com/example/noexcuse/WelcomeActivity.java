package com.example.noexcuse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WelcomeActivity extends AppCompatActivity {

    TextView tvWelcome, tvLoading;
    Button btnNext;
    List<String> pages = new ArrayList<>();
    int currentPage = 0;
    Handler animationHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. تطبيق اللغة المختارة
        SharedPreferences langPrefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        setLocale(langPrefs.getString("lang", "en"));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvLoading = findViewById(R.id.tvLoading);
        btnNext = findViewById(R.id.btnNext);

        tvLoading.setText(getString(R.string.loading));
        tvLoading.setVisibility(View.VISIBLE);

        // 2. جيب الـ UID من الـ Shared
        String uid = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("uid", null);

        if (uid != null) {
            fetchUserDataFromApi(uid);
        } else {
            setupPages("User", "");
            loadPage(0);
        }
    }

    private void fetchUserDataFromApi(String uid) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getUserData(uid).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String firstName = (String) response.body().get("firstName");
                    String lastName = (String) response.body().get("lastName");
                    setupPages(firstName, lastName);
                } else {
                    setupPages("User", "");
                }
                loadPage(0);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setupPages("User", "");
                loadPage(0);
            }
        });
    }

    private void setupPages(String firstName, String lastName) {
        String fullName = (firstName + " " + lastName).trim();
        pages.clear();
        pages.add(String.format(getString(R.string.welcome_p1), fullName));
        pages.add(getString(R.string.welcome_p2));
        pages.add(getString(R.string.welcome_p3));
    }

    private void loadPage(int index) {
        tvWelcome.setText("");
        btnNext.setVisibility(View.GONE);

        if (index == pages.size() - 1) {
            btnNext.setText(getString(R.string.btn_start));
        } else {
            btnNext.setText(getString(R.string.btn_next));
        }

        animateText(pages.get(index));
    }

    private void animateText(String text) {
        final int[] i = {0};
        tvLoading.setVisibility(View.GONE);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (i[0] < text.length()) {
                    tvWelcome.setText(text.substring(0, i[0]++));
                    animationHandler.postDelayed(this, 10);
                } else {
                    btnNext.setVisibility(View.VISIBLE);
                }
            }
        };
        animationHandler.post(runnable);

        btnNext.setOnClickListener(v -> {
            if (currentPage < pages.size() - 1) {
                currentPage++;
                loadPage(currentPage);
            } else {
                // هادي هي اللي كتقول للتطبيق "راه صافي، هاد اليوزر داز من الترحيب"
                getSharedPreferences("welcomPrefs", MODE_PRIVATE).edit().putBoolean("shown", true).apply();
                startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }
}