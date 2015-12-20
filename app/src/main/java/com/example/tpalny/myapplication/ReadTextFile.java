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
        HttpResponse resp = null;
        try {
            if (Select_Folders.textList.isEmpty()) {
                return null;
            }
            resp =
                    Select_Folders.mGOOSvc.getRequestFactory()
                            .buildGetRequest(new GenericUrl(Select_Folders.textList
                                    .get(0).getDownloadUrl())).execute();
            InputStream is = resp.getContent();

            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            StringBuilder total = new StringBuilder(is.available());
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
                total.append("      ");
            }
            return total.toString();
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
    protected void onPostExecute(String result) {
        FullscreenSlideshow.mText.setText(result);
        FullscreenSlideshow.mText.setRndDuration(250);
        FullscreenSlideshow.mText.startScroll();
    }
}
