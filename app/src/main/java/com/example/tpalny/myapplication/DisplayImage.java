package com.example.tpalny.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by tpalny on 20/12/2015.
 */
public class DisplayImage extends AsyncTask<Void, Void, Bitmap> {
    protected static int currentPic = 0;
    private Context mContext;
    private ViewFlipper mViewFlipper = FullscreenSlideshow.mViewFlipper;
    private ImageView im1 = FullscreenSlideshow.imageView1;
    private ImageView im2 = FullscreenSlideshow.imageView2;
    private Drive mGOOSvc = SearchTask.mGOOSvc;

    DisplayImage(Context context) {
        mContext = context;
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        HttpResponse resp;
        InputStream is;
        try {
            if (Select_Folders.imagesList.isEmpty()) {
                return null;
            }
            currentPic = currentPic % Select_Folders.imagesList.size();
            GenericUrl url = new GenericUrl(Select_Folders.imagesList
                    .get(currentPic++).getDownloadUrl());
            resp = mGOOSvc.getRequestFactory().buildGetRequest(url).execute();
            is = resp.getContent();

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);


            final int imageHeight = options.outHeight;
            final int imageWidth = options.outWidth;

            int inSampleSize = 1;

            if (imageHeight > FullscreenSlideshow.heightPixels || imageWidth > FullscreenSlideshow.widthPixels) {

                final int halfHeight = imageHeight / 2;
                final int halfWidth = imageWidth / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) > FullscreenSlideshow.heightPixels
                        && (halfWidth / inSampleSize) > FullscreenSlideshow.widthPixels) {
                    inSampleSize *= 2;
                }
            }

            options.inJustDecodeBounds = false;
            Bitmap bm = null;
            while (bm == null) {
                options.inSampleSize = inSampleSize;

                // Decode bitmap with inSampleSize set

                is.close();
                resp.disconnect();
                resp = mGOOSvc.getRequestFactory().buildGetRequest(url).execute();
                is = resp.getContent();
                try {
                    bm = BitmapFactory.decodeStream(is, null, options);
                } catch (OutOfMemoryError e) {
                    inSampleSize *= 2;
                }
            }
            is.close();
            resp.disconnect();
            return bm;


        } catch (IOException e) {
            // An error occurred.
            e.printStackTrace();
        }
        return null;

    }


    @Override
    protected void onPostExecute(Bitmap bm) {

        if (currentPic == Select_Folders.imagesList.size()) {
            Select_Folders.loadPicsTimer.cancel();
            Toast.makeText(mContext, "Finished loading images, num of Images= " + Select_Folders.imagesList.size(), Toast.LENGTH_SHORT).show();
            //new SearchTask(mContext, true, false).execute();
        } else if (bm == null) {
            Toast.makeText(mContext, "bm = null!", Toast.LENGTH_SHORT).show();
        }
        if (currentPic % 2 == 0) {
            im1.setImageBitmap(bm);

        } else {
            im2.setImageBitmap(bm);
        }


    }
}
