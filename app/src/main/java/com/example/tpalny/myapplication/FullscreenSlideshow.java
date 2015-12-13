package com.example.tpalny.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import com.bumptech.glide.Glide;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveId;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;


public class FullscreenSlideshow extends AppCompatActivity {


    private ImageView mImageView;
    private int currentPic = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_slideshow);

        mImageView = (ImageView) findViewById(R.id.my_image);

    }

    private class DisplayImage extends AsyncTask<Void, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                currentPic = currentPic % Select_Folders.imagesList.size();
                HttpResponse resp =
                        Select_Folders.mGOOSvc.getRequestFactory()
                                .buildGetRequest(new GenericUrl(Select_Folders.imagesList
                                        .get(currentPic++).getDownloadUrl())).execute();
                InputStream is = resp.getContent();

                return BitmapFactory.decodeStream(is);
            } catch (IOException e) {
                // An error occurred.
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bm) {
            mImageView.setImageBitmap(bm);
            /*try {
                Integer delay = Integer.parseInt(Select_Folders.slideShowDelay.getText().toString());
                Thread.sleep(delay * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/

        }
    }

    private class SetDelay extends AsyncTask<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... params) {
            try {
                Thread.sleep(params[0] * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            new DisplayImage().execute();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        while (Select_Folders.imagesList == null) {
        }
        //Integer delay = Integer.parseInt(Select_Folders.slideShowDelay.getText().toString());
        //new SetDelay().execute(delay);
        new DisplayImage().execute();
        /*while (true) {

        }*/


    }

    public void onImageClicked(View view) {
        finish();
    }


}
