package com.example.noexcuse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * ExercisePreviewActivity
 *
 * Started from ActiveWorkoutActivity (or WarmupActivity) with:
 *   intent.putExtra("EXERCISE_NAME", exerciseName);   // e.g. "Bench Press"
 *   intent.putExtra("PLAN_BODY_PART", bodyPart);       // optional, for display
 *
 * Flow:
 *  1. Check connectivity
 *  2. Call ApiService.searchExercises(muscle=EXERCISE_NAME) → take first result
 *     (or fall back to the raw name as folder key)
 *  3. Build static image URL from BASE_URL/static/{folder}/0.jpg  and  /1.jpg
 *  4. Load & cross-fade animate the two frames (same logic as WarmupActivity)
 */
public class ExercisePreviewActivity extends AppCompatActivity {

    // ── Change this to match your server ──────────────────────────────────
    private static final String BASE_URL  = "http://192.168.1.11:8000/";
    private static final long   FRAME_MS  = 700;

    // ── Views ─────────────────────────────────────────────────────────────
    private ImageView      ivExercise;
    private ProgressBar    progressBar;
    private TextView       tvExerciseName;
    private TextView       tvMuscle;
    private TextView       tvStatus;
    private MaterialButton btnRetry;
    private View           flImageContainer;

    // ── State ─────────────────────────────────────────────────────────────
    private Bitmap  frame0, frame1;
    private boolean imagesLoaded = false;
    private Runnable animRunnable;

    private final ExecutorService executor    = Executors.newSingleThreadExecutor();
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    private String exerciseName;
    private String bodyPart;

    // ─────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_preview);

        View root = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // ── Bind views ────────────────────────────────────────────────────
        FrameLayout btnBack     = findViewById(R.id.btnBack);
        ivExercise              = findViewById(R.id.ivExercise);
        progressBar             = findViewById(R.id.progressBar);
        tvExerciseName          = findViewById(R.id.tvExerciseName);
        tvMuscle                = findViewById(R.id.tvMuscle);
        tvStatus                = findViewById(R.id.tvStatus);
        btnRetry                = findViewById(R.id.btnRetry);
        flImageContainer        = findViewById(R.id.flImageContainer);

        btnBack.setOnClickListener(v -> finish());

        // ── Read intent ───────────────────────────────────────────────────
        exerciseName = getIntent().getStringExtra("EXERCISE_NAME");
        bodyPart     = getIntent().getStringExtra("PLAN_BODY_PART");

        if (exerciseName == null || exerciseName.isEmpty()) {
            Toast.makeText(this, "No exercise name provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvExerciseName.setText(exerciseName);
        tvMuscle.setText(bodyPart != null ? bodyPart.toUpperCase() : "");

        // ── Check connectivity then load ───────────────────────────────────
        btnRetry.setOnClickListener(v -> startLoading());
        startLoading();
    }

    // ── Entry point ───────────────────────────────────────────────────────

    private void startLoading() {
        if (!isConnected()) {
            showError("📡 Pas de connexion — vérifie ton réseau");
            return;
        }

        // ── Fuzzy-match the exercise name against the global list ──────────
        // This fixes typos and wrong casing before hitting the API.
        // e.g. "bench pres" → "Barbell Bench Press - Medium Grip" (closest)
        //      "hammer curl" → "Alternate Hammer Curl"
        String corrected = ExerciseMatcher.findBestMatch(exerciseName);
        if (!corrected.equals(exerciseName)) {
            android.util.Log.d("ExercisePreview",
                    "Fuzzy match: \"" + exerciseName + "\" → \"" + corrected + "\"");
            exerciseName = corrected;
            if (tvExerciseName != null) tvExerciseName.setText(exerciseName);
        }

        showLoading();
        fetchExerciseFromApi();
    }

    // ── Connectivity ──────────────────────────────────────────────────────

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

    // ── API: search exercise by name ──────────────────────────────────────

    private void fetchExerciseFromApi() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService api = retrofit.create(ApiService.class);

        // Search by exercise name (used as muscle/keyword query)
        api.searchExercises(exerciseName).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call,
                                   Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null
                        && !response.body().isEmpty()) {

                    Map<String, Object> first = response.body().get(0);

                    // Try to extract a folder/image key from the API result
                    // Common fields: "name", "image_url", "folder", "gif_url"
                    String folder = extractImageFolder(first);

                    // Update muscle label if API returns it
                    Object muscle = first.get("muscle");
                    if (muscle != null && tvMuscle != null) {
                        mainHandler.post(() ->
                                tvMuscle.setText(muscle.toString().toUpperCase()));
                    }

                    loadImages(folder);
                } else {
                    // Fallback: use exercise name as folder key (underscores)
                    String fallbackFolder = exerciseName.trim().replace(" ", "_");
                    loadImages(fallbackFolder);
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                // Fallback to name-based folder
                String fallbackFolder = exerciseName.trim().replace(" ", "_");
                loadImages(fallbackFolder);
            }
        });
    }

    /**
     * Try to extract an image folder/key from the API response map.
     * Adjust field names to match your actual FastAPI response structure.
     */
    private String extractImageFolder(Map<String, Object> exercise) {
        // Try "folder" field first
        Object folder = exercise.get("folder");
        if (folder != null && !folder.toString().isEmpty()) return folder.toString();

        // Try "name" field → convert to folder format
        Object name = exercise.get("name");
        if (name != null && !name.toString().isEmpty()) {
            return name.toString().trim().replace(" ", "_");
        }

        // Last resort: use the original exercise name
        return exerciseName.trim().replace(" ", "_");
    }

    // ── Load images from static server ────────────────────────────────────

    private void loadImages(String folder) {
        executor.execute(() -> {
            Bitmap b0 = fetchBitmap(BASE_URL + "static/" + folder + "/0.jpg");
            Bitmap b1 = fetchBitmap(BASE_URL + "static/" + folder + "/1.jpg");
            if (b1 == null) b1 = b0;  // fallback: loop same frame

            final Bitmap f0 = b0, f1 = b1;

            mainHandler.post(() -> {
                if (f0 != null) {
                    frame0 = f0;
                    frame1 = f1;
                    imagesLoaded = true;
                    showImages();
                } else {
                    showError("Impossible de charger l'image\nVérifie le serveur");
                }
            });
        });
    }

    private Bitmap fetchBitmap(String urlStr) {
        try {
            HttpURLConnection conn =
                    (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            Bitmap bmp = BitmapFactory.decodeStream(conn.getInputStream());
            conn.disconnect();
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }

    // ── UI states ─────────────────────────────────────────────────────────

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        flImageContainer.setVisibility(View.GONE);
        tvStatus.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);
    }

    private void showImages() {
        progressBar.setVisibility(View.GONE);
        tvStatus.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);

        ivExercise.setImageBitmap(frame0);
        ivExercise.setAlpha(0f);

        flImageContainer.setVisibility(View.VISIBLE);
        flImageContainer.setAlpha(0f);
        flImageContainer.setTranslationY(30f);
        flImageContainer.animate()
                .alpha(1f).translationY(0f)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        ivExercise.animate().alpha(1f).setDuration(300).start();
        startAnim();
    }

    private void showError(String message) {
        mainHandler.post(() -> {
            progressBar.setVisibility(View.GONE);
            flImageContainer.setVisibility(View.GONE);
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText(message);
            btnRetry.setVisibility(View.VISIBLE);
        });
    }

    // ── Cross-fade animation (frame0 ↔ frame1) ────────────────────────────

    private void startAnim() {
        stopAnim();
        final boolean[] useF0 = {true};

        animRunnable = new Runnable() {
            @Override public void run() {
                if (ivExercise.getVisibility() != View.VISIBLE) return;
                useF0[0] = !useF0[0];
                final Bitmap next = useF0[0] ? frame0 : frame1;
                if (next == null) { mainHandler.postDelayed(this, FRAME_MS); return; }

                ivExercise.animate().alpha(0.15f).setDuration(150)
                        .withEndAction(() -> {
                            ivExercise.setImageBitmap(next);
                            ivExercise.animate().alpha(1f).setDuration(150).start();
                        }).start();

                mainHandler.postDelayed(this, FRAME_MS);
            }
        };
        mainHandler.postDelayed(animRunnable, FRAME_MS);
    }

    private void stopAnim() {
        if (animRunnable != null) {
            mainHandler.removeCallbacks(animRunnable);
            animRunnable = null;
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAnim();
        executor.shutdown();
        mainHandler.removeCallbacksAndMessages(null);
    }
}