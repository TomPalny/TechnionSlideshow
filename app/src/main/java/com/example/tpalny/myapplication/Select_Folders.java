package com.example.tpalny.myapplication;

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.OpenFileActivityBuilder;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Select_Folders extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final int MAXIMUM_ATTEMPTS_TO_CONNECT = 3;
    private static final int REQUEST_CODE_RESOLUTION = 1004;
    private GoogleApiClient mGoogleApiClient;
    protected static GoogleAccountCredential mCredential;
    protected static Drive mGOOSvc;
    private IntentSender intentSender;
    private Button slideShowButton;
    private boolean mResolvingError = false;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_AUTHORIZATION = 1003;
    private static final String DIALOG_ERROR = "dialog_error";
    private boolean textWasSelected = false;
    private boolean picturesWereSelected = false;
    private TextView pictureSelectionText;
    private TextView textSelectionText;

    //private static boolean mConnected;
    private static final String[] SCOPES = {DriveScopes.DRIVE_READONLY};

    private static String folderName = null;
    protected static String folderID = null;
    public static List<File> imagesList;
    public static List<File> textList;
    protected static DriveId globalDriveId;
    protected static EditText slideShowDelay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select__folders);
        slideShowButton = (Button) findViewById(R.id.Start_Slideshow_Button);
        slideShowButton.setEnabled(false);
        slideShowButton.setAlpha(.5f);
        pictureSelectionText = (TextView) findViewById(R.id.Selected_Image_Folder);
        textSelectionText = (TextView) findViewById(R.id.Selected_Text_Folder);
        slideShowDelay = (EditText) findViewById(R.id.slideshow_delay);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(com.google.android.gms.drive.Drive.API)
                .addScope(com.google.android.gms.drive.Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(settings.getString("accountName", null));
        mGOOSvc = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), mCredential)
                .setApplicationName("Technion Slideshow App").build();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (isGooglePlayServicesAvailable()) {
            refreshResults();
        } else {
            new AlertDialog.Builder(getApplicationContext())
                    .setMessage("Google Play Services required: " +
                            "after installing, close and relaunch this app.").show();
        }
        if (!mGoogleApiClient.isConnecting() &&
                !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    private void refreshResults() {
        if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        }
            /*if (isDeviceOnline()) {
                new SearchTask(mCredential, folderID, false, false).execute();
            } else {
                new AlertDialog.Builder(getApplicationContext())
                        .setMessage("No network connection available.").show();
            }*/

    }


    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private void chooseAccount() {
        startActivityForResult(
                mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    private boolean isGooglePlayServicesAvailable() {
        final int connectionStatusCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        } else if (connectionStatusCode != ConnectionResult.SUCCESS) {
            return false;
        }
        return true;
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                connectionStatusCode,
                Select_Folders.this,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    @Override
    protected void onPause() {
        /*if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }*/
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {  // more about this later
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        //mGoogleApiClient.disconnect();
        super.onStop();
    }

    private static final String TAG = "PickFolderWithOpener";

    private static final int REQUEST_CODE_IMAGE_OPENER = 1;
    private static final int REQUEST_CODE_TEXT_OPENER = 2;


    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        // Make sure the app is not already connected or attempting to connect
        if (!mGoogleApiClient.isConnecting() &&
                !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            return;
        }
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    isGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String email =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (email != null) {
                        mCredential.setSelectedAccountName(email);
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("accountName", email);
                        editor.apply();
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    new AlertDialog.Builder(Select_Folders.this)
                            .setMessage("Account unspecified.").show();
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode != RESULT_OK) {
                    chooseAccount();
                }
                break;
            case REQUEST_CODE_IMAGE_OPENER:
                pictureOrTextSelection(true, false, data);
                break;
            case REQUEST_CODE_TEXT_OPENER:
                pictureOrTextSelection(false, true, data);
                break;
        }
    }

    private void pictureOrTextSelection(final boolean isImageCase, final boolean isTextCase, Intent data) {
        if (data == null) return;
        final DriveId driveId = data.getParcelableExtra(
                OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
        globalDriveId = driveId;

        final DriveFolder driveFolder = driveId.asDriveFolder();
        driveFolder.getMetadata(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveResource.MetadataResult>() {
                    @Override
                    public void onResult(DriveResource.MetadataResult metadataResult) {
                        folderName = metadataResult.getMetadata().getTitle();
                        folderID = driveId.getResourceId();

                        if (isImageCase) {
                            pictureSelectionText.setText(folderName);
                            slideShowButton.setEnabled(true);
                            slideShowButton.setAlpha(1);
                            picturesWereSelected = true;
                            if (isDeviceOnline()) {
                                new SearchTask(Select_Folders.this, true, false).execute();

                            } else {
                                new AlertDialog.Builder(getApplicationContext())
                                        .setMessage("No network connection available.").show();
                            }

                        } else if (isTextCase) {
                            textSelectionText.setText(folderName);
                            textWasSelected = true;
                            if (isDeviceOnline()) {
                                new SearchTask(Select_Folders.this, false, true).execute();

                            } else {
                                new AlertDialog.Builder(Select_Folders.this)
                                        .setMessage("No network connection available.").show();
                            }
                        }
                    }
                });
    }

    public void onPicturesSelectClicked(View view) {
        int x = 0;
        while (x++ <= MAXIMUM_ATTEMPTS_TO_CONNECT) {
            if (!mGoogleApiClient.isConnecting() &&
                    !mGoogleApiClient.isConnected()) {
                mGoogleApiClient.connect();
            }
        }

        intentSender = com.google.android.gms.drive.Drive.DriveApi
                .newOpenFileActivityBuilder()
                .setMimeType(new String[]{DriveFolder.MIME_TYPE})
                .build(mGoogleApiClient);
        try {
            startIntentSenderForResult(
                    intentSender, REQUEST_CODE_IMAGE_OPENER, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.w(TAG, "Unable to send intent", e);
        }
    }

    public void onTextSelectClicked(View view) {
        int x = 0;
        while (x++ <= MAXIMUM_ATTEMPTS_TO_CONNECT) {
            if (!mGoogleApiClient.isConnecting() &&
                    !mGoogleApiClient.isConnected()) {
                mGoogleApiClient.connect();
            }
        }
        intentSender = com.google.android.gms.drive.Drive.DriveApi
                .newOpenFileActivityBuilder()
                .setMimeType(new String[]{DriveFolder.MIME_TYPE})
                .build(mGoogleApiClient);
        try {
            startIntentSenderForResult(
                    intentSender, REQUEST_CODE_TEXT_OPENER, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.w(TAG, "Unable to send intent", e);
        }
    }


    public void onClearPicturesClicked(View view) {
        picturesWereSelected = false;
        slideShowButton.setEnabled(false);
        slideShowButton.setAlpha(.5f);
        pictureSelectionText.setText("None Selected");
    }

    public void onClearTextClicked(View view) {
        textWasSelected = false;
        textSelectionText.setText("None Selected");
    }

    public void onPlaySlideshowClicked(View view) {
        String delay = slideShowDelay.getText().toString();
        if (delay.isEmpty() || delay.equals("0")) {
            new AlertDialog.Builder(this)
                    .setMessage("Please enter a number greater than 0.").show();
            return;
        }
        Intent intent = new Intent(this, FullscreenSlideshow.class);
        startActivity(intent);
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
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
            return;
        } else
            showErrorDialog(result.getErrorCode());
        mResolvingError = true;
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
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
        dialogFragment.show(getFragmentManager(), "errorDialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() {
        }

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