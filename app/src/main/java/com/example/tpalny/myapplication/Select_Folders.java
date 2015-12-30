package com.example.tpalny.myapplication;

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.OpenFileActivityBuilder;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class Select_Folders extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final int MAXIMUM_ATTEMPTS_TO_CONNECT = 4;
    private static final String PICS_FOLDER_NAME_TAG = "pics_folder_name";
    private static final String TEXT_FOLDER_NAME_TAG = "text_folder_name";
    protected static final String START_ON_BOOT = "start_on_boot";
    private static final String PICS_DRIVEID = "pics_driveID";
    private static final String TEXT_DRIVEID = "text_driveID";
    protected static final String USER_CANCELLED_SLIDESHOW = "user_cancelled_slideshow";
    protected static final String SCROLLING_SPEED = "scrolling_speed";
    protected static final String REFRESH_RATE = "refresh_rate";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private final String PICTURES_FOLDER_TAG = "pictures_folder";
    private final String TEXT_FOLDER_TAG = "text_folder";
    private final String DELAY_TAG = "delay";
    private static final int REQUEST_CODE_IMAGE_OPENER = 1;
    private static final int REQUEST_CODE_TEXT_OPENER = 2;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    private static final int REQUEST_CODE_RESOLUTION = 1003;
    private static final String TAG = "PickFolderWithOpener";

    private GoogleApiClient mGoogleApiClient;
    protected static GoogleAccountCredential mCredential;
    private IntentSender intentSender;
    private Button slideShowButton;
    private TextView pictureSelectionText;
    private TextView textSelectionText;
    private static String picturesFolderName = null;
    protected static String picturesFolderID = null;
    public static List<File> imagesList;
    public static List<File> textList;
    protected static EditText slideShowDelay;
    protected static EditText textScrollSpeed;
    protected static EditText textFileRefreshRate;
    private static SharedPreferences settings;
    protected static String textFolderID = null;
    private String textFolderName = null;
    private ToggleButton toggle;
    protected static boolean isSlideShowWithText = false;
    protected static Boolean noTextFoundMessageFirstTimeAppearance;
    protected static Timer loadPicsTimer;

    private static final String[] SCOPES = {DriveScopes.DRIVE_READONLY};


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
        textScrollSpeed = (EditText) findViewById(R.id.text_scroll_speed);
        textFileRefreshRate = (EditText) findViewById(R.id.text_file_refresh_rate);
        noTextFoundMessageFirstTimeAppearance = true;
        loadPicsTimer = new Timer();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(com.google.android.gms.drive.Drive.API)
                .addScope(com.google.android.gms.drive.Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        settings = getSharedPreferences("com.example.tpalny.myapplication_preferences", Context.MODE_PRIVATE);
        connectToGoogleService(this);

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

    protected static void connectToGoogleService(Context context) {
        mCredential = GoogleAccountCredential.usingOAuth2(
                context, Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));

    }


    @Override
    protected void onResume() {
        super.onResume();
        DisplayImage.currentPic = 0;
        loadPicsTimer.cancel();
        if (isGoogleAvailable()) {
            refreshResults();
        } else {
            new AlertDialog.Builder(Select_Folders.this)
                    .setMessage("Google Play Services required: " +
                            "after installing, close and relaunch this app.").show();
        }

        String chosenFolder = settings.getString(PICTURES_FOLDER_TAG, "");
        if (!chosenFolder.isEmpty()) {
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

        TimerTask slowNetworkTask = new TimerTask() {
            @Override
            public void run() {
                picsDriveFolder.getMetadata(mGoogleApiClient)
                        .setResultCallback(new ResultCallback<DriveResource.MetadataResult>() {
                            @Override
                            public void onResult(@NonNull DriveResource.MetadataResult metadataResult) {
                                picturesFolderName = settings.getString(PICS_FOLDER_NAME_TAG, "");
                                pictureSelectionText.setText(picturesFolderName);
                                picturesFolderID = picsDriveId.getResourceId();
                                slideShowButton.setEnabled(true);
                                slideShowButton.setAlpha(1);
                                if (isDeviceOnline()) {
                                    new SearchTask(Select_Folders.this, true, false).execute();

                                } else {
                                    Toast.makeText(Select_Folders.this,
                                            "No network connection available, attempting again in 10 seconds, please be patient...",
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        });

                textFolderID = settings.getString(TEXT_FOLDER_TAG, "");
                if (!textFolderID.isEmpty()) {
                    String driveIdDecode = settings.getString(TEXT_DRIVEID, "");
                    if (driveIdDecode.isEmpty()) {
                        return;
                    }
                    isSlideShowWithText = true;
                    final DriveId textDriveId = DriveId.decodeFromString(driveIdDecode);
                    final DriveFolder textDriveFolder = textDriveId.asDriveFolder();
                    textDriveFolder.getMetadata(mGoogleApiClient)
                            .setResultCallback(new ResultCallback<DriveResource.MetadataResult>() {
                                @Override
                                public void onResult(@NonNull DriveResource.MetadataResult metadataResult) {
                                    textFolderName = settings.getString(TEXT_FOLDER_NAME_TAG, "");
                                    textSelectionText.setText(textFolderName);
                                    textFolderID = textDriveId.getResourceId();
                                    if (isDeviceOnline()) {
                                        new SearchTask(Select_Folders.this, false, true).execute();

                                    } else {
                                        Toast.makeText(Select_Folders.this,
                                                "No network connection available, attempting again in 10 seconds, please be patient...",
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            });

                }
            }
        };
        Timer slowNetworkTimer = new Timer();

        Boolean userCancelledSlideshow = settings.getBoolean(USER_CANCELLED_SLIDESHOW, true);
        //this is the case of a restart after device rebooted during slideshow
        if (!userCancelledSlideshow) {
            slowNetworkTimer.schedule(slowNetworkTask, 11 * 1000);
        } else {
            slowNetworkTimer.schedule(slowNetworkTask, 500);
        }


        String delay = settings.getString(DELAY_TAG, "5");
        slideShowDelay.setText(delay);
        String scrollingSpeed = settings.getString(SCROLLING_SPEED, "5");
        textScrollSpeed.setText(scrollingSpeed);
        String refreshRate = settings.getString(REFRESH_RATE, "60");
        textFileRefreshRate.setText(refreshRate);
        toggle.setChecked(settings.getString(START_ON_BOOT, "").

                        equals("true")

        );

        if (!userCancelledSlideshow) {
            showToast();
            final Intent intent = new Intent(this, FullscreenSlideshow.class);
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    startActivity(intent);
                }
            };
            Timer timer = new Timer();
            timer.schedule(tt, 16 * 1000);
        }
    }

    private void showToast() {
        LinearLayout layout = new LinearLayout(this);
        layout.setBackgroundResource(R.color.myBackground);

        TextView tv = new TextView(this);
        // set the TextView properties like color, size etc

        tv.setTextColor(ContextCompat.getColor(this, R.color.myToastText));
        tv.setTextSize(30);

        tv.setGravity(Gravity.CENTER);

        // set the text you want to show in  Toast
        tv.setText("The Slideshow will start in a few seconds...");

        ImageView img = new ImageView(this);

        // give the drawble resource for the ImageView
        img.setImageResource(R.drawable.technion_logo);
        img.setScaleType(ImageView.ScaleType.CENTER);
        // add both the Views TextView and ImageView in layout
        layout.addView(img);
        layout.addView(tv);

        Toast toast = new Toast(this); //context is object of Context write "this" if you are an Activity
        // Set The layout as Toast View
        toast.setView(layout);

        // Position you toast here toast position is 50 dp from bottom you can give any integral value
        toast.setGravity(Gravity.BOTTOM, 0, 50);
        toast.setDuration(Toast.LENGTH_LONG);
        for (int x = 0; x < 4; x++) {
            toast.show();
        }
    }

    private void refreshResults() {
        if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        }
    }

    private void chooseAccount() {
        startActivityForResult(
                mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date. Will
     * launch an error dialog for the user to update Google Play Services if
     * possible.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private boolean isGoogleAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                api.isGooglePlayServicesAvailable(this);
        if (api.isUserResolvableError(connectionStatusCode)) {
            api.showErrorDialogFragment(this, connectionStatusCode, REQUEST_GOOGLE_PLAY_SERVICES);
            return false;
        } else if (connectionStatusCode != ConnectionResult.SUCCESS) {
            String str = GoogleApiAvailability.getInstance().getErrorString(connectionStatusCode);
            Toast.makeText(this, str, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        // Make sure the app is not already connected or attempting to connect

        if (!mGoogleApiClient.isConnecting() &&
                !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    isGoogleAvailable();
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
                    } else if (resultCode == RESULT_CANCELED) {
                        new AlertDialog.Builder(this).setMessage("Account unspecified").show();
                    }
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
                    public void onResult(@NonNull DriveResource.MetadataResult metadataResult) {


                        if (isImageCase) {
                            picturesFolderName = metadataResult.getMetadata().getTitle();
                            picturesFolderID = driveId.getResourceId();
                            editor.putString(PICTURES_FOLDER_TAG, picturesFolderID).apply();
                            editor.putString(PICS_FOLDER_NAME_TAG, picturesFolderName).apply();
                            pictureSelectionText.setText(picturesFolderName);
                            slideShowButton.setEnabled(true);
                            slideShowButton.setAlpha(1);
                            if (isDeviceOnline()) {
                                new SearchTask(Select_Folders.this, true, false).execute();

                            } else {
                                new AlertDialog.Builder(Select_Folders.this)
                                        .setMessage("No network connection available.").show();
                            }

                        } else if (isTextCase) {
                            textFolderName = metadataResult.getMetadata().getTitle();
                            textFolderID = driveId.getResourceId();
                            editor.putString(TEXT_FOLDER_TAG, textFolderID).apply();
                            editor.putString(TEXT_FOLDER_NAME_TAG, textFolderName).apply();
                            textSelectionText.setText(textFolderName);
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
        slideShowButton.setEnabled(false);
        slideShowButton.setAlpha(.5f);
        pictureSelectionText.setText("None Selected");
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PICS_FOLDER_NAME_TAG, "").apply();
        editor.putString(PICTURES_FOLDER_TAG, "").apply();
    }

    public void onClearTextClicked(View view) {
        isSlideShowWithText = false;
        textSelectionText.setText("None Selected");
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(TEXT_FOLDER_NAME_TAG, "").apply();
        editor.putString(TEXT_FOLDER_TAG, "").apply();
    }

    public void onPlaySlideshowClicked(View view) {
        String delay = slideShowDelay.getText().toString();
        slideShowButton.setAlpha(.7f);
        if (delay.isEmpty() || Integer.parseInt(delay) < 5) {
            Toast.makeText(this, "Delay should be at least 5 seconds", Toast.LENGTH_SHORT).show();
            slideShowButton.setAlpha(1);
            return;
        }
        String scrollingSpeed = textScrollSpeed.getText().toString();
        Integer scrollingSpeedInt = Integer.parseInt(scrollingSpeed);
        if (scrollingSpeedInt > 10 || scrollingSpeedInt < 1) {
            Toast.makeText(this, "Scrolling Speed between 1 and 10...", Toast.LENGTH_SHORT).show();
            slideShowButton.setAlpha(1);
            return;
        }
        String refreshRate = textFileRefreshRate.getText().toString();
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(DELAY_TAG, delay).apply();
        editor.putString(SCROLLING_SPEED, scrollingSpeed).apply();
        editor.putString(REFRESH_RATE, refreshRate).apply();
        editor.putBoolean(USER_CANCELLED_SLIDESHOW, false).apply();
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
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }
        // The failure has a resolution. Resolve it.
        // Called typically when the app is not yet authorized, and an
        // authorization
        // dialog is displayed to the user.
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

}