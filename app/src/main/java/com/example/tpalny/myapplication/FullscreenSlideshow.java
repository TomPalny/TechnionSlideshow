package com.example.tpalny.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.util.Timer;
import java.util.TimerTask;


public class FullscreenSlideshow extends AppCompatActivity {


    private static final int MAX_NUM_OF_ATTEMPTS = 10;
    protected static ScrollTextView mText;
    protected static ViewFlipper mViewFlipper;
    private Timer textUpdateTimer;
    private SharedPreferences settings;
    protected static int heightPixels;
    protected static int widthPixels;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_slideshow);
        settings = getSharedPreferences("com.example.tpalny.myapplication_preferences", Context.MODE_PRIVATE);
        mText = (ScrollTextView) findViewById(R.id.my_text);
        mViewFlipper = (ViewFlipper) findViewById(R.id.flipper);
        mViewFlipper.setInAnimation(this, R.anim.slide_in_from_right);
        mViewFlipper.setOutAnimation(this, R.anim.slide_out_to_left);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        heightPixels = metrics.heightPixels;
        widthPixels = metrics.widthPixels;

    }


    @Override
    protected void onResume() {
        super.onResume();
        int x = 0;
        while (Select_Folders.imagesList == null) {
            x++;
            if (x > MAX_NUM_OF_ATTEMPTS) {
                x = 0;
                mViewFlipper.stopFlipping();
                cancelTimer(textUpdateTimer);
                Toast.makeText(this, "Network is slow, images didn't load", Toast.LENGTH_SHORT).show();
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
        mViewFlipper.stopFlipping();
        cancelTimer(textUpdateTimer);

        super.onBackPressed();
    }

    public void startTimer() {

        Integer delay = Integer.parseInt(Select_Folders.slideShowDelay.getText().toString());
        mViewFlipper.setFlipInterval(delay * 1000);
        int counter = 0;
        while (counter++ < Select_Folders.imagesList.size()) {
            new DisplayImage(FullscreenSlideshow.this)
                    .executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }
        mViewFlipper.startFlipping();

    }


    public void onImageClicked(View view) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(Select_Folders.USER_CANCELLED_SLIDESHOW, true).apply();
        mViewFlipper.stopFlipping();
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
