package com.example.gemini_4;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.graphics.Color;
import android.widget.ProgressBar;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    ImageView imgBefore;
    ImageView imgAfter;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        imgBefore = findViewById(R.id.imgBefore);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btGetImage).setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, 100); // 100 is requestCode
        });
        findViewById(R.id.btAskModel).setOnClickListener(view -> {
            showInputDialog();
        });
        imgAfter = findViewById(R.id.imgAfter);

    }


    public void showInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter a word");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            String object = input.getText().toString();
            Bitmap bitmap = ((BitmapDrawable)imgBefore.getDrawable()).getBitmap();
            Bitmap bitmap2 = blackOutWholeImage(bitmap);
            imgAfter.setImageBitmap(bitmap2);
            searchForObject(bitmap, object);
           // Log.d("DialogInput", "User entered: " + userInput);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }


    private void searchForObject(final Bitmap bitmap, String object) {
        progressBar.setVisibility(View.VISIBLE);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        ModelManager modelManager = ModelManager.getInstance(this, R.string.sys_prompt);
        modelManager.sendMessage(getString(R.string.search_object, object, width, height), bitmap,
                new ModelManager.CallBacks() {
                    @Override
                    public void onModelSuccess(String response) {
                        Log.i("monitor response", response);
                        int p0 = response.indexOf('{');
                        int p1 = response.lastIndexOf('}');
                        JSONObject obj = null;
                        int x,y, radius;
                        try {
                            obj = new JSONObject(response.substring(p0, p1 + 1));
                            x = obj.getInt("x");
                            y = obj.getInt("y");
                            radius = obj.getInt("radius");
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        runOnUiThread(() -> {
                            if (x==-1 || y==-1 || radius==-1)
                                Toast.makeText(MainActivity.this,"לא מצאתי!!!",Toast.LENGTH_SHORT).show();
                            else {
                                Bitmap bitmap2 = highlightArea(bitmap, x, y, 1.5f * radius);
                                imgAfter.setImageBitmap(bitmap2);
                            }
                            progressBar.setVisibility(View.GONE);
                        });
                    }

                    @Override
                    public void onModelError(Throwable error) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,"שגיאה מול המודל AI: ",Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        });
                    }
                });
    }

    private static Bitmap createBlackedOutOverlay(int width, int height) {
        Bitmap overlay = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas overlayCanvas = new Canvas(overlay);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#AA000000")); // semi-transparent black
        overlayCanvas.drawRect(0, 0, width, height, paint);
        return overlay;
    }

    public static Bitmap blackOutWholeImage(Bitmap original) {
        Bitmap result = original.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap overlay = createBlackedOutOverlay(result.getWidth(), result.getHeight());
        new Canvas(result).drawBitmap(overlay, 0, 0, null);
        return result;
    }

    public static Bitmap highlightArea(Bitmap original, float x, float y, float radius) {
        Bitmap result = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);

        Bitmap overlay = createBlackedOutOverlay(result.getWidth(), result.getHeight());
        Canvas overlayCanvas = new Canvas(overlay);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        overlayCanvas.drawCircle(x, y, radius, paint);

        canvas.drawBitmap(overlay, 0, 0, null);

        Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
        outline.setStyle(Paint.Style.STROKE);
        outline.setColor(Color.YELLOW);
        outline.setStrokeWidth(4);
        canvas.drawCircle(x, y, radius, outline);

        return result;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                // Use the bitmap here (e.g., set it in an ImageView)
                imgBefore.setImageBitmap(bitmap);
                Bitmap bitmap2 = blackOutWholeImage(bitmap);
                imgAfter.setImageBitmap(bitmap2);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}