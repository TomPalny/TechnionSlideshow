package com.example.tpalny.myapplication;

import android.os.AsyncTask;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by tpalny on 20/12/2015.
 */
public class ReadTextFile extends AsyncTask<Void, Void, String> {

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
                        Select_Folders.mGOOSvc.getRequestFactory()
                                .buildGetRequest(new GenericUrl(Select_Folders.textList
                                        .get(currentTextFileNum++).getDownloadUrl())).execute();
                InputStream is = resp.getContent();

                BufferedReader r = new BufferedReader(new InputStreamReader(is));
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
        FullscreenSlideshow.mText.setText(result);
        Integer[] speeds = {500, 450, 400, 350, 300, 250, 200, 150, 100, 75};
        Integer selectedSpeed = Integer.parseInt(Select_Folders.textScrollSpeed.getText().toString()) - 1;
        FullscreenSlideshow.mText.setRndDuration(speeds[selectedSpeed]);
        FullscreenSlideshow.mText.startScroll();
    }
}
