package com.example.noexcuse;

import android.Manifest;
import android.app.AlertDialog;
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
import com.example.noexcuse.database.EducationTask;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudyHelperActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TTS_UTTERANCE_ID = "noexcuse_study_reply";

    private ProfessorAvatarView avatarView;
    private LinearLayout chatContainer;
    private ScrollView scrollChat;
    private TextInputEditText etMessage;
    private MaterialButton btnSend;
    private MaterialButton btnMic;
    private TextView tvStudyContext;
    private ApiService apiService;
    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private EducationTask educationTask;
    private boolean isTtsReady = false;
    private boolean isListening = false;
    private boolean shouldSpeakNextReply = true;
    private String currentConversationId = null;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<String> audioPermissionLauncher;
    private Call<Map<String, Object>> activeCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_helper);

        FrameLayout btnBack = findViewById(R.id.btnBackStudyHelper);
        FrameLayout btnHistory = findViewById(R.id.btnStudyHistory);
        avatarView = findViewById(R.id.professorAvatarView);
        chatContainer = findViewById(R.id.studyChatContainer);
        scrollChat = findViewById(R.id.scrollStudyChat);
        etMessage = findViewById(R.id.etStudyMessage);
        btnSend = findViewById(R.id.btnSendStudy);
        btnMic = findViewById(R.id.btnMicStudy);
        tvStudyContext = findViewById(R.id.tvStudyContext);
        apiService = RetrofitClient.getClient().create(ApiService.class);

        audioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        startVoiceInput();
                    } else {
                        Toast.makeText(this, "Microphone permission is required for voice study chat", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        btnBack.setOnClickListener(v -> finish());
        btnHistory.setOnClickListener(v -> showStudyHistory());
        btnSend.setOnClickListener(v -> sendMessage(false));
        btnMic.setOnClickListener(v -> requestVoiceInput());

        setupTextToSpeech();
        setupSpeechRecognizer();
        loadEducationTask();
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
        executor.shutdown();
        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        isTtsReady = status == TextToSpeech.SUCCESS;
        if (isTtsReady) {
            int result = textToSpeech.setLanguage(Locale.FRANCE);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech.setLanguage(Locale.FRENCH);
            }
            selectProfessorVoice();
            textToSpeech.setSpeechRate(0.9f);
            textToSpeech.setPitch(1.06f);
        }
    }

    private void selectProfessorVoice() {
        if (textToSpeech == null || textToSpeech.getVoices() == null) return;

        Voice bestVoice = null;
        int bestScore = Integer.MIN_VALUE;
        for (Voice voice : textToSpeech.getVoices()) {
            if (voice == null || voice.getLocale() == null) continue;

            Locale locale = voice.getLocale();
            String name = voice.getName() != null ? voice.getName().toLowerCase(Locale.US) : "";
            boolean preferredLanguage = Locale.FRENCH.getLanguage().equals(locale.getLanguage())
                    || Locale.ENGLISH.getLanguage().equals(locale.getLanguage());
            if (!preferredLanguage) continue;

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
            if (Locale.FRENCH.getLanguage().equals(locale.getLanguage())) score += 30;
            if ("FR".equalsIgnoreCase(locale.getCountry())) score += 20;
            if (soundsFemale) score += 35;
            if (voice.getQuality() >= Voice.QUALITY_HIGH) score += 18;
            if (voice.getQuality() >= Voice.QUALITY_VERY_HIGH) score += 24;
            if (!voice.isNetworkConnectionRequired()) score += 8;
            if (name.contains("enhanced") || name.contains("premium") || name.contains("neural")) score += 12;
            if (soundsMale && !soundsFemale) score -= 20;

            if (score > bestScore) {
                bestScore = score;
                bestVoice = voice;
            }
        }

        if (bestVoice != null) {
            textToSpeech.setVoice(bestVoice);
        }
    }

    private void loadEducationTask() {
        int eduId = getIntent().getIntExtra("EDU_ID", -1);
        if (eduId == -1) {
            finish();
            return;
        }

        executor.execute(() -> {
            EducationTask task = AppDatabase.getInstance(getApplicationContext()).educationDao().getById(eduId);
            handler.post(() -> {
                if (task == null) {
                    finish();
                    return;
                }
                educationTask = task;
                tvStudyContext.setText(task.moduleName + " - " + safeText(task.studyPlan, "No study plan"));
                addAiMessage("Bonjour. Je suis ton professeur IA pour cette session. Pose-moi seulement des questions sur " + task.moduleName + ".");
            });
        });
    }

    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(this, this);
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                handler.post(() -> avatarView.setAvatarState(ProfessorAvatarView.STATE_SPEAKING));
            }

            @Override
            public void onDone(String utteranceId) {
                handler.post(() -> avatarView.setAvatarState(ProfessorAvatarView.STATE_IDLE));
            }

            @Override
            public void onStop(String utteranceId, boolean interrupted) {
                handler.post(() -> avatarView.setAvatarState(ProfessorAvatarView.STATE_IDLE));
            }

            @Override
            public void onError(String utteranceId) {
                handler.post(() -> avatarView.setAvatarState(ProfessorAvatarView.STATE_IDLE));
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
        speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask your study professor");

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { setListening(true); }
            @Override public void onBeginningOfSpeech() { setListening(true); }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { setListening(false); }

            @Override
            public void onError(int error) {
                setListening(false);
                Toast.makeText(StudyHelperActivity.this, "Voice input error", Toast.LENGTH_SHORT).show();
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
            avatarView.setAvatarState(ProfessorAvatarView.STATE_IDLE);
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

    private void sendMessage(boolean speakReply) {
        String message = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (message.isEmpty()) {
            Toast.makeText(this, "Ask a study question first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (educationTask == null) {
            Toast.makeText(this, "Study session is still loading", Toast.LENGTH_SHORT).show();
            return;
        }

        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            setListening(false);
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            avatarView.setAvatarState(ProfessorAvatarView.STATE_IDLE);
        }

        shouldSpeakNextReply = speakReply;
        addUserMessage(message);
        etMessage.setText("");
        hideKeyboard();
        setLoading(true);

        Map<String, Object> body = buildStudyRequestBody(message);
        activeCall = apiService.studyHelper(body);
        activeCall.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (call.isCanceled()) return;

                setLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    addAiMessage("Study helper error (" + response.code() + "). Check backend logs.");
                    return;
                }

                String reply = extractReply(response.body());
                if (reply.isEmpty()) {
                    reply = "I could not read the study answer.";
                }
                Object conversationId = response.body().get("conversation_id");
                if (conversationId != null && !"null".equals(String.valueOf(conversationId))) {
                    currentConversationId = String.valueOf(conversationId);
                }
                addAiMessage(reply);
                if (shouldSpeakNextReply) {
                    speakReply(reply);
                } else {
                    avatarView.setAvatarState(ProfessorAvatarView.STATE_IDLE);
                }
                shouldSpeakNextReply = false;
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (call.isCanceled()) return;
                setLoading(false);
                addAiMessage("Network error. Check your backend connection and try again.");
            }
        });
    }

    private Map<String, Object> buildStudyRequestBody(String message) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Map<String, Object> education = new HashMap<>();
        education.put("id", educationTask.id);
        education.put("moduleName", educationTask.moduleName);
        education.put("studyPlan", educationTask.studyPlan);
        education.put("startTime", timeFormat.format(new Date(educationTask.startTime)));
        education.put("endTime", timeFormat.format(new Date(educationTask.endTime)));
        education.put("isFocusMode", educationTask.isFocusMode);
        education.put("isDone", educationTask.isDone);

        Map<String, Object> body = new HashMap<>();
        body.put("uid", getUserUid());
        if (currentConversationId != null && !currentConversationId.trim().isEmpty()) {
            body.put("conversation_id", currentConversationId);
        }
        body.put("message", message);
        body.put("education_task", education);
        return body;
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

    private void speakReply(String reply) {
        if (!isTtsReady || textToSpeech == null) {
            avatarView.setAvatarState(ProfessorAvatarView.STATE_SPEAKING);
            handler.postDelayed(() -> avatarView.setAvatarState(ProfessorAvatarView.STATE_IDLE), 1400L);
            return;
        }
        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, TTS_UTTERANCE_ID);
        avatarView.setAvatarState(ProfessorAvatarView.STATE_SPEAKING);
        textToSpeech.speak(reply, TextToSpeech.QUEUE_FLUSH, params, TTS_UTTERANCE_ID);
    }

    private void setListening(boolean listening) {
        isListening = listening;
        btnMic.setIconResource(listening ? R.drawable.ic_mic_off_24 : R.drawable.ic_mic_24);
        avatarView.setAvatarState(listening ? ProfessorAvatarView.STATE_THINKING : ProfessorAvatarView.STATE_IDLE);
    }

    private void setLoading(boolean loading) {
        btnSend.setEnabled(!loading);
        btnMic.setEnabled(!loading && speechRecognizer != null);
        btnSend.setText(loading ? "..." : "Send");
        if (!loading || !isListening) {
            avatarView.setAvatarState(loading ? ProfessorAvatarView.STATE_THINKING : ProfessorAvatarView.STATE_IDLE);
        }
    }

    private void showStudyHistory() {
        if (educationTask == null) {
            Toast.makeText(this, "Study session is still loading", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(4), dp(8), dp(4), 0);

        TextView newConversation = buildActionText("Nouvelle discussion");
        newConversation.setOnClickListener(v -> {
            currentConversationId = null;
            chatContainer.removeAllViews();
            addAiMessage("Bonjour. Je suis ton professeur IA pour cette session. Pose-moi seulement des questions sur " + educationTask.moduleName + ".");
            Toast.makeText(this, "Nouvelle discussion", Toast.LENGTH_SHORT).show();
        });
        content.addView(newConversation);

        ScrollView historyScroll = new ScrollView(this);
        LinearLayout historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        historyContainer.setPadding(0, dp(8), 0, dp(8));
        historyScroll.addView(historyContainer, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        content.addView(historyScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(430)
        ));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Discussions d'etude")
                .setView(content)
                .setPositiveButton("Fermer", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.rgb(33, 150, 243));

        addHistoryStatus(historyContainer, "Chargement des discussions...");
        loadStudyConversations(dialog, historyContainer);
    }

    private void loadStudyConversations(AlertDialog dialog, LinearLayout historyContainer) {
        apiService.getStudyConversations(getUserUid(), educationTask.id).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                historyContainer.removeAllViews();
                if (!response.isSuccessful() || response.body() == null) {
                    addHistoryStatus(historyContainer, "Impossible de charger les discussions.");
                    return;
                }

                Object raw = response.body().get("conversations");
                if (!(raw instanceof java.util.List) || ((java.util.List<?>) raw).isEmpty()) {
                    addHistoryStatus(historyContainer, "Aucune discussion d'etude pour ce module.");
                    return;
                }

                String currentDate = "";
                for (Object itemRaw : (java.util.List<?>) raw) {
                    if (!(itemRaw instanceof Map)) continue;
                    Map<?, ?> item = (Map<?, ?>) itemRaw;
                    String dateLabel = valueAsString(item.get("date_label"), "Sans date");
                    if (!dateLabel.equals(currentDate)) {
                        currentDate = dateLabel;
                        addDateHeader(historyContainer, dateLabel);
                    }
                    addStudyConversationRow(dialog, historyContainer, item);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                historyContainer.removeAllViews();
                addHistoryStatus(historyContainer, "Erreur reseau.");
            }
        });
    }

    private void addStudyConversationRow(AlertDialog dialog, LinearLayout historyContainer, Map<?, ?> item) {
        String conversationId = valueAsString(item.get("conversation_id"), "");
        if (conversationId.isEmpty()) return;

        String title = valueAsString(item.get("title"), "Discussion");
        String preview = valueAsString(item.get("preview"), "");
        String time = valueAsString(item.get("updated_time"), "");
        String count = valueAsString(item.get("message_count"), "0");

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(9), dp(8), dp(9));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(conversationId.equals(currentConversationId) ? Color.rgb(22, 55, 85) : Color.rgb(28, 28, 28));
        bg.setStroke(dp(1), Color.rgb(58, 58, 58));
        row.setBackground(bg);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(0, 0, dp(8), 0);

        TextView titleView = new TextView(this);
        titleView.setText(time.isEmpty() ? title : title + " - " + time);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(15f);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleView.setSingleLine(true);

        TextView previewView = new TextView(this);
        previewView.setText((preview.isEmpty() ? "Aucun apercu" : preview) + " (" + count + ")");
        previewView.setTextColor(Color.rgb(170, 170, 170));
        previewView.setTextSize(13f);
        previewView.setMaxLines(2);

        texts.addView(titleView);
        texts.addView(previewView);
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView delete = new TextView(this);
        delete.setText("Supprimer");
        delete.setTextColor(Color.rgb(239, 83, 80));
        delete.setTextSize(13f);
        delete.setPadding(dp(8), dp(8), dp(8), dp(8));
        row.addView(delete);

        row.setOnClickListener(v -> openStudyConversation(dialog, conversationId));
        delete.setOnClickListener(v -> confirmDeleteStudyConversation(dialog, historyContainer, conversationId));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(8));
        historyContainer.addView(row, params);
    }

    private void openStudyConversation(AlertDialog dialog, String conversationId) {
        apiService.getStudyHistory(getUserUid(), conversationId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(StudyHelperActivity.this, "Impossible d'ouvrir la discussion", Toast.LENGTH_SHORT).show();
                    return;
                }
                Object raw = response.body().get("messages");
                if (!(raw instanceof java.util.List)) return;

                currentConversationId = conversationId;
                chatContainer.removeAllViews();
                for (Object itemRaw : (java.util.List<?>) raw) {
                    if (!(itemRaw instanceof Map)) continue;
                    Map<?, ?> item = (Map<?, ?>) itemRaw;
                    String role = valueAsString(item.get("role"), "");
                    String text = valueAsString(item.get("content"), "");
                    if (!text.isEmpty()) {
                        addBubble(text, "user".equalsIgnoreCase(role));
                    }
                }
                if (dialog != null && dialog.isShowing()) dialog.dismiss();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(StudyHelperActivity.this, "Erreur reseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteStudyConversation(AlertDialog dialog, LinearLayout historyContainer, String conversationId) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer cette discussion ?")
                .setMessage("Cette discussion d'etude sera supprimee.")
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Supprimer", (d, w) -> deleteStudyConversation(dialog, historyContainer, conversationId))
                .show();
    }

    private void deleteStudyConversation(AlertDialog dialog, LinearLayout historyContainer, String conversationId) {
        apiService.deleteStudyConversation(getUserUid(), conversationId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (conversationId.equals(currentConversationId)) {
                    currentConversationId = null;
                    chatContainer.removeAllViews();
                    addAiMessage("Bonjour. Je suis ton professeur IA pour cette session. Pose-moi seulement des questions sur " + educationTask.moduleName + ".");
                }
                Toast.makeText(StudyHelperActivity.this, "Discussion supprimee", Toast.LENGTH_SHORT).show();
                historyContainer.removeAllViews();
                addHistoryStatus(historyContainer, "Chargement des discussions...");
                loadStudyConversations(dialog, historyContainer);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(StudyHelperActivity.this, "Impossible de supprimer", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private TextView buildActionText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(15f);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(Color.rgb(33, 150, 243));
        view.setBackground(bg);
        return view;
    }

    private void addDateHeader(LinearLayout container, String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextColor(Color.rgb(90, 90, 90));
        header.setTextSize(13f);
        header.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.setPadding(dp(4), dp(12), dp(4), dp(6));
        container.addView(header);
    }

    private void addHistoryStatus(LinearLayout container, String text) {
        TextView status = new TextView(this);
        status.setText(text);
        status.setTextColor(Color.rgb(80, 80, 80));
        status.setTextSize(15f);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(10), dp(18), dp(10), dp(18));
        container.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
    }

    private String valueAsString(Object value, String fallback) {
        if (value == null) return fallback;
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equals(text) ? fallback : text;
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
        bg.setColor(isUser ? Color.rgb(33, 150, 243) : Color.rgb(30, 30, 30));
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

    private String getUserUid() {
        String uid = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("uid", null);
        return uid != null && !uid.trim().isEmpty() ? uid : "local_user";
    }

    private String safeText(String text, String fallback) {
        return text != null && !text.trim().isEmpty() ? text : fallback;
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
