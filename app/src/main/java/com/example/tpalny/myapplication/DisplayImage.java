package com.example.tpalny.myapplication;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import com.google.api.services.drive.Drive;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.TimerTask;

/**
 * Created by tpalny on 20/12/2015.
 */
public class DisplayImage extends AsyncTask<Void, Void, File>{
    protected static int currentPic = 0;
    private Context mContext;
    private ViewFlipper mViewFlipper = FullscreenSlideshow.mViewFlipper;
    private ImageView im1 = FullscreenSlideshow.imageView1;
    private ImageView im2 = FullscreenSlideshow.imageView2;
    private ImageView im3 = FullscreenSlideshow.imageView3;


    private static final int[] inAnimation = {R.anim.fade_in, R.anim.grow_fade_in_from_bottom,
            R.anim.popup_enter};
    private static final int[] outAnimation = {R.anim.fade_out, R.anim.shrink_fade_out_from_bottom,
            R.anim.popup_exit};



    DisplayImage(Context context) {
        mContext = context;
        mViewFlipper.setInAnimation(mContext, inAnimation[FullscreenSlideshow.i]);
        mViewFlipper.setOutAnimation(mContext, outAnimation[FullscreenSlideshow.i]);
        if (currentPic % inAnimation.length == 0) {
            FullscreenSlideshow.i = (++FullscreenSlideshow.i) % inAnimation.length;
        }

    }

    @Override
    protected File doInBackground(Void... params) {
        File[] files = Select_Folders.myDir.listFiles();
        currentPic = currentPic % files.length;
        File f = files[currentPic];
        /*BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(files[currentPic].toString(), options);

        int inSampleSize = 1;
       *//* final int imageHeight = options.outHeight;
        final int imageWidth = options.outWidth;



        if (imageHeight > FullscreenSlideshow.heightPixels || imageWidth > FullscreenSlideshow.widthPixels) {

            final int halfHeight = imageHeight / 2;
            final int halfWidth = imageWidth / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > FullscreenSlideshow.heightPixels
                    && (halfWidth / inSampleSize) > FullscreenSlideshow.widthPixels) {
                inSampleSize *= 2;
            }
        }*//*

        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;
        Bitmap bm = null;
        while (bm == null) {
            options.inSampleSize = inSampleSize;

            try {
                bm = BitmapFactory.decodeFile(files[currentPic].toString(), options);
            } catch (OutOfMemoryError e) {
                inSampleSize *= 2;
            }
        }*/
        currentPic++;
        if (currentPic == files.length) currentPic = 0;

        return f;

    }



    @Override
    protected void onPostExecute(File file) {
        if (currentPic == Select_Folders.myDir.listFiles().length ) {
            FullscreenSlideshow.i = (++FullscreenSlideshow.i) % inAnimation.length;
            /*new SearchTask(mContext, true, false).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);*/

        } /*else if (bm == null) {
            Toast.makeText(mContext, "Internet Connection Lost", Toast.LENGTH_SHORT).show();
        }*/
        if (currentPic % 3 == 0) {
            //im3.setImageBitmap(bm);
            Picasso.with(mContext).load(file).into(im3);
            mViewFlipper.setDisplayedChild(2);

        } else if (currentPic % 3 == 1) {
            //im1.setImageBitmap(bm);
            Picasso.with(mContext).load(file).into(im1);
            mViewFlipper.setDisplayedChild(0);

        } else if (currentPic % 3 == 2) {
            //im2.setImageBitmap(bm);
            Picasso.with(mContext).load(file).into(im2);
            mViewFlipper.setDisplayedChild(1);

        }

        if (currentPic % 3 == 0) {
            im1.setImageBitmap(null);
        } else if (currentPic % 3 == 1) {
            im2.setImageBitmap(null);
        } else if (currentPic % 3 == 2) {
            im3.setImageBitmap(null);
        }
    }



}
