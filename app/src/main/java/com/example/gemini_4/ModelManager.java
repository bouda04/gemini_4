package com.example.gemini_4;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.Html;

import androidx.annotation.NonNull;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.ai.client.generativeai.type.ImagePart;
import com.google.ai.client.generativeai.type.Part;
import com.google.ai.client.generativeai.type.RequestOptions;
import com.google.ai.client.generativeai.type.TextPart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlin.Result;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class ModelManager {

    private static final String TAG = "ModelManager";
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    private static volatile ModelManager INSTANCE;
    protected final GenerativeModel modelReference;



    protected ModelManager(Context context, int systemPromptId) {
        final String SYSTEM_PROMPT = String.valueOf(
                Html.fromHtml(context.getString(systemPromptId),
                Html.FROM_HTML_MODE_COMPACT));

        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();

        // Set the temperature using the setter method
        configBuilder.temperature= 0.0f; //no randomness!!!


        List<Part> parts = new ArrayList<Part>();
        parts.add(new TextPart(SYSTEM_PROMPT));
        modelReference = new GenerativeModel(
                "gemini-2.0-flash",
                API_KEY,
                /* generation config */configBuilder.build(),
                /* safety setting */ null,
                /* request options */ new RequestOptions(),
                /* tools */ null,
                /* tool config */ null,
                /* system prompt */ new Content(parts)
        );
    }


    public static ModelManager getInstance(Context context, int systemPromptId) {
        if (INSTANCE == null) {
            synchronized (ModelManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ModelManager(context.getApplicationContext(), systemPromptId);
                }
            }
        }
        return INSTANCE;
    }

    public void sendMessage(String input, Bitmap bitmap, CallBacks callback) {
        List<Part> parts = new ArrayList<>();
        parts.add(new TextPart(input));
        if (bitmap != null)
            parts.add(new ImagePart(bitmap));

        Content message = new Content("user", parts);


        modelReference.generateContent(new Content[]{message},
                new Continuation<GenerateContentResponse>() {
                    @NonNull
                    @Override
                    public CoroutineContext getContext() {
                        return EmptyCoroutineContext.INSTANCE;
                    }

                    @Override
                    public void resumeWith(@NonNull Object result) {
                        if (result instanceof Result.Failure) {
                            callback.onModelError(((Result.Failure) result).exception);
                        } else {
                            callback.onModelSuccess(((GenerateContentResponse) result).getText());
                        }
                    }
        });
    }


    public interface CallBacks{
        void onModelSuccess(String response);
        void onModelError(Throwable error);
    }
}


