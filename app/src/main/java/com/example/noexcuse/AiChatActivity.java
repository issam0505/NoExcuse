package com.example.noexcuse;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.noexcuse.database.AppDatabase;
import com.example.noexcuse.database.DailyTask;
import com.example.noexcuse.database.EducationTask;
import com.example.noexcuse.database.GymPlan;
import com.example.noexcuse.database.PlannedExercise;
import com.example.noexcuse.database.SleepSettings;
import com.example.noexcuse.database.WeekUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiChatActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "AiChatActivity";
    private static final String TTS_UTTERANCE_ID = "noexcuse_ai_reply";
    private static final Locale AI_VOICE_LOCALE = Locale.FRANCE;

    private AiAvatarView avatarView;
    private LinearLayout chatContainer;
    private ScrollView scrollChat;
    private TextInputEditText etMessage;
    private MaterialButton btnSend;
    private MaterialButton btnMic;
    private ApiService apiService;
    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private boolean isTtsReady = false;
    private boolean isListening = false;
    private boolean shouldSpeakNextReply = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<String> audioPermissionLauncher;
    private Call<Map<String, Object>> activeCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        FrameLayout btnBack = findViewById(R.id.btnBackAi);
        avatarView = findViewById(R.id.avatarView);
        chatContainer = findViewById(R.id.chatContainer);
        scrollChat = findViewById(R.id.scrollChat);
        etMessage = findViewById(R.id.etAiMessage);
        btnSend = findViewById(R.id.btnSendAi);
        btnMic = findViewById(R.id.btnMicAi);
        apiService = RetrofitClient.getClient().create(ApiService.class);

        audioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        startVoiceInput();
                    } else {
                        Toast.makeText(this, "Microphone permission is required for voice chat", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        setupTextToSpeech();
        setupSpeechRecognizer();

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage(false));
        btnMic.setOnClickListener(v -> requestVoiceInput());

        addAiMessage("Bonjour ! Je suis votre assistant IA général. Posez-moi votre question et je vous répondrai.");
    }

    @Override
    protected void onDestroy() {
        if (activeCall != null) {
            activeCall.cancel();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        dbExecutor.shutdown();
        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        isTtsReady = status == TextToSpeech.SUCCESS;
        if (isTtsReady) {
            int result = textToSpeech.setLanguage(AI_VOICE_LOCALE);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech.setLanguage(Locale.FRENCH);
            }
            selectPreferredFrenchVoice();
            textToSpeech.setSpeechRate(0.92f);
            textToSpeech.setPitch(0.86f);
        }
    }

    private void selectPreferredFrenchVoice() {
        if (textToSpeech == null || textToSpeech.getVoices() == null) return;

        Voice bestVoice = null;
        int bestScore = Integer.MIN_VALUE;
        for (Voice voice : textToSpeech.getVoices()) {
            if (voice == null || voice.getLocale() == null) continue;

            String name = voice.getName() != null ? voice.getName().toLowerCase(Locale.US) : "";
            Locale locale = voice.getLocale();
            if (!Locale.FRENCH.getLanguage().equals(locale.getLanguage())) continue;

            boolean soundsMale = name.contains("male")
                    || name.contains("man")
                    || name.contains("homme")
                    || name.contains("masc")
                    || name.contains("baritone")
                    || name.contains("low");
            boolean soundsFemale = name.contains("female")
                    || name.contains("woman")
                    || name.contains("femme")
                    || name.contains("girl");

            int score = 0;
            if (AI_VOICE_LOCALE.getCountry().equals(locale.getCountry())) score += 35;
            if (soundsMale && !soundsFemale) score += 24;
            if (voice.getQuality() >= Voice.QUALITY_HIGH) score += 18;
            if (voice.getQuality() >= Voice.QUALITY_VERY_HIGH) score += 24;
            if (!voice.isNetworkConnectionRequired()) score += 10;
            if (voice.getLatency() <= Voice.LATENCY_NORMAL) score += 8;
            if (name.contains("fr-fr") || name.contains("fra-fra") || name.contains("france")) score += 18;
            if (name.contains("enhanced") || name.contains("premium") || name.contains("neural")) score += 12;
            if (soundsFemale) score -= 10;

            if (score > bestScore) {
                bestScore = score;
                bestVoice = voice;
            }
        }

        if (bestVoice != null) {
            textToSpeech.setVoice(bestVoice);
            Log.d(TAG, "Selected French-accent TTS voice: " + bestVoice.getName()
                    + ", locale=" + bestVoice.getLocale()
                    + ", quality=" + bestVoice.getQuality()
                    + ", network=" + bestVoice.isNetworkConnectionRequired());
        }
    }

    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(this, this);
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                handler.post(() -> avatarView.setAvatarState(AiAvatarView.STATE_SPEAKING));
            }

            @Override
            public void onDone(String utteranceId) {
                handler.post(() -> avatarView.setAvatarState(AiAvatarView.STATE_IDLE));
            }

            @Override
            public void onError(String utteranceId) {
                handler.post(() -> avatarView.setAvatarState(AiAvatarView.STATE_IDLE));
            }
        });
    }

    private void setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            btnMic.setEnabled(false);
            btnMic.setIconResource(R.drawable.ic_mic_off_24);
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault());
        speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to NoExcuse AI");

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { setListening(true); }
            @Override public void onBeginningOfSpeech() { setListening(true); }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { setListening(false); }

            @Override
            public void onError(int error) {
                setListening(false);
                Toast.makeText(AiChatActivity.this, speechErrorMessage(error), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                setListening(false);
                handleSpeechResults(results, true);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                handleSpeechResults(partialResults, false);
            }

            @Override public void onEvent(int eventType, Bundle params) { }
        });
    }

    private void requestVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startVoiceInput();
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startVoiceInput() {
        if (speechRecognizer == null || speechIntent == null) {
            Toast.makeText(this, "Voice input is not available on this device", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isListening) {
            speechRecognizer.stopListening();
            setListening(false);
            return;
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            avatarView.setAvatarState(AiAvatarView.STATE_IDLE);
        }
        hideKeyboard();
        etMessage.setText("");
        speechRecognizer.startListening(speechIntent);
        setListening(true);
    }

    private void handleSpeechResults(Bundle results, boolean finalResult) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches == null || matches.isEmpty()) return;

        String spokenText = matches.get(0).trim();
        if (spokenText.isEmpty()) return;

        etMessage.setText(spokenText);
        etMessage.setSelection(etMessage.length());
        if (finalResult) {
            sendMessage(true);
        }
    }

    private void setListening(boolean listening) {
        isListening = listening;
        btnMic.setIconResource(listening ? R.drawable.ic_mic_off_24 : R.drawable.ic_mic_24);
        btnMic.setEnabled(!btnSend.isEnabled() || !listening || speechRecognizer != null);
        avatarView.setAvatarState(listening ? AiAvatarView.STATE_THINKING : AiAvatarView.STATE_IDLE);
    }

    private String speechErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO: return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT: return "Voice input cancelled";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Microphone permission missing";
            case SpeechRecognizer.ERROR_NETWORK: return "Voice network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Voice network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH: return "I did not catch that";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Voice recognizer is busy";
            case SpeechRecognizer.ERROR_SERVER: return "Voice service error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No speech detected";
            default: return "Voice input error";
        }
    }

    private void sendMessage(boolean speakReply) {
        String message = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (message.isEmpty()) {
            Toast.makeText(this, "Write or say a message first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            setListening(false);
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            avatarView.setAvatarState(AiAvatarView.STATE_IDLE);
        }

                shouldSpeakNextReply = speakReply;

        addUserMessage(message);
        etMessage.setText("");
        hideKeyboard();
        setLoading(true);

        dbExecutor.execute(() -> {
            Map<String, Object> body = buildChatRequestBody(message);
            handler.post(() -> sendChatRequest(body));
        });
    }

    private void sendChatRequest(Map<String, Object> body) {
        activeCall = apiService.chatWithAI(body);
        activeCall.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (call.isCanceled()) return;

                setLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    String errorText = readError(response);
                    Log.e(TAG, "AI chat failed. HTTP " + response.code() + ": " + errorText);
                    addAiMessage("AI service error (" + response.code() + "). Check backend logs for the real exception.");
                    return;
                }

                String reply = extractReply(response.body());
                if (reply.isEmpty()) {
                    reply = "I received the response, but could not read the AI message.";
                }
                addAiMessage(reply);
                if (shouldSpeakNextReply) {
                    speakAiReply(reply);
                } else {
                    avatarView.setAvatarState(AiAvatarView.STATE_IDLE);
                }
                shouldSpeakNextReply = false;
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (call.isCanceled()) return;

                setLoading(false);
                Log.e(TAG, "AI chat network failure", t);
                addAiMessage("Network error. Check your backend connection and try again.");
            }
        });
    }

    private Map<String, Object> buildChatRequestBody(String message) {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        long startOfDay = WeekUtils.getStartOfToday();
        long endOfDay = WeekUtils.getEndOfToday();

        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("prompt", message);
        body.put("question", message);
        body.put("daily_tasks", buildDailyTasks(db.taskDao().getTasksForDay(startOfDay, endOfDay)));
        body.put("education_tasks", buildEducationTasks(db.educationDao().getSessionsForDay(startOfDay, endOfDay)));
        body.put("gym_plans", buildGymPlans(db));
        body.put("sleep_settings", buildSleepSettings(db.sleepDao().getSleepSettings()));
        Log.d(TAG, "Chat context sent: daily=" + ((List<?>) body.get("daily_tasks")).size()
                + ", education=" + ((List<?>) body.get("education_tasks")).size()
                + ", gym=" + ((List<?>) body.get("gym_plans")).size()
                + ", sleep=" + (body.get("sleep_settings") != null));
        return body;
    }

    private List<Map<String, Object>> buildDailyTasks(List<DailyTask> tasks) {
        List<Map<String, Object>> result = new ArrayList<>();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        for (DailyTask task : tasks) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", task.id);
            item.put("title", task.title);
            item.put("description", task.description);
            item.put("taskTime", timeFormat.format(new Date(task.taskTime)));
            item.put("isDone", task.isDone);
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildEducationTasks(List<EducationTask> tasks) {
        List<Map<String, Object>> result = new ArrayList<>();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        for (EducationTask task : tasks) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", task.id);
            item.put("moduleName", task.moduleName);
            item.put("studyPlan", task.studyPlan);
            item.put("startTime", timeFormat.format(new Date(task.startTime)));
            item.put("endTime", timeFormat.format(new Date(task.endTime)));
            item.put("isFocusMode", task.isFocusMode);
            item.put("isDone", task.isDone);
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildGymPlans(AppDatabase db) {
        List<Map<String, Object>> result = new ArrayList<>();
        String today = WeekUtils.getTodayDayOfWeek();
        String weekStart = WeekUtils.getCurrentWeekStart();
        List<GymPlan> plans = db.gymDao().getPlansForDayAndWeek(today, weekStart);
        for (GymPlan plan : plans) {
            List<PlannedExercise> exercises = db.gymDao().getExercisesForPlan(plan.id);
            List<Map<String, Object>> exerciseMaps = new ArrayList<>();
            List<String> exerciseNames = new ArrayList<>();
            for (PlannedExercise exercise : exercises) {
                Map<String, Object> ex = new HashMap<>();
                ex.put("exerciseName", exercise.exerciseName);
                ex.put("setsTarget", exercise.setsTarget);
                ex.put("durationMinutes", exercise.durationMinutes);
                ex.put("isCardio", exercise.isCardio);
                exerciseMaps.add(ex);
                if (exercise.exerciseName != null && !exercise.exerciseName.trim().isEmpty()) {
                    exerciseNames.add(exercise.exerciseName);
                }
            }

            String bodyPart = plan.bodyPart != null ? plan.bodyPart : "";
            if (!exerciseNames.isEmpty()) {
                bodyPart += " | Exercises: " + join(exerciseNames);
            }

            Map<String, Object> item = new HashMap<>();
            item.put("id", plan.id);
            item.put("dayOfWeek", plan.dayOfWeek);
            item.put("weekStartDate", plan.weekStartDate);
            item.put("bodyPart", bodyPart);
            item.put("startTime", plan.startTime);
            item.put("exercises", exerciseMaps);
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> buildSleepSettings(SleepSettings settings) {
        if (settings == null) return null;
        Map<String, Object> item = new HashMap<>();
        item.put("bedTime", settings.sleepTime);
        item.put("sleepTime", settings.sleepTime);
        item.put("wakeTime", settings.wakeUpTime);
        item.put("wakeUpTime", settings.wakeUpTime);
        item.put("isAlarmOn", settings.isAlarmOn);
        item.put("isQRRequired", settings.isQRRequired);
        item.put("lastSleepDuration", settings.lastSleepDuration);
        return item;
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) builder.append(", ");
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private String extractReply(Map<String, Object> body) {
        String[] keys = {"response", "reply", "answer", "message"};
        for (String key : keys) {
            Object value = body.get(key);
            if (value != null) {
                String text = String.valueOf(value).trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return "";
    }

    private String readError(Response<?> response) {
        ResponseBody errorBody = response.errorBody();
        if (errorBody == null) {
            return "No error body";
        }
        try {
            String text = errorBody.string();
            if (text.length() > 600) {
                return text.substring(0, 600);
            }
            return text;
        } catch (Exception e) {
            return "Could not read error body: " + e.getMessage();
        }
    }

    private void setLoading(boolean loading) {
        btnSend.setEnabled(!loading);
        btnMic.setEnabled(!loading && speechRecognizer != null);
        btnSend.setText(loading ? "..." : "Send");
        if (!loading || !isListening) {
            avatarView.setAvatarState(loading ? AiAvatarView.STATE_THINKING : AiAvatarView.STATE_IDLE);
        }
    }

    private void speakAiReply(String reply) {
        if (!isTtsReady || textToSpeech == null) {
            speakBriefly();
            return;
        }
        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, TTS_UTTERANCE_ID);
        avatarView.setAvatarState(AiAvatarView.STATE_SPEAKING);
        textToSpeech.speak(reply, TextToSpeech.QUEUE_FLUSH, params, TTS_UTTERANCE_ID);
    }

    private void speakBriefly() {
        avatarView.setAvatarState(AiAvatarView.STATE_SPEAKING);
        handler.postDelayed(() -> avatarView.setAvatarState(AiAvatarView.STATE_IDLE), 1400L);
    }

    private void addUserMessage(String text) {
        addBubble(text, true);
    }

    private void addAiMessage(String text) {
        addBubble(text, false);
    }

    private void addBubble(String text, boolean isUser) {
        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setTextSize(15f);
        bubble.setLineSpacing(2f, 1.08f);
        bubble.setTextColor(Color.WHITE);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));
        bubble.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        bubble.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.78f));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(isUser ? Color.rgb(76, 175, 80) : Color.rgb(30, 30, 30));
        if (!isUser) {
            bg.setStroke(dp(1), Color.rgb(55, 55, 55));
        }
        bubble.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = isUser ? Gravity.END : Gravity.START;
        params.setMargins(0, dp(6), 0, dp(6));
        chatContainer.addView(bubble, params);

        scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View current = getCurrentFocus();
        if (imm != null && current != null) {
            imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
