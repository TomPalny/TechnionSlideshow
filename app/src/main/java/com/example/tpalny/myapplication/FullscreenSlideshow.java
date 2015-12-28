package com.example.tpalny.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.util.Timer;
import java.util.TimerTask;


public class FullscreenSlideshow extends AppCompatActivity {


    private static final int MAX_NUM_OF_ATTEMPTS = 10;
    protected static ImageView mImageView1;
    protected static ImageView mImageView2;
    protected static ScrollTextView mText;
    private Timer picturesChangeTimer;
    private TimerTask timerTask;
    protected static ViewFlipper mViewFlipper;
    private Timer textUpdateTimer;
    private SharedPreferences settings;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_slideshow);
        settings = getSharedPreferences("com.example.tpalny.myapplication_preferences", Context.MODE_PRIVATE);
        mImageView1 = (ImageView) findViewById(R.id.my_image1);
        mImageView2 = (ImageView) findViewById(R.id.my_image2);
        mText = (ScrollTextView) findViewById(R.id.my_text);
        mViewFlipper = (ViewFlipper) findViewById(R.id.flipper);
        mViewFlipper.setInAnimation(this, R.anim.slide_in_from_right);
        mViewFlipper.setOutAnimation(this, R.anim.slide_out_to_left);

    }


    @Override
    protected void onResume() {
        super.onResume();
        int x = 0;
        while (Select_Folders.imagesList == null) {
            x++;
            if (x >= MAX_NUM_OF_ATTEMPTS) {
                x = 0;

                cancelTimer(picturesChangeTimer);
                cancelTimer(textUpdateTimer);
                Toast.makeText(this, "Network is slow, images didn't load", Toast.LENGTH_LONG).show();
                finish();
            }
        }

        if (Select_Folders.isSlideShowWithText) {
            new ReadTextFile().execute();
            TimerTask textUpdateTask = new TimerTask() {
                @Override
                public void run() {
                    new SearchTask(FullscreenSlideshow.this, false, true).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
                    new ReadTextFile().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
                }
            };
            textUpdateTimer = new Timer();
            Integer rate = Integer.parseInt(Select_Folders.textFileRefreshRate.getText().toString());
            textUpdateTimer.schedule(textUpdateTask, rate * 1000, rate * 1000);
        }
        startTimer();

    }

    @Override
    public void onBackPressed() {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(Select_Folders.USER_CANCELLED_SLIDESHOW, true).apply();
        cancelTimer(picturesChangeTimer);
        cancelTimer(textUpdateTimer);

        super.onBackPressed();
    }

    public void startTimer() {
        //set a new Timer
        picturesChangeTimer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        Integer delay = Integer.parseInt(Select_Folders.slideShowDelay.getText().toString());
        picturesChangeTimer.schedule(timerTask, 0, delay * 1000);
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {

                new DisplayImage(FullscreenSlideshow.this)
                        .execute();

            }

        };

    }

    public void onImageClicked(View view) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(Select_Folders.USER_CANCELLED_SLIDESHOW, true).apply();
        cancelTimer(picturesChangeTimer);
        cancelTimer(textUpdateTimer);
        finish();
    }

    private void cancelTimer(Timer t) {
        //stop the timer, if it's not already null
        if (t != null) {
            t.cancel();

        }

    }

}
