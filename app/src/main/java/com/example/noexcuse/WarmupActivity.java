package com.example.noexcuse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WarmupActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://192.168.1.11:8000";

    private static final String[][] EXERCISES = {
            { "Band Pull Apart", "Band_Pull_Apart", "Rear Deltoids" },
            { "Arm Circles",     "Arm_Circles",      "Shoulders"    },
            { "Push-Ups",        "Pushups",           "Chest"       }
    };

    private final Bitmap[]   frame0       = new Bitmap[3];
    private final Bitmap[]   frame1       = new Bitmap[3];
    private final boolean[]  loaded       = new boolean[3];
    private final Runnable[] animRunnable = new Runnable[3];

    private final ExecutorService executor    = Executors.newFixedThreadPool(3);
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    private LinearLayout exercisesContainer;

    // ─────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warmup);

        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        FrameLayout btnBack = findViewById(R.id.btnBackWarmupActivity);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        exercisesContainer = findViewById(R.id.warmupExercisesContainer);
        buildExerciseCards();
    }

    // ── Connexion ─────────────────────────────────────────────────────────
    private boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return nc != null && (
                nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)     ||
                        nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    private void showNoConnectionSnackbar(View anchor) {
        Snackbar.make(anchor,
                        "📡 Pas de connexion — vérifie ton réseau",
                        Snackbar.LENGTH_LONG)
                .setBackgroundTint(0xFF1A1A1A)
                .setTextColor(0xFFFF6D00)
                .show();
    }

    // ── Build cards ───────────────────────────────────────────────────────
    private void buildExerciseCards() {
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < EXERCISES.length; i++) {
            final int idx = i;
            String[]  ex  = EXERCISES[i];

            View card = inflater.inflate(
                    R.layout.item_warmup_exercise, exercisesContainer, false);

            // Slide-up staggeré à l'entrée
            card.setAlpha(0f);
            card.setTranslationY(50f);
            card.animate()
                    .alpha(1f).translationY(0f)
                    .setDuration(380).setStartDelay(idx * 120L)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();

            TextView       tvNumber      = card.findViewById(R.id.tvWarmupExNumber);
            TextView       tvName        = card.findViewById(R.id.tvWarmupExName);
            TextView       tvTarget      = card.findViewById(R.id.tvWarmupExTarget);
            MaterialButton btnShow       = card.findViewById(R.id.btnShowExercise);
            ImageView      ivEx          = card.findViewById(R.id.ivWarmupExerciseCard);
            ProgressBar    pb            = card.findViewById(R.id.pbWarmupCard);
            View           flContainer   = card.findViewById(R.id.flImageContainer);

            tvNumber.setText(String.valueOf(i + 1));
            tvName.setText(ex[0]);
            tvTarget.setText(ex[2].toUpperCase());

            flContainer.setVisibility(View.GONE);
            ivEx.setAlpha(0f);
            pb.setVisibility(View.GONE);

            btnShow.setOnClickListener(v -> {

                if (!loaded[idx] && !isConnected()) {
                    showNoConnectionSnackbar(card);
                    return;
                }

                if (loaded[idx]) {
                    if (flContainer.getVisibility() == View.VISIBLE) {
                        // ── Hide ──
                        stopAnim(idx);
                        ivEx.animate().alpha(0f).setDuration(200)
                                .withEndAction(() -> {
                                    flContainer.setVisibility(View.GONE);
                                    ivEx.setAlpha(0f);
                                    btnShow.setText("Show");
                                }).start();
                    } else {
                        // ── Show ──
                        flContainer.setVisibility(View.VISIBLE);
                        ivEx.setAlpha(0f);
                        ivEx.animate().alpha(1f).setDuration(300).start();
                        btnShow.setText("Hide");
                        startAnim(idx, ivEx);
                    }
                } else {
                    pb.setVisibility(View.VISIBLE);
                    btnShow.setEnabled(false);
                    loadImages(idx, ex[1], ivEx, flContainer, pb, btnShow);
                }
            });

            exercisesContainer.addView(card);
        }
    }

    // ── Load images ───────────────────────────────────────────────────────
    private void loadImages(int idx, String folder,
                            ImageView iv, View flContainer,
                            ProgressBar pb, MaterialButton btn) {
        executor.execute(() -> {
            Bitmap b0 = fetchBitmap(BASE_URL + "/static/" + folder + "/0.jpg");
            Bitmap b1 = fetchBitmap(BASE_URL + "/static/" + folder + "/1.jpg");
            if (b1 == null) b1 = b0;

            final Bitmap f0 = b0, f1 = b1;
            mainHandler.post(() -> {
                pb.setVisibility(View.GONE);
                btn.setEnabled(true);

                if (f0 != null) {
                    frame0[idx] = f0;
                    frame1[idx] = f1;
                    loaded[idx] = true;

                    iv.setImageBitmap(f0);
                    iv.setAlpha(0f);
                    flContainer.setVisibility(View.VISIBLE);
                    iv.animate().alpha(1f).setDuration(300).start();

                    btn.setText("Hide");
                    startAnim(idx, iv);
                } else {
                    btn.setText("✕ Error");
                }
            });
        });
    }

    // ── Toggle animation (cross-fade frame0 ↔ frame1) ────────────────────
    private static final long FRAME_MS = 700;

    private void startAnim(int idx, ImageView iv) {
        stopAnim(idx);
        final boolean[] useF0 = {true};

        Runnable r = new Runnable() {
            @Override public void run() {
                if (iv.getVisibility() != View.VISIBLE) return;

                useF0[0] = !useF0[0];
                final Bitmap next = useF0[0] ? frame0[idx] : frame1[idx];
                if (next == null) { mainHandler.postDelayed(this, FRAME_MS); return; }

                iv.animate().alpha(0.15f).setDuration(150)
                        .withEndAction(() -> {
                            iv.setImageBitmap(next);
                            iv.animate().alpha(1f).setDuration(150).start();
                        }).start();

                mainHandler.postDelayed(this, FRAME_MS);
            }
        };

        animRunnable[idx] = r;
        mainHandler.postDelayed(r, FRAME_MS);
    }

    private void stopAnim(int idx) {
        if (animRunnable[idx] != null) {
            mainHandler.removeCallbacks(animRunnable[idx]);
            animRunnable[idx] = null;
        }
    }

    // ── Fetch bitmap ──────────────────────────────────────────────────────
    private Bitmap fetchBitmap(String urlStr) {
        try {
            HttpURLConnection conn =
                    (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            Bitmap bmp = BitmapFactory.decodeStream(conn.getInputStream());
            conn.disconnect();
            return bmp;
        } catch (Exception e) { return null; }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        mainHandler.removeCallbacksAndMessages(null);
    }
}