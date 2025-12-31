package com.crocodilehughes.firstgame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {

    private ImageView carPreview;
    private ImageView carPreviewDecal;
    private int[] colors = { Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.WHITE };
    private int[] decalColors = { Color.WHITE, Color.BLACK, Color.YELLOW, Color.RED };
    private int currentColorIndex = 0;
    private int currentDecalColorIndex = 0;
    private boolean isDecalEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        TextView highScoreText = findViewById(R.id.high_score_text);
        carPreview = findViewById(R.id.car_preview_body);
        carPreviewDecal = findViewById(R.id.car_preview_decal);
        Button changeColorButton = findViewById(R.id.change_color_button);
        Button toggleDecalButton = findViewById(R.id.toggle_decal_button);
        Button changeDecalColorButton = findViewById(R.id.change_decal_color_button);
        Button startButton = findViewById(R.id.start_button);

        // Load High Score
        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        long highScore = prefs.getLong("HIGH_SCORE", 0);
        highScoreText.setText("High Score: " + highScore);

        // Initial Color
        updateCarColor();
        updateDecalColor();

        changeColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentColorIndex = (currentColorIndex + 1) % colors.length;
                updateCarColor();
            }
        });

        changeDecalColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDecalColorIndex = (currentDecalColorIndex + 1) % decalColors.length;
                updateDecalColor();
            }
        });

        toggleDecalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDecalEnabled = !isDecalEnabled;
                carPreviewDecal.setVisibility(isDecalEnabled ? View.VISIBLE : View.GONE);
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuActivity.this, MainActivity.class);
                intent.putExtra("CAR_COLOR", colors[currentColorIndex]);
                intent.putExtra("IS_DECAL_ENABLED", isDecalEnabled);
                intent.putExtra("DECAL_COLOR", decalColors[currentDecalColorIndex]);
                startActivity(intent);
            }
        });
    }

    private void updateCarColor() {
        carPreview.setColorFilter(colors[currentColorIndex]);
    }

    private void updateDecalColor() {
        carPreviewDecal.setColorFilter(decalColors[currentDecalColorIndex]);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload High Score in case we just came back from a game
        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        long highScore = prefs.getLong("HIGH_SCORE", 0);
        TextView highScoreText = findViewById(R.id.high_score_text);
        highScoreText.setText("High Score: " + highScore);
    }
}
