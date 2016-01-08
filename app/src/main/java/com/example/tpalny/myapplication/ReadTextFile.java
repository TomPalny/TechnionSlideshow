package com.example.tpalny.myapplication;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by tpalny on 20/12/2015.
 */
public class ReadTextFile extends AsyncTask<Void, Void, String> {
    private Drive mGOOSvc = SearchTask.mGOOSvc;

    @Override
    protected String doInBackground(Void... params) {
        if (Select_Folders.textList.isEmpty()) {
            return null;
        }
        HttpResponse resp = null;
        StringBuilder superTotal = new StringBuilder();
        int currentTextFileNum = 0;
        while (currentTextFileNum < Select_Folders.textList.size()) {

            try {
                resp =
                        mGOOSvc.getRequestFactory()
                                .buildGetRequest(new GenericUrl(Select_Folders.textList
                                        .get(currentTextFileNum++).getDownloadUrl())).execute();
                InputStream is = resp.getContent();

                BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder total = new StringBuilder(is.available());
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line);
                    total.append("      ");
                }
                superTotal.append(total);
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
        }
        return superTotal.toString();


    }

    @Override
    protected void onPostExecute(String result) {
        if (result == null) {
            FullscreenSlideshow.mText.setVisibility(View.INVISIBLE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            FullscreenSlideshow.mViewFlipper.setLayoutParams(params);
            return;
        }
        FullscreenSlideshow.mText.setText(result);
        FullscreenSlideshow.mText.setBackgroundColor(Color.WHITE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ABOVE, FullscreenSlideshow.mText.getId());
        FullscreenSlideshow.mViewFlipper.setLayoutParams(params);

        Integer[] speeds = {500, 450, 400, 350, 300, 250, 200, 150, 100, 75};
        Integer selectedSpeed = Integer.parseInt(Select_Folders.textScrollSpeed.getText().toString()) - 1;
        FullscreenSlideshow.mText.setRndDuration(speeds[selectedSpeed]);
        FullscreenSlideshow.mText.startScroll();
    }
}
