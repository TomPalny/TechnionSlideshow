package com.example.tpalny.myapplication;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.support.v4.app.ActivityCompat.startActivityForResult;

/**
 * Created by Tom on 15/12/2015.
 */
class SearchTask extends AsyncTask<Void, Void, Void> {

    private Exception mLastError = null;
    private String mPicturesFolderId = Select_Folders.picturesFolderID;
    private String mTextFolderId = Select_Folders.textFolderID;
    private Boolean mIsImage = false;
    private Boolean mIsText = false;
    protected static Drive mGOOSvc = null;
    private Context mContext;

    public SearchTask(Context context, Boolean isImage, Boolean isText) {

        mIsImage = isImage;
        mIsText = isText;
        mContext = context;

        mGOOSvc = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(),
                Select_Folders.mCredential)
                .setApplicationName("Technion Slideshow App").build();

    }

    /**
     * Background task to call Drive API.
     *
     * @param params no parameters needed for this task.
     */
    @Override
    protected Void doInBackground(Void... params) {
        try {
            Select_Folders.connectToGoogleService(mContext);
            getDataFromApi();

        } catch (Exception e) {
            mLastError = e;
            cancel(true);
        }
        return null;
    }

    /**
     * List of Strings describing files, or an empty list if no files
     * found.
     *
     * @throws IOException
     */
    private void getDataFromApi() throws IOException {


        FileList result = null;
        if (mPicturesFolderId != null) {
            if (mIsImage) {
                result = mGOOSvc.files().list().setQ("'" + mPicturesFolderId +
                        "' in parents and mimeType contains 'image/' and trashed = false")
                        .execute();
            }
            if (mIsText) {
                result = mGOOSvc.files().list().setQ("'" + mTextFolderId +
                        "' in parents and mimeType = 'text/plain' and trashed = false")
                        .execute();
            }

        } else {
            result = mGOOSvc.files().list().setQ("mimeType contains 'image/' and trashed = false")
                    .execute();
        }
        List<File> files = result.getItems();
        if (files != null) {
            if (mIsImage) {
                Select_Folders.imagesList = new ArrayList<>();
                for (File file : files) {
                    Select_Folders.imagesList.add(file);
                }
            }
            if (mIsText) {
                Select_Folders.textList = new ArrayList<>();
                Select_Folders.isSlideShowWithText = true;
                for (File file : files) {
                    Select_Folders.textList.add(file);
                }
            }
        }

    }


    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (mIsImage && Select_Folders.imagesList.size() == 0) {
            Toast.makeText(mContext, "No Image files in folder", Toast.LENGTH_LONG).show();

        } else if (mIsText && Select_Folders.noTextFoundMessageFirstTimeAppearance &&
                Select_Folders.textList.size() == 0) {
            Toast.makeText(mContext, "No Text files in folder", Toast.LENGTH_LONG).show();
            Select_Folders.noTextFoundMessageFirstTimeAppearance = false;

        } else {
            if (mContext instanceof Select_Folders)
                new Select_Folders.DownloadTask(mContext, Select_Folders.imagesList.size(), true).execute();
            else
                new Select_Folders.DownloadTask(mContext, Select_Folders.imagesList.size(), false).execute();

        }
    }

    @Override
    protected void onCancelled() {
        if (mLastError != null) {
            if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                GoogleApiAvailability api = GoogleApiAvailability.getInstance();
                api.showErrorDialogFragment((Activity) mContext, ((GooglePlayServicesAvailabilityIOException) mLastError)
                        .getConnectionStatusCode(), Select_Folders.REQUEST_GOOGLE_PLAY_SERVICES);
            } else if (mLastError instanceof UserRecoverableAuthIOException) {
                startActivityForResult((Activity) mContext,
                        ((UserRecoverableAuthIOException) mLastError).getIntent(),
                        Select_Folders.REQUEST_AUTHORIZATION, null);
            } else {
                Log.i("SearchTask", "Error Occured " + mLastError.getMessage());
            /*new AlertDialog.Builder(mContext)
                    .setMessage("The following error occurred:\n"
                            + mLastError.getMessage()).show();*/
            }

        } else {
            Log.i("SearchTask", "Request cancelled.");
            /*new AlertDialog.Builder(mContext)
                    .setMessage("Request cancelled.").show();*/
        }
    }
}
