package com.example.noexcuse;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WelcomeActivity extends AppCompatActivity {

    TextView tvWelcome;
    Button btnNext;
    Handler animationHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. التشييك: واش اليوزر ديجا شاف الترحيب؟
        boolean welcomShown = getSharedPreferences("welcomPrefs", MODE_PRIVATE)
                .getBoolean("shown", false);

        if (welcomShown) {
            // يلا ديجا شافها، دوز للـ Main ديريكت وما تافيشيش هاد الـ Layout
            startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
            finish();
            return; // هنا كنحبسو باش ما يكملش الـ onCreate
        }

        // 2. يلا أول مرة، كمل الخدمة
        setContentView(R.layout.activity_welcome);
        tvWelcome = findViewById(R.id.tvWelcome);
        btnNext = findViewById(R.id.btnNext);
        btnNext.setVisibility(View.GONE);

        // Fetch User Data
        String uid = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("uid", null);
        if (uid != null) {
            fetchUserData(uid);
        } else {
            // في حالة كان شي خطأ في الـ UID
            animateText(String.format(getString(R.string.welcome_text), "User"));
        }

        btnNext.setOnClickListener(v -> {
            // سجل بلي راه شافها
            getSharedPreferences("welcomPrefs", MODE_PRIVATE).edit().putBoolean("shown", true).apply();
            startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
            finish();
        });
    }

    private void fetchUserData(String uid) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getUserData(uid).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String firstName = (String) response.body().get("firstName");
                    String lastName = (String) response.body().get("lastName");
                    String fullName = firstName + " " + lastName;

                    animateText(String.format(getString(R.string.welcome_text), fullName));
                } else {
                    animateText(String.format(getString(R.string.welcome_text), "User"));
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                animateText(String.format(getString(R.string.welcome_text), "User"));
            }
        });
    }

    private void animateText(String text) {
        final int[] i = {0};
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (i[0] < text.length()) {
                    tvWelcome.setText(text.substring(0, i[0]++));
                    animationHandler.postDelayed(this, 15);
                } else {
                    // ملي تسالي الكتابة، بين الزر
                    btnNext.setVisibility(View.VISIBLE);
                    btnNext.animate().alpha(1f).setDuration(500);
                }
            }
        };
        animationHandler.post(runnable);
    }
}