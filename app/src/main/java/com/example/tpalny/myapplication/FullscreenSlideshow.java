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


    private ImageView mImageView;
    private int currentPic = 0;
    //private Boolean firstPass = true;
    private Timer timer;
    private TimerTask timerTask;
    private final Handler handler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_slideshow);

        mImageView = (ImageView) findViewById(R.id.my_image);

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
            if (bm == null) {
                cancelTimerAndReturn();
            }
            if (currentPic == Select_Folders.imagesList.size()) {
                new SearchTask(FullscreenSlideshow.this, true, false).execute();
            }

            /*FileOutputStream out = null;
            try {
                out = new FileOutputStream("R.drawable.my_image.xml");
                bm.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                // PNG is a lossless format, the compression factor (100) is ignored
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }*/
            mImageView.setImageBitmap(bm);
            //Drawable d = new BitmapDrawable(getResources(),bm);
            //Picasso.with(getApplicationContext()).load(R.drawable.my_image);
            /*try {
                Integer delay = Integer.parseInt(Select_Folders.slideShowDelay.getText().toString());
                Thread.sleep(delay * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/

        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        while (Select_Folders.imagesList == null) {
        }
        //Integer delay = Integer.parseInt(Select_Folders.slideShowDelay.getText().toString());
        startTimer();


    }

    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        Integer delay = Integer.parseInt(Select_Folders.slideShowDelay.getText().toString());
        timer.schedule(timerTask, 0, delay * 1000); //
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
