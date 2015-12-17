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
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

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
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class Select_Folders extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final int MAXIMUM_ATTEMPTS_TO_CONNECT = 4;
    private static final int REQUEST_CODE_RESOLUTION = 1004;
    private static final String PICS_FOLDER_NAME_TAG = "pics_folder_name";
    private static final String TEXT_FOLDER_NAME_TAG = "text_folder_name";
    protected static final String START_ON_BOOT = "start_on_boot";
    private static final String PICS_DRIVEID = "pics_driveID";
    private static final String TEXT_DRIVEID = "text_driveID";
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
    protected static Boolean userCancelledSlideshow = false;
    private TextView pictureSelectionText;
    private TextView textSelectionText;
    private final String PICTURES_FOLDER_TAG = "pictures_folder";
    private final String TEXT_FOLDER_TAG = "text_folder";
    private final String DELAY_TAG = "delay";

    //private static boolean mConnected;
    private static final String[] SCOPES = {DriveScopes.DRIVE_READONLY};

    private static String picturesFolderName = null;
    protected static String picturesFolderID = null;
    public static List<File> imagesList;
    public static List<File> textList;
    protected static EditText slideShowDelay;
    private SharedPreferences settings;
    protected static String textFolderID = null;
    private String textFolderName = null;
    private ToggleButton toggle;


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

        settings = getSharedPreferences("com.example.tpalny.myapplication_preferences", Context.MODE_PRIVATE);
        connectToGoogleService();

        toggle = (ToggleButton) findViewById(R.id.boot_start);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = settings.edit();
                if (isChecked) {
                    editor.putString(START_ON_BOOT, "true").apply();

                } else {
                    editor.putString(START_ON_BOOT, "false").apply();

                }
            }
        });

    }

    private void connectToGoogleService() {
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

        String chosenFolder = settings.getString(PICTURES_FOLDER_TAG, "");
        if (!chosenFolder.isEmpty() && !userCancelledSlideshow) {
            populateFieldsWithExistingData();
        }
    }

    private void populateFieldsWithExistingData() {
        String driveIdDecode = settings.getString(PICS_DRIVEID, "");
        if (driveIdDecode.isEmpty()) {
            return;
        }
        final DriveId picsDriveId = DriveId.decodeFromString(driveIdDecode);
        final DriveFolder picsDriveFolder = picsDriveId.asDriveFolder();
        picsDriveFolder.getMetadata(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveResource.MetadataResult>() {
                    @Override
                    public void onResult(DriveResource.MetadataResult metadataResult) {
                        picturesFolderName = settings.getString(PICS_FOLDER_NAME_TAG, "");
                        pictureSelectionText.setText(picturesFolderName);
                        picturesFolderID = picsDriveId.getResourceId();
                        slideShowButton.setEnabled(true);
                        slideShowButton.setAlpha(1);
                        picturesWereSelected = true;
                        if (isDeviceOnline()) {
                            new SearchTask(Select_Folders.this, true, false).execute();

                        } else {
                            new AlertDialog.Builder(getApplicationContext())
                                    .setMessage("No network connection available.").show();
                        }
                    }
                });

        textFolderID = settings.getString(TEXT_FOLDER_TAG, "");
        if (!textFolderID.isEmpty()) {
            driveIdDecode = settings.getString(TEXT_DRIVEID, "");
            if (driveIdDecode.isEmpty()) {
                return;
            }
            final DriveId textDriveId = DriveId.decodeFromString(driveIdDecode);
            final DriveFolder textDriveFolder = textDriveId.asDriveFolder();
            textDriveFolder.getMetadata(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<DriveResource.MetadataResult>() {
                        @Override
                        public void onResult(DriveResource.MetadataResult metadataResult) {
                            textFolderName = settings.getString(TEXT_FOLDER_NAME_TAG, "");
                            textSelectionText.setText(textFolderName);
                            textFolderID = textDriveId.getResourceId();
                            textWasSelected = true;
                            if (isDeviceOnline()) {
                                new SearchTask(Select_Folders.this, false, true).execute();

                            } else {
                                new AlertDialog.Builder(getApplicationContext())
                                        .setMessage("No network connection available.").show();
                            }
                        }
                    });


        }
        String delay = settings.getString(DELAY_TAG, "");
        slideShowDelay.setText(delay);
        toggle.setChecked(settings.getString(START_ON_BOOT, "").equals("true"));
        final Intent intent = new Intent(this, FullscreenSlideshow.class);
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                startActivity(intent);
            }
        };
        Timer timer = new Timer();
        timer.schedule(tt, 5000);
    }

    private void refreshResults() {
        if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        }
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
        if (!mResolvingError) {
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
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("accountName", email).apply();
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
        final SharedPreferences.Editor editor = settings.edit();
        if (isImageCase) {
            editor.putString(PICS_DRIVEID, driveId.encodeToString()).apply();
        }
        if (isTextCase) {
            editor.putString(TEXT_DRIVEID, driveId.encodeToString()).apply();
        }


        final DriveFolder driveFolder = driveId.asDriveFolder();
        driveFolder.getMetadata(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveResource.MetadataResult>() {
                    @Override
                    public void onResult(DriveResource.MetadataResult metadataResult) {


                        if (isImageCase) {
                            picturesFolderName = metadataResult.getMetadata().getTitle();
                            picturesFolderID = driveId.getResourceId();
                            editor.putString(PICTURES_FOLDER_TAG, picturesFolderID).apply();
                            editor.putString(PICS_FOLDER_NAME_TAG, picturesFolderName).apply();
                            pictureSelectionText.setText(picturesFolderName);
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
                            textFolderName = metadataResult.getMetadata().getTitle();
                            textFolderID = driveId.getResourceId();
                            editor.putString(TEXT_FOLDER_TAG, textFolderID).apply();
                            editor.putString(TEXT_FOLDER_NAME_TAG, textFolderName).apply();
                            textSelectionText.setText(textFolderName);
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
        reconnectToGoogleApi();

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

    private void reconnectToGoogleApi() {
        int x = 0;
        while (x++ <= MAXIMUM_ATTEMPTS_TO_CONNECT) {
            if (!mGoogleApiClient.isConnecting() &&
                    !mGoogleApiClient.isConnected()) {
                mGoogleApiClient.connect();
            }
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
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PICS_FOLDER_NAME_TAG, "").apply();
        editor.putString(PICTURES_FOLDER_TAG, "").apply();
    }

    public void onClearTextClicked(View view) {
        textWasSelected = false;
        textSelectionText.setText("None Selected");
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(TEXT_FOLDER_NAME_TAG, "").apply();
        editor.putString(TEXT_FOLDER_TAG, "").apply();
    }

    public void onPlaySlideshowClicked(View view) {
        String delay = slideShowDelay.getText().toString();
        if (delay.isEmpty() || delay.equals("0")) {
            new AlertDialog.Builder(this)
                    .setMessage("Please enter a number greater than 0.").show();
            return;
        }
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(DELAY_TAG, delay).apply();
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