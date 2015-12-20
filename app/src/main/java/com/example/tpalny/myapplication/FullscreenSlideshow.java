package com.example.tpalny.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;


public class FullscreenSlideshow extends AppCompatActivity {


    private static final int MAX_NUM_OF_ATTEMPTS = 10;
    protected static ImageView mImageView1;
    protected static ImageView mImageView2;
    protected static TextView mText;
    private Timer timer;
    private TimerTask timerTask;
    private final Handler handler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_slideshow);
        Select_Folders.userCancelledSlideshow = false;
        mImageView1 = (ImageView) findViewById(R.id.my_image1);
        mImageView2 = (ImageView) findViewById(R.id.my_image2);
        mText = (TextView) findViewById(R.id.my_text);

    }


    @Override
    protected void onResume() {
        super.onResume();
        int x = 0;
        while (Select_Folders.imagesList == null) {
            x++;
            if (x >= MAX_NUM_OF_ATTEMPTS) {
                x = 0;
                //new SearchTask(FullscreenSlideshow.this, true, false).execute();
                cancelTimerAndReturn();
            }
        }

        //startTimer();
        new ReadTextFile().execute();


    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Select_Folders.userCancelledSlideshow = true;
        cancelTimerAndReturn();
    }

    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        Integer delay = Integer.parseInt(Select_Folders.slideShowDelay.getText().toString());
        timer.schedule(timerTask, 0, delay * 1000);
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {

                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        new DisplayImage(FullscreenSlideshow.this).execute();

                    }
                });
            }

        };

    }

    public void onImageClicked(View view) {
        Select_Folders.userCancelledSlideshow = true;
        cancelTimerAndReturn();
    }

    private void cancelTimerAndReturn() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        finish();
    }
}
