package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import java.util.ArrayList;
import java.util.Locale;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity {

    private static final int VOICE_REQUEST_CODE = 1001;

    private EditText promptEditText;
    private TextView responseTextView;
    private ProgressBar progressBar;
    private ImageButton voiceInputButton;
    private ToggleButton themeToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        promptEditText = findViewById(R.id.promptEditText);
        ImageButton submitPromptButton = findViewById(R.id.sendButton);
        responseTextView = findViewById(R.id.displayTextView);
        progressBar = findViewById(R.id.progressBar);
        voiceInputButton = findViewById(R.id.voiceInputButton);
        themeToggle = findViewById(R.id.themeToggle);

        voiceInputButton.setOnClickListener(v -> startVoiceRecognition());

        themeToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        GenerativeModel generativeModel = new GenerativeModel(
                "gemini-2.0-flash",
                BuildConfig.GEMINI_API_KEY
        );

        submitPromptButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString().trim();
            promptEditText.setError(null);

            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                String string = getString(R.string.aistring);
                responseTextView.setText(TextFormatter.getBoldSpannableText(string));
                return;
            }

            progressBar.setVisibility(VISIBLE);

            generativeModel.generateContent(prompt, new Continuation<>() {
                @NonNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object o) {
                    runOnUiThread(() -> progressBar.setVisibility(GONE));

                    if (o instanceof Throwable) {
                        Throwable error = (Throwable) o;
                        Log.e("Error", "API request failed", error);
                        runOnUiThread(() ->
                                responseTextView.setText("Request failed: " + error.getMessage())
                        );
                    } else {
                        try {
                            GenerateContentResponse response = (GenerateContentResponse) o;
                            String responseString = response.getText();
                            assert responseString != null;
                            Log.d("Response", responseString);
                            runOnUiThread(() ->
                                    responseTextView.setText(TextFormatter.getBoldSpannableText(responseString))
                            );
                        } catch (Exception e) {
                            Log.e("Error", "Exception occurred", e);
                            runOnUiThread(() ->
                                    responseTextView.setText("Error processing response: " + e.getMessage())
                            );
                        }
                    }
                }
            });
        });
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your prompt...");
        try {
            startActivityForResult(intent, VOICE_REQUEST_CODE);
        } catch (Exception e) {
            promptEditText.setError("Voice input not supported on this device");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                promptEditText.setText(results.get(0));
            }
        }
    }
}
