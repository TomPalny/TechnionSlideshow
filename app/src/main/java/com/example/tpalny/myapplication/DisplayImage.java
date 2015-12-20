package com.example.tpalny.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by tpalny on 20/12/2015.
 */
public class DisplayImage extends AsyncTask<Void, Void, Bitmap> {
    private static int currentPic = 0;
    private Context mContext;
    private ImageView mImageView1 = FullscreenSlideshow.mImageView1;
    private ImageView mImageView2 = FullscreenSlideshow.mImageView2;
    private ViewFlipper mViewFlipper = FullscreenSlideshow.mViewFlipper;

    DisplayImage(Context context) {
        mContext = context;
    }

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
            new SearchTask(mContext, true, false).execute();
        }
        if (currentPic % 2 == 0) {
            mImageView1.setImageBitmap(bm);
            mViewFlipper.setDisplayedChild(0);

        } else {
            mImageView2.setImageBitmap(bm);
            mViewFlipper.setDisplayedChild(1);
        }

    }
}
