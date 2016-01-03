package com.example.tpalny.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;
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
    private ImageView im3 = FullscreenSlideshow.imageView3;
    private ImageView im4 = FullscreenSlideshow.imageView4;
    private Drive mGOOSvc = SearchTask.mGOOSvc;
    private Bitmap bm1 = null;
    private Bitmap bm2 = null;
    private Bitmap bm3 = null;
    private Bitmap bm4 = null;
    private static final int[] inAnimation = {R.anim.fade_in, R.anim.grow_fade_in_from_bottom,
            R.anim.popup_enter, R.anim.slide_in_bottom, R.anim.slide_in_top,
            R.anim.slide_in_from_right};
    private static final int[] outAnimation = {R.anim.fade_out, R.anim.shrink_fade_out_from_bottom,
            R.anim.popup_exit, R.anim.slide_out_top, R.anim.slide_out_bottom,
            R.anim.slide_out_to_left};
    private final int animationChangeCounter = 5;


    DisplayImage(Context context) {
        mContext = context;
        mViewFlipper.setInAnimation(mContext, inAnimation[FullscreenSlideshow.i]);
        mViewFlipper.setOutAnimation(mContext, outAnimation[FullscreenSlideshow.i]);
        if (currentPic % animationChangeCounter == 0) {
            FullscreenSlideshow.i = (++FullscreenSlideshow.i) % inAnimation.length;
        }
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        HttpResponse resp = null;
        InputStream is = null;
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
            is.close();

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

                //is.close();
                //resp.disconnect();
                resp = mGOOSvc.getRequestFactory().buildGetRequest(url).execute();

                is = resp.getContent();
                try {
                    bm = BitmapFactory.decodeStream(is, null, options);
                } catch (OutOfMemoryError e) {
                    inSampleSize *= 2;
                }
            }


            if (bm1 == null || bm1.isRecycled()) {
                bm1 = bm;
                return bm1;
            } else if (bm2 == null || bm2.isRecycled()) {
                bm2 = bm;
                return bm2;
            } else if (bm3 == null || bm3.isRecycled()) {
                bm3 = bm;
                return bm3;
            } else if (bm4 == null || bm4.isRecycled()) {
                bm4 = bm;
                return bm4;
            }


        } catch (IOException e) {
            // An error occurred.
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
            //Toast.makeText(mContext, "Finished loading images, num of Images= " + Select_Folders.imagesList.size(), Toast.LENGTH_SHORT).show();
            FullscreenSlideshow.i = (++FullscreenSlideshow.i) % inAnimation.length;
            new SearchTask(mContext, true, false).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

        } /*else if (bm == null) {
            Toast.makeText(mContext, "Internet Connection Lost", Toast.LENGTH_SHORT).show();
        }*/
        if (currentPic % 4 == 0) {
            im4.setImageBitmap(bm);
            mViewFlipper.setDisplayedChild(3);


        } else if (currentPic % 4 == 1) {
            im1.setImageBitmap(bm);
            mViewFlipper.setDisplayedChild(0);

        } else if (currentPic % 4 == 2) {
            im2.setImageBitmap(bm);
            mViewFlipper.setDisplayedChild(1);

        } else if (currentPic % 4 == 3) {
            im3.setImageBitmap(bm);
            mViewFlipper.setDisplayedChild(2);
        }

        if (currentPic % 4 == 0) {
            im1.setImageBitmap(null);
        } else if (currentPic % 4 == 1) {
            im2.setImageBitmap(null);
        } else if (currentPic % 4 == 2) {
            im3.setImageBitmap(null);
        } else if (currentPic % 4 == 3) {
            im4.setImageBitmap(null);
        }


    }
}
