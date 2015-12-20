package com.example.tpalny.myapplication;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import java.util.Timer;
import java.util.TimerTask;


public class FullscreenSlideshow extends AppCompatActivity {


    private static final int MAX_NUM_OF_ATTEMPTS = 10;
    protected static ImageView mImageView1;
    protected static ImageView mImageView2;
    protected static ScrollTextView mText;
    private Timer timer;
    private TimerTask timerTask;
    protected static ViewFlipper mViewFlipper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_slideshow);
        Select_Folders.userCancelledSlideshow = false;
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
                //new SearchTask(FullscreenSlideshow.this, true, false).execute();
                cancelTimer(timer);
            }
        }

        if (Select_Folders.isSlideShowWithText) {
            new ReadTextFile().execute();
        }
        startTimer();

    }

    @Override
    public void onBackPressed() {
        Select_Folders.userCancelledSlideshow = true;
        cancelTimer(timer);

        super.onBackPressed();
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

                new DisplayImage(FullscreenSlideshow.this)
                        .execute();

            }

        };

    }

    public void onImageClicked(View view) {
        Select_Folders.userCancelledSlideshow = true;
        cancelTimer(timer);
        finish();
    }

    private void cancelTimer(Timer t) {
        //stop the timer, if it's not already null
        if (t != null) {
            t.cancel();

        }

    }

}
