package com.example.tpalny.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;


public class FullscreenSlideshow extends AppCompatActivity {


    private static final int MAX_NUM_OF_ATTEMPTS = 5;
    private ImageView mImageView1;
    private ImageView mImageView2;
    private int currentPic = 0;
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

    }

    private class DisplayImage extends AsyncTask<Void, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Void... params) {
            HttpResponse resp = null;
            try {
                if (Select_Folders.imagesList.isEmpty()) {
                    return null;
                }
                currentPic = currentPic % Select_Folders.imagesList.size();
                resp =
                        Select_Folders.mGOOSvc.getRequestFactory()
                                .buildGetRequest(new GenericUrl(Select_Folders.imagesList
                                        .get(currentPic++).getDownloadUrl())).execute();
                InputStream is = resp.getContent();

                return BitmapFactory.decodeStream(is);
            } catch (IOException e) {
                // An error occurred.
                e.printStackTrace();
            } finally {
                if (resp != null) {
                    try {
                        resp.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
            return null;

        }

        @Override
        protected void onPostExecute(Bitmap bm) {

            if (currentPic == Select_Folders.imagesList.size() || bm == null) {
                new SearchTask(FullscreenSlideshow.this, true, false).execute();
            }
            if (currentPic % 2 == 0) {
                mImageView2.setImageResource(android.R.color.transparent);
                mImageView1.setImageBitmap(bm);

            } else {
                mImageView1.setImageResource(android.R.color.transparent);
                mImageView2.setImageBitmap(bm);
            }

        }
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

        startTimer();


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
                        new DisplayImage().execute();

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
