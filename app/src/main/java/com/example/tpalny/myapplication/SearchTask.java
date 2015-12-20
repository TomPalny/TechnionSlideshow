package com.example.tpalny.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tom on 15/12/2015.
 */
class SearchTask extends AsyncTask<Void, Void, Void> {

    private Exception mLastError = null;
    private String mFolderId = Select_Folders.picturesFolderID;
    private Boolean mIsImage = false;
    private Boolean mIsText = false;
    private Drive mGOOSvc = Select_Folders.mGOOSvc;
    private Context mContext;

    public SearchTask(Context context, Boolean isImage, Boolean isText) {

        mIsImage = isImage;
        mIsText = isText;
        mContext = context;

    }

    /**
     * Background task to call Drive API.
     *
     * @param params no parameters needed for this task.
     */
    @Override
    protected Void doInBackground(Void... params) {
        try {
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
        if (mFolderId != null) {
            if (mIsImage) {
                result = mGOOSvc.files().list().setQ("'" + mFolderId +
                        "' in parents and mimeType contains 'image/' and trashed = false")
                        .execute();
            }
            if (mIsText) {
                result = mGOOSvc.files().list().setQ("'" + mFolderId + "'" +
                        " in parents and mimeType = 'text/plain' and trashed = false")
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
                for (File file : files) {
                    Select_Folders.textList.add(file);
                }
            }
        }

    }


    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (Select_Folders.imagesList.size() == 0) {
            Toast.makeText(mContext, "No matching files in folder.", Toast.LENGTH_LONG).show();

        }
    }

    @Override
    protected void onCancelled() {
        if (mLastError != null) {

            new AlertDialog.Builder(mContext)
                    .setMessage("The following error occurred:\n"
                            + mLastError.getMessage()).show();

        } else {
            new AlertDialog.Builder(mContext)
                    .setMessage("Request cancelled.").show();
        }
    }
}
