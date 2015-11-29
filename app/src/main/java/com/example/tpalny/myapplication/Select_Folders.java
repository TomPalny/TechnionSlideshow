package com.example.tpalny.myapplication;

import android.content.Intent;
import android.content.IntentSender;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.OpenFileActivityBuilder;

public class Select_Folders extends DriveConnector {

    private GoogleApiClient mGoogleApiClient;
    private IntentSender intentSender;
    private Button slideShowButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select__folders);
        slideShowButton = (Button) findViewById(R.id.Start_Slideshow_Button);
        slideShowButton.setEnabled(false);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }


    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    private static final String TAG = "PickFolderWithOpener";

    private static final int REQUEST_CODE_IMAGE_OPENER = 1;
    private static final int REQUEST_CODE_TEXT_OPENER = 2;

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);
        intentSender = Drive.DriveApi
                .newOpenFileActivityBuilder()
                .setMimeType(new String[] { DriveFolder.MIME_TYPE })
                .build(getGoogleApiClient());

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_CODE_IMAGE_OPENER:
                if (resultCode == RESULT_OK) {
                    DriveId driveId = data.getParcelableExtra(
                            OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                    DriveFolder driveFolder = Drive.DriveApi.getFolder(mGoogleApiClient, driveId);
                    DriveResource.MetadataResult metadataResult = driveFolder.getMetadata(mGoogleApiClient).await();
                    if (metadataResult != null && metadataResult.getStatus().isSuccess()){
                        String folderName = metadataResult.getMetadata().getTitle();
                        TextView textView = (TextView) findViewById(R.id.Selected_Image_Folder);
                        textView.setText(folderName);
                        slideShowButton.setEnabled(true);
                    }

                }
                finish();
                break;
            case REQUEST_CODE_TEXT_OPENER:
                if (resultCode == RESULT_OK) {
                    DriveId driveId = data.getParcelableExtra(
                            OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                    DriveFolder driveFolder = Drive.DriveApi.getFolder(mGoogleApiClient, driveId);
                    DriveResource.MetadataResult metadataResult = driveFolder.getMetadata(mGoogleApiClient).await();
                    if (metadataResult != null && metadataResult.getStatus().isSuccess()){
                        String folderName = metadataResult.getMetadata().getTitle();
                        TextView textView = (TextView) findViewById(R.id.Selected_Text_Folder);
                        textView.setText(folderName);
                    }

                }
                finish();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    public void onPicturesSelectClicked(View view) {
        try {
            startIntentSenderForResult(
                    intentSender, REQUEST_CODE_IMAGE_OPENER, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.w(TAG, "Unable to send intent", e);
        }
    }

    public void onTextSelectClicked(View view) {
        try {
            startIntentSenderForResult(
                    intentSender, REQUEST_CODE_TEXT_OPENER, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.w(TAG, "Unable to send intent", e);
        }
    }

    public void onPlaySlideshowClicked(View view) {

    }
}
