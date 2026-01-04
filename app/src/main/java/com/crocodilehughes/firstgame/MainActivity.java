package com.crocodilehughes.firstgame;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private View playerCar;
    private ImageView playerCarBody;
    private View road;
    private View line1, line2, line3, startLine;
    private View bumperLeft, bumperRight;

    private boolean isTurningLeft = false;
    private boolean isTurningRight = false;
    private boolean raceStarted = false;
    private boolean gameOver = false;
    private TextView countdownText;
    private TextView scoreText;
    private Button playAgainButton;
    private Button menuButton;
    private ValueAnimator animator;
    private long raceStartTime;
    private long currentScore = 0;
    private int carColor = Color.RED;
    private int decalColor = Color.WHITE;
    private boolean isDecalEnabled = false;
    private EngineSoundSynthesizer synthesizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        carColor = getIntent().getIntExtra("CAR_COLOR", Color.RED);
        isDecalEnabled = getIntent().getBooleanExtra("IS_DECAL_ENABLED", false);
        decalColor = getIntent().getIntExtra("DECAL_COLOR", Color.WHITE);
        playerCar = findViewById(R.id.player_car);
        ImageView playerCarDecal = findViewById(R.id.player_car_decal);
        playerCarDecal.setVisibility(isDecalEnabled ? View.VISIBLE : View.GONE);
        playerCarDecal.setColorFilter(decalColor);
        playerCarBody = findViewById(R.id.player_car_body);
        playerCarBody.setColorFilter(carColor);
        playerCar.setScaleX(8.0f);
        playerCar.setScaleY(8.0f);
        playerCar.setTranslationY(-100.0f);
        playerCar.setTranslationX(0f); // This guarantees the car starts centered.

        road = findViewById(R.id.road);
        line1 = findViewById(R.id.line1);
        line2 = findViewById(R.id.line2);
        line3 = findViewById(R.id.line3);
        startLine = findViewById(R.id.start_line);
        bumperLeft = findViewById(R.id.bumper_left);
        bumperRight = findViewById(R.id.bumper_right);
        scoreText = findViewById(R.id.score_text);
        countdownText = findViewById(R.id.countdown_text);
        playAgainButton = findViewById(R.id.play_again_button);
        menuButton = findViewById(R.id.menu_button);

        playAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        });

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Returns to MenuActivity
            }
        });

        ImageButton leftButton = findViewById(R.id.left_button);
        leftButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!raceStarted || gameOver)
                    return false;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isTurningLeft = true;
                        return true;
                    case MotionEvent.ACTION_UP:
                        isTurningLeft = false;
                        return true;
                }
                return false;
            }
        });

        ImageButton rightButton = findViewById(R.id.right_button);
        rightButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!raceStarted || gameOver)
                    return false;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isTurningRight = true;
                        return true;
                    case MotionEvent.ACTION_UP:
                        isTurningRight = false;
                        return true;
                }
                return false;
            }
        });

        synthesizer = new EngineSoundSynthesizer();
        startCountdown();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (synthesizer != null) {
            synthesizer.stop();
        }
    }

    private void startCountdown() {
        new CountDownTimer(4000, 1000) {
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                if (seconds > 0) {
                    countdownText.setText(String.valueOf(seconds));
                } else {
                    countdownText.setText("Go!");
                }
            }

            public void onFinish() {
                countdownText.setVisibility(View.GONE);
                raceStarted = true;
                raceStartTime = System.currentTimeMillis();
                synthesizer.start();
                startRoadAnimation();
            }
        }.start();
    }

    private float totalTime = 0f;

    private float getPseudoRandom(int x) {
        x = (x << 13) ^ x;
        return (float) (1.0 - ((x * (x * x * 15731 + 789221) + 1376312589) & 0x7fffffff) / 1073741824.0);
    }

    private float cosineInterpolate(float a, float b, float x) {
        float ft = x * 3.1415927f;
        float f = (1 - (float) Math.cos(ft)) * 0.5f;
        return a * (1 - f) + b * f;
    }

    private float getNoise(float x) {
        int intX = (int) Math.floor(x);
        float fracX = x - intX;

        float v1 = getPseudoRandom(intX);
        float v2 = getPseudoRandom(intX + 1);

        return cosineInterpolate(v1, v2, fracX);
    }

    private void startRoadAnimation() {
        final int screenHeight = getResources().getDisplayMetrics().heightPixels;
        final int screenWidth = getResources().getDisplayMetrics().widthPixels;

        animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(12000L); // Duration controls the update tick rate essentially

        final View[] repeatingLines = { line1, line2, line3 };

        // Reset time when starting
        totalTime = 0;

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private float lastRoadOffset = 0f;
            private long lastUpdate = 0;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (!raceStarted || gameOver) {
                    lastUpdate = 0;
                    return;
                }

                long currentTime = System.currentTimeMillis();
                if (lastUpdate == 0) {
                    lastUpdate = currentTime;
                }
                float dt = (currentTime - lastUpdate) / 1000f;
                lastUpdate = currentTime;
                totalTime += dt;

                final float roadMoveSpeed = 15.0f; // This is the vertical speed
                final float carDriftSpeed = 5.0f;

                // Engine Sound Logic
                if (isTurningLeft || isTurningRight) {
                    synthesizer.setFrequency(180.0); // Lower pitch on turn
                } else {
                    synthesizer.setFrequency(220.0); // Base pitch
                }

                // Base Noise: Large, slow turns
                float noise1 = getNoise(totalTime * 0.2f);
                // Detail Noise: Faster, smaller wiggles
                float noise2 = getNoise(totalTime * 6.0f);

                float mainTurnFactor = noise1;
                float wiggleFactor = noise2;

                // Combine them.
                // noise outputs -1 to 1 approximately (based on the hash func).
                // mainTurnFactor drives the big curves (screen width / 3.5)
                // wiggleFactor drives the jitter (screen width / 25)
                float roadTurnOffset = (mainTurnFactor * (screenWidth / 3.5f)) + (wiggleFactor * (screenWidth / 25f));

                float roadDeltaX = roadTurnOffset - lastRoadOffset;
                lastRoadOffset = roadTurnOffset;

                // Road Schrinking Logic
                float scaleSpeed = 0.00005f;
                float additionalScale = 0.0001f * (currentScore / 1000f);
                float currentScaleX = road.getScaleX();
                if (currentScaleX > 0.3f) {
                    road.setScaleX(currentScaleX - scaleSpeed); // removed additionalScale for stability if needed, can
                                                                // add back
                }

                float shrinkOffset = (road.getWidth() * (1 - road.getScaleX())) / 2;

                road.setTranslationX(roadTurnOffset);
                bumperLeft.setTranslationX(roadTurnOffset + shrinkOffset);
                bumperRight.setTranslationX(roadTurnOffset - shrinkOffset);
                startLine.setTranslationX(roadTurnOffset);
                for (View item : repeatingLines)
                    item.setTranslationX(roadTurnOffset);

                if (isTurningLeft) {
                    playerCar.setRotation(playerCar.getRotation() - 2);
                }
                if (isTurningRight) {
                    playerCar.setRotation(playerCar.getRotation() + 2);
                }

                float newCarX = playerCar.getTranslationX();
                double angleInRadians = Math.toRadians(playerCar.getRotation());
                float carDrift = (float) (Math.sin(angleInRadians) * carDriftSpeed);
                newCarX += carDrift;

                if (checkCollision(newCarX, shrinkOffset)) {
                    endGame();
                    return;
                }

                playerCar.setTranslationX(newCarX);

                float rotation = playerCar.getRotation() % 360;
                if (rotation < 0)
                    rotation += 360;
                boolean isUpsideDown = rotation > 90 && rotation < 270;
                float verticalMove = isUpsideDown ? -roadMoveSpeed : roadMoveSpeed;

                float lineMargin = getResources().getDisplayMetrics().density * 80;
                float totalPatternHeight = repeatingLines.length * (line1.getHeight() + lineMargin);

                for (View line : repeatingLines) {
                    line.setTranslationY(line.getTranslationY() + verticalMove);
                    if (verticalMove > 0 && line.getTop() + line.getTranslationY() > screenHeight) {
                        line.setTranslationY(line.getTranslationY() - totalPatternHeight);
                    } else if (verticalMove < 0 && line.getBottom() + line.getTranslationY() < 0) {
                        line.setTranslationY(line.getTranslationY() + totalPatternHeight);
                    }
                }
                startLine.setTranslationY(startLine.getTranslationY() + verticalMove);

                currentScore = System.currentTimeMillis() - raceStartTime;
                scoreText.setText("Score: " + currentScore);
            }
        });
        animator.start();
    }

    private boolean checkCollision(float newCarX, float shrinkOffset) {
        float carHalfWidth = (playerCar.getWidth() * playerCar.getScaleX()) / 2;
        float carCenterInParent = getResources().getDisplayMetrics().widthPixels / 2f;
        float newCarVisualCenter = carCenterInParent + newCarX;
        float newCarLeft = newCarVisualCenter - carHalfWidth;
        float newCarRight = newCarVisualCenter + carHalfWidth;
        float carTop = playerCar.getY();
        float carBottom = carTop + playerCar.getHeight();

        float roadLeftEdge = road.getX() + shrinkOffset;
        float roadRightEdge = road.getX() + road.getWidth() - shrinkOffset;

        if (newCarLeft <= roadLeftEdge || newCarRight >= roadRightEdge) {
            return true;
        }

        return false;
    }

    private void endGame() {
        if (gameOver)
            return;
        gameOver = true;
        animator.cancel();

        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        long highScore = prefs.getLong("HIGH_SCORE", 0);
        if (currentScore > highScore) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("HIGH_SCORE", currentScore);
            editor.apply();
            highScore = currentScore;
        }

        countdownText.setText("Game Over\nScore: " + currentScore + "\nHigh Score: " + highScore);
        countdownText.setTextSize(30f);
        countdownText.setVisibility(View.VISIBLE);
        playAgainButton.setVisibility(View.VISIBLE);
        menuButton.setVisibility(View.VISIBLE);
        if (synthesizer != null) {
            synthesizer.playSadMelody();
        }
    }
}
