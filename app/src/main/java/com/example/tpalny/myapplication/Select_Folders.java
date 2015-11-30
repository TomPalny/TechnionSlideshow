package com.example.tpalny.myapplication;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.OpenFileActivityBuilder;

public class Select_Folders extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private IntentSender intentSender;
    private Button slideShowButton;
    //protected static final int REQUEST_CODE_RESOLUTION = 1;
    private boolean mResolvingError = false;
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final String DIALOG_ERROR = "dialog_error";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select__folders);
        slideShowButton = (Button) findViewById(R.id.Start_Slideshow_Button);
        slideShowButton.setEnabled(false);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addScope(Drive.SCOPE_APPFOLDER) // required for App Folder sample
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addScope(Drive.SCOPE_APPFOLDER) // required for App Folder sample
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        //mGoogleApiClient.connect();
    }

    /*@Override
    protected void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }*/
    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {  // more about this later
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    private static final String TAG = "PickFolderWithOpener";

    private static final int REQUEST_CODE_IMAGE_OPENER = 1;
    private static final int REQUEST_CODE_TEXT_OPENER = 2;


    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            // Make sure the app is not already connected or attempting to connect
            if (!mGoogleApiClient.isConnecting() &&
                    !mGoogleApiClient.isConnected()) {
                mGoogleApiClient.connect();
            }
            if (requestCode == REQUEST_RESOLVE_ERROR) {
                mResolvingError = false;
                return;
            }
            DriveId driveId = data.getParcelableExtra(
                    OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);

            DriveFolder driveFolder = driveId.asDriveFolder();
            driveFolder.getMetadata(mGoogleApiClient).setResultCallback(new ResultCallback<DriveResource.MetadataResult>() {
                @Override
                public void onResult(DriveResource.MetadataResult metadataResult) {
                    String folderName = metadataResult.getMetadata().getTitle();
                    TextView textView=null;
                    if (requestCode == REQUEST_CODE_IMAGE_OPENER){
                        textView = (TextView) findViewById(R.id.Selected_Image_Folder);
                        slideShowButton.setEnabled(true);
                    }else if (requestCode == REQUEST_CODE_TEXT_OPENER) {
                        textView = (TextView) findViewById(R.id.Selected_Text_Folder);
                    }

                    textView.setText(folderName);

                }
            });
            switch(requestCode) {
                case REQUEST_CODE_IMAGE_OPENER:


                    break;
                case REQUEST_CODE_TEXT_OPENER:

                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
                    break;
            }
        }
    }

    public void onPicturesSelectClicked(View view) {
        //mGoogleApiClient.connect();
        intentSender = Drive.DriveApi
                .newOpenFileActivityBuilder()
                .setMimeType(new String[] { DriveFolder.MIME_TYPE })
                .build(mGoogleApiClient);
        try {
            startIntentSenderForResult(
                    intentSender, REQUEST_CODE_IMAGE_OPENER, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.w(TAG, "Unable to send intent", e);
        }
    }

    public void onTextSelectClicked(View view) {
        intentSender = Drive.DriveApi
                .newOpenFileActivityBuilder()
                .setMimeType(new String[] { DriveFolder.MIME_TYPE })
                .build(mGoogleApiClient);
        try {
            startIntentSenderForResult(
                    intentSender, REQUEST_CODE_TEXT_OPENER, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.w(TAG, "Unable to send intent", e);
        }
    }

    public void onPlaySlideshowClicked(View view) {

    }

        @Override
    public void onConnected(Bundle bundle) {
    Log.i("SELECT_FOLDERS", "Google API Client is Connected");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        }else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
            /*// show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;*/
        } else
            showErrorDialog(result.getErrorCode());
        mResolvingError = true;
        /*try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }*/
    }

    // The rest of this code is all about building the error dialog

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getFragmentManager() , "errorDialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((Select_Folders) getActivity()).onDialogDismissed();
        }
    }
}
