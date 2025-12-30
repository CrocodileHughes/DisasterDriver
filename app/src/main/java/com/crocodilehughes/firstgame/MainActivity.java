package com.crocodilehughes.firstgame;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ImageView playerCar;
    private View road;
    private View line1, line2, line3, startLine;
    private View bumperLeft, bumperRight;
    private ImageView cone1, cone2, cone3;
    private ImageView[] cones;
    private boolean isTurningLeft = false;
    private boolean isTurningRight = false;
    private boolean raceStarted = false;
    private boolean gameOver = false;
    private TextView countdownText;
    private ValueAnimator animator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerCar = findViewById(R.id.player_car);
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
        countdownText = findViewById(R.id.countdown_text);

        cone1 = findViewById(R.id.cone1);
        cone2 = findViewById(R.id.cone2);
        cone3 = findViewById(R.id.cone3);
        cones = new ImageView[]{cone1, cone2, cone3};

        ImageButton leftButton = findViewById(R.id.left_button);
        leftButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!raceStarted || gameOver) return false;
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
                if (!raceStarted || gameOver) return false;
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

        startCountdown();
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
                startRoadAnimation();
            }
        }.start();
    }

    private void startRoadAnimation() {
        final int screenHeight = getResources().getDisplayMetrics().heightPixels;
        final int screenWidth = getResources().getDisplayMetrics().widthPixels;

        animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(12000L);

        final View[] repeatingLines = {line1, line2, line3};

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private float lastRoadOffset = 0f;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (!raceStarted || gameOver) return;

                final float roadMoveSpeed = 15.0f;
                final float carDriftSpeed = 5.0f;

                float animationProgress = animation.getAnimatedFraction();
                float mainTurnFactor = (float) Math.sin(animationProgress * 2 * Math.PI);
                float wiggleFactor = (float) Math.sin(animationProgress * 96 * Math.PI);
                float roadTurnOffset = (mainTurnFactor * (screenWidth / 4f)) + (wiggleFactor * (screenWidth / 30f));

                float roadDeltaX = roadTurnOffset - lastRoadOffset;
                lastRoadOffset = roadTurnOffset;

                road.setTranslationX(roadTurnOffset);
                bumperLeft.setTranslationX(roadTurnOffset);
                bumperRight.setTranslationX(roadTurnOffset);
                startLine.setTranslationX(roadTurnOffset);
                for (View item : repeatingLines) item.setTranslationX(roadTurnOffset);
                for (View item : cones) item.setTranslationX(roadTurnOffset);

                if (isTurningLeft) {
                    playerCar.setRotation(playerCar.getRotation() - 2);
                }
                if (isTurningRight) {
                    playerCar.setRotation(playerCar.getRotation() + 2);
                }

                float newCarX = playerCar.getTranslationX() + roadDeltaX;
                double angleInRadians = Math.toRadians(playerCar.getRotation());
                float carDrift = (float) (Math.sin(angleInRadians) * carDriftSpeed);
                newCarX += carDrift;

                if (checkCollision(newCarX)) {
                    endGame();
                    return;
                }

                playerCar.setTranslationX(newCarX);

                float rotation = playerCar.getRotation() % 360;
                if (rotation < 0) rotation += 360;
                boolean isUpsideDown = rotation > 90 && rotation < 270;
                float verticalMove = isUpsideDown ? -roadMoveSpeed : roadMoveSpeed;

                float lineMargin = getResources().getDisplayMetrics().density * 80;
                float totalPatternHeight = repeatingLines.length * (line1.getHeight() + lineMargin);

                for (View line : repeatingLines) {
                    line.setTranslationY(line.getTranslationY() + verticalMove);
                    if (verticalMove > 0 && line.getTop() + line.getTranslationY() > screenHeight) {
                        line.setTranslationY(line.getTranslationY() - totalPatternHeight - 200);
                    } else if (verticalMove < 0 && line.getBottom() + line.getTranslationY() < 0) {
                        line.setTranslationY(line.getTranslationY() + totalPatternHeight + 200);
                    }
                }
                startLine.setTranslationY(startLine.getTranslationY() + verticalMove);

                for (ImageView cone : cones) {
                    cone.setTranslationY(cone.getTranslationY() + verticalMove);
                    if (verticalMove > 0 && cone.getTop() + cone.getTranslationY() > screenHeight) {
                        cone.setTranslationY(cone.getTranslationY() - (screenHeight + 500));
                    }
                }
            }
        });
        animator.start();
    }

    private boolean checkCollision(float newCarX) {
        float carHalfWidth = (playerCar.getWidth() * playerCar.getScaleX()) / 2;
        float carCenterInParent = getResources().getDisplayMetrics().widthPixels / 2f;
        float newCarVisualCenter = carCenterInParent + newCarX;
        float newCarLeft = newCarVisualCenter - carHalfWidth;
        float newCarRight = newCarVisualCenter + carHalfWidth;
        float carTop = playerCar.getY();
        float carBottom = carTop + playerCar.getHeight();

        float roadLeftEdge = road.getX();
        float roadRightEdge = road.getX() + road.getWidth();

        if (newCarLeft <= roadLeftEdge || newCarRight >= roadRightEdge) {
            return true;
        }

        for (ImageView cone : cones) {
            float coneLeft = cone.getX() + cone.getTranslationX();
            float coneRight = coneLeft + cone.getWidth();
            float coneTop = cone.getY() + cone.getTranslationY();
            float coneBottom = coneTop + cone.getHeight();

            if (newCarLeft < coneRight && newCarRight > coneLeft && carTop < coneBottom && carBottom > coneTop) {
                return true;
            }
        }

        return false;
    }

    private void endGame() {
        if (gameOver) return;
        gameOver = true;
        animator.cancel();
        countdownText.setText("Game Over");
        countdownText.setVisibility(View.VISIBLE);
    }
}
