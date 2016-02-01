package com.example.tpalny.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by tpalny on 31/01/2016.
 */
public class LoadImage {
    protected static int currentPic = 0;
    private final String mImageName;
    private Context mContext;
    private ViewFlipper mViewFlipper = FullscreenSlideshow.mViewFlipper;
    private ImageView im1 = FullscreenSlideshow.imageView1;
    private ImageView im2 = FullscreenSlideshow.imageView2;
    private ImageView im3 = FullscreenSlideshow.imageView3;
    private ImageView im4 = FullscreenSlideshow.imageView4;

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

    LoadImage(Context context, String image) {
        mContext = context;
        mImageName = image;
        mViewFlipper.setInAnimation(mContext, inAnimation[FullscreenSlideshow.i]);
        mViewFlipper.setOutAnimation(mContext, outAnimation[FullscreenSlideshow.i]);
        if (currentPic % animationChangeCounter == 0) {
            FullscreenSlideshow.i = (++FullscreenSlideshow.i) % inAnimation.length;
        }
    }

    private void loadNext() {
        Bitmap b = null;
        FileInputStream fis;
        try {
            fis = mContext.openFileInput(mImageName + currentPic++);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(fis, null, options);
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
                try {
                    bm = BitmapFactory.decodeStream(fis, null, options);
                } catch (OutOfMemoryError e) {
                    inSampleSize *= 2;
                }
            }
            setImage(bm);
            fis.close();

        } catch (FileNotFoundException e) {
            Log.d("LoadImage", "file not found");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d("LoadImage", "io exception");
            e.printStackTrace();
        }
    }


    public void setImage(Bitmap bm) {
        if (currentPic == Select_Folders.imagesList.size() || bm == null) {
            //Toast.makeText(mContext, "Finished loading images, num of Images= " + Select_Folders.imagesList.size(), Toast.LENGTH_SHORT).show();
            FullscreenSlideshow.i = (++FullscreenSlideshow.i) % inAnimation.length;
            currentPic = 0;

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
