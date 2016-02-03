package com.example.tpalny.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class FullscreenSlideshow extends AppCompatActivity {


    private static final int MAX_NUM_OF_ATTEMPTS = 10;
    protected static ScrollTextView mText;
    protected static ViewFlipper mViewFlipper;
    private static Timer textUpdateTimer;
    private SharedPreferences settings;
    protected static int heightPixels;
    protected static int widthPixels;
    private static Timer loadPicsTimer;
    protected static ImageView imageView1;
    protected static ImageView imageView2;
    protected static ImageView imageView3;
    protected static ImageView imageView4;
    protected static int i = 0;
    private final ScheduledExecutorService textScheduler =
            Executors.newScheduledThreadPool(1);
    private final ScheduledExecutorService imageScheduler =
            Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> textUpdateHandle;
    private ScheduledFuture<?> pictureDisplayHandle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_slideshow);
        settings = getSharedPreferences("com.example.tpalny.myapplication_preferences", Context.MODE_PRIVATE);
        mText = (ScrollTextView) findViewById(R.id.my_text);
        mViewFlipper = (ViewFlipper) findViewById(R.id.flipper);

        imageView1 = (ImageView) findViewById(R.id.im1);
        imageView2 = (ImageView) findViewById(R.id.im2);
        imageView3 = (ImageView) findViewById(R.id.im3);
        imageView4 = (ImageView) findViewById(R.id.im4);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        heightPixels = metrics.heightPixels;
        widthPixels = metrics.widthPixels;
        //checking if the device has navigation bar at the bottom before hiding it
        //boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        boolean hasMenuKey = ViewConfiguration.get(this).hasPermanentMenuKey();
        if (!hasMenuKey) {
            // Do whatever you need to do, this device has a navigation bar
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        int x = 0;
        while (Select_Folders.imagesList == null) {
            x++;
            if (x > MAX_NUM_OF_ATTEMPTS) {
                x = 0;
                //mViewFlipper.stopFlipping();
                finish();
            }
        }

        if (Select_Folders.isSlideShowWithText) {
            new ReadTextFile().execute();
            Runnable textUpdate = new Runnable() {
                @Override
                public void run() {
                    new SearchTask(FullscreenSlideshow.this, false, true).execute();
                    new ReadTextFile().execute();
                }
            };
            Integer rate = Integer.parseInt(Select_Folders.textFileRefreshRate.getText().toString());
            textUpdateHandle = textScheduler.scheduleAtFixedRate(textUpdate, rate, rate, TimeUnit.MINUTES);

            /*TimerTask textUpdateTask = new TimerTask() {
                @Override
                public void run() {
                    new SearchTask(FullscreenSlideshow.this, false, true).execute();
                    new ReadTextFile().execute();
                }
            };
            textUpdateTimer = new Timer();
            Integer rate = Integer.parseInt(Select_Folders.textFileRefreshRate.getText().toString());
            textUpdateTimer.schedule(textUpdateTask, rate * 60000, rate * 60000);*/
        }
        startTimer();

    }


    public void startTimer() {

        Integer delay = Integer.parseInt(Select_Folders.slideShowDelay.getText().toString());
        Runnable imageDisplay = new Runnable() {
            @Override
            public void run() {
                new DisplayImage(FullscreenSlideshow.this)
                        .executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            }
        };

        pictureDisplayHandle = imageScheduler.scheduleAtFixedRate(imageDisplay, 0, delay, TimeUnit.SECONDS);
        /*final TimerTask loadPicsTask = new TimerTask() {
            @Override
            public void run() {
                if(loadPicsTimer == null){
                    cancel();
                }
                new DisplayImage(FullscreenSlideshow.this)
                        .executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            }
        };
        loadPicsTimer = new Timer();

        loadPicsTimer.schedule(loadPicsTask, 0, delay * 1000);*/

    }


    public void onImageClicked(View view) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(Select_Folders.USER_CANCELLED_SLIDESHOW, true).apply();
        cancelTimer();
        /*cancelTimer(textUpdateTimer);
        cancelTimer(loadPicsTimer);*/
        clearMem();
        finish();
    }

    private void clearMem() {
        imageView1.setImageBitmap(null);
        imageView2.setImageBitmap(null);
        imageView3.setImageBitmap(null);
        imageView4.setImageBitmap(null);
    }

    @Override
    public void onBackPressed() {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(Select_Folders.USER_CANCELLED_SLIDESHOW, true).apply();
        cancelTimer();
        /*cancelTimer(textUpdateTimer);
        cancelTimer(loadPicsTimer);*/
        clearMem();
        super.onBackPressed();
    }

    private void cancelTimer() {
        if (textUpdateHandle != null) {
            textUpdateHandle.cancel(true);
        }
        pictureDisplayHandle.cancel(true);
        /*if (timer != null){
            timer.cancel();
            timer.purge();
            timer = null;
        }*/
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
        clearMem();
        unbindDrawables(findViewById(R.id.root_view));
        System.gc();
    }

    private void unbindDrawables(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
        }
    }

}
