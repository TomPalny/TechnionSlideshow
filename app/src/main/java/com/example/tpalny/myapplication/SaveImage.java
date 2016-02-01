package com.example.tpalny.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by tpalny on 31/01/2016.
 */
public class SaveImage extends AsyncTask<Void, Void, Bitmap> {
    protected static int currentPic = 0;
    private final String mImageName;
    private Context mContext;
    private Drive mGOOSvc = SearchTask.mGOOSvc;
    private boolean firstRun;

    SaveImage(Context context, String image) {
        mContext = context;
        mImageName = image;
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

            Bitmap bitmap = BitmapFactory.decodeStream(is);
            FileOutputStream fos = mContext.openFileOutput(mImageName + currentPic, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();


        }catch (FileNotFoundException e){
            Log.d("OpenFile", "File not found");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d("OpenFile", "io exception");
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
        if (currentPic == 2 && firstRun){
            //TODO: start timer
            new LoadImage(mContext, mImageName);
        }
        if (currentPic == Select_Folders.imagesList.size() -1 && firstRun){
            //TODO: stop timer
        }

    }
}
