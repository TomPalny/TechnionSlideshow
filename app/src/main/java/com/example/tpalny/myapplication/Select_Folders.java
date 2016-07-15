package com.example.tpalny.myapplication;

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
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
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static android.os.Environment.MEDIA_MOUNTED;
import static android.os.Environment.getExternalStorageState;


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
    protected static final String TEXT_REFRESH_RATE = "text_refresh_rate";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String MINIMUM_PIC_DURATION = "4";
    private static final String DELAY_START = "delay_start";
    private static final String PICS_REFRESH_RATE = "pics_refresh_rate";
    private final String PICTURES_FOLDER_TAG = "pictures_folder";
    private final String TEXT_FOLDER_TAG = "text_folder";
    private final String PIC_DURATION = "pic_duration";
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
    protected static EditText pictureFileRefreshRate;
    private static SharedPreferences settings;
    protected static String textFolderID = null;
    private String textFolderName = null;
    private ToggleButton toggle;
    protected static boolean isSlideShowWithText = false;
    protected static Boolean noTextFoundMessageFirstTimeAppearance;
    //protected static Boolean userCanceledSlideshow = true;

    private static final String[] SCOPES = {DriveScopes.DRIVE_READONLY};
    private EditText delayAfterBoot;
    protected static String root;
    protected static java.io.File myDir;
    protected static ProgressDialog mProgressDialog;
    private Timer timerAutoRestart;


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
        pictureFileRefreshRate = (EditText) findViewById(R.id.picture_file_refresh_rate);
        delayAfterBoot = (EditText) findViewById(R.id.delay_after_boot);
        noTextFoundMessageFirstTimeAppearance = true;
        root = getExternalFilesDir(null).toString();
        myDir = new java.io.File(root + "/saved_images");
        myDir.mkdirs();


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


    public static class DownloadTask extends AsyncTask<Void, Integer, Void> {
        private Drive mGOOSvc = SearchTask.mGOOSvc;
        private Context mContext;
        int mListSize;
        Boolean showProgress;
        Boolean exifFlag = false;
        //this is the number of times download is called before old pics in local storage are
        // checked against the cloud storage in order to delete obsolete local files
        static int deleteFilesCounter = 0;

        DownloadTask(Context context, int listSize, boolean showProgressFlag) {
            deleteFilesCounter++;
            mContext = context;
            mListSize = listSize;
            showProgress = showProgressFlag;
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mContext instanceof FullscreenSlideshow) {
                FullscreenSlideshow.pictureDisplayHandle.cancel(true);
                FullscreenSlideshow.picsFileRefreshHandle.cancel(true);
            }
            if (!getExternalStorageState().equals(MEDIA_MOUNTED)) {
                cancel(true);
            }
            if (showProgress) {
                mProgressDialog = new ProgressDialog(mContext);
                mProgressDialog.setMax(mListSize);
                mProgressDialog.setMessage("Downloading files...");
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setIndeterminate(false);
                try {
                    mProgressDialog.show();
                } catch (Exception e) {
                    Log.e("Progress Dialog", "can't show progress dialog!");
                }

            }

            if (deleteFilesCounter == 2) {
                deleteFilesCounter = 0;
                java.io.File[] files = myDir.listFiles();
                for (int i = 0; i < files.length; i++) {
                    boolean obsolete = true;
                    for (int j = 0; j < imagesList.size(); j++) {
                        if (files[i].getName().equals(imagesList.get(j).getId() + ".jpg")) {
                            obsolete = false;
                        }
                    }
                    if (obsolete) {
                        files[i].delete();
                        files = myDir.listFiles();
                        i = 0;
                    }
                }
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            HttpResponse resp = null;
            InputStream is = null;
            Integer count = 1;

            for (int currentPic = 0; currentPic < mListSize; currentPic++) {
                try {
                    GenericUrl url = new GenericUrl(imagesList
                            .get(currentPic).getDownloadUrl());
                    String fname = imagesList.get(currentPic).getId() + ".jpg";
                    java.io.File file = new java.io.File(myDir, fname);
                    if (file.exists()) continue;


                    resp = mGOOSvc.getRequestFactory().buildGetRequest(url).execute();
                    is = resp.getContent();

                    Bitmap bm = BitmapFactory.decodeStream(is);

                    FileOutputStream out = new FileOutputStream(file);
                    try {

                        bm.compress(Bitmap.CompressFormat.JPEG, 95, out);
                    } catch (NullPointerException e) {
                        Log.e("NullPointerException", "Download cannot complete, try again");
                        exifFlag = true;
                    }
                    if (showProgress)
                        publishProgress(count++);
                    out.flush();
                    out.close();


                } catch (IOException e) {
                    // An error occurred.
                    e.printStackTrace();
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (resp != null) {
                        try {
                            resp.disconnect();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return null;

        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onCancelled() {
            Log.e("MOUNT ERROR", "Check External Storage!");
            super.onCancelled();

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(exifFlag && showProgress){
                mProgressDialog.dismiss();
                new AlertDialog.Builder(mContext)
                        .setMessage("Please pay close attention to the following information:\n" +
                                "Some pictures in the selected folder contain EXIF metadata, \n" +
                                "and this device, running an older version of Android, \n" +
                                "cannot decode those pictures, so the slideshow cannot start.\n" +
                                "Please clear ALL EXIF data from pictures and try again.\n" +
                                "Here is one way to do so:\n" +
                                "Go to www.ImageMagick.org and download ImageMagick,\n" +
                                "extract it and open  CMD from within the folder,\n" +
                                "then run the following command:\n\n" +
                                "mogrify -auto-orient -strip <folder of pictures>\\*.jpg\n\n" +
                                "Afterwards, upload them again to Google Drive.\n" +
                                "This command OVERWRITES the files, auto-rotates them\n" +
                                "and then removes the EXIF metadata.\n" +
                                "If you don't want to overwrite, use:\n\n" +
                                "convert -auto-orient -strip <original folder of pictures>\\*.jpg \n" +
                                "<target folder of pictures>\\%04d.jpg\n\n" +
                                "or consult the documentation of \"convert\" or \"mogrify\" commands\n" +
                                "at http://www.imagemagick.org/script/command-line-tools.php").show();
                return;
            }
            if (showProgress)
                mProgressDialog.dismiss();
            if (mContext instanceof FullscreenSlideshow) {

                Runnable imageDisplay = new Runnable() {
                    @Override
                    public void run() {
                        new DisplayImage(mContext).execute();
                    }
                };
                Runnable picturesRefresh = new Runnable() {
                    @Override
                    public void run() {
                        new SearchTask(mContext, true, false).execute();
                    }
                };
                Integer picsRefreshTime = Integer.parseInt(pictureFileRefreshRate.getText().toString());
                Integer delay = Integer.parseInt(slideShowDelay.getText().toString());
                //restart the pictures slideshow
                FullscreenSlideshow.pictureDisplayHandle = FullscreenSlideshow.imageScheduler
                        .scheduleAtFixedRate(imageDisplay, 0, delay, TimeUnit.SECONDS);
                //restart the picture file refresh timer
                FullscreenSlideshow.picsFileRefreshHandle = FullscreenSlideshow.picsRefreshScheduler
                        .scheduleAtFixedRate(picturesRefresh, picsRefreshTime,
                                picsRefreshTime, TimeUnit.MINUTES);
            }
            super.onPostExecute(aVoid);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        DisplayImage.currentPic = 0;
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

        picturesFolderID = settings.getString(PICTURES_FOLDER_TAG, "");
        textFolderID = settings.getString(TEXT_FOLDER_TAG, "");

        if (!textFolderID.equals("")) isSlideShowWithText = true;


        Boolean userCancelledSlideshow = settings.getBoolean(USER_CANCELLED_SLIDESHOW, true);
        //this is the case of a restart after device rebooted during slideshow
        String delayString = settings.getString(DELAY_START, "20");
        delayAfterBoot.setText(delayString);
        Integer delayInt = Integer.parseInt(delayString);
        /*if (!userCancelledSlideshow) {
            slowNetworkTimer.schedule(slowNetworkTask, delayInt * 1000);
        } else {
            slowNetworkTimer.schedule(slowNetworkTask, 0);
        }*/

        picturesFolderName = settings.getString(PICS_FOLDER_NAME_TAG, "");
        pictureSelectionText.setText(picturesFolderName);
        slideShowButton.setAlpha(1);
        slideShowButton.setEnabled(true);
        textFolderName = settings.getString(TEXT_FOLDER_NAME_TAG, "");
        textSelectionText.setText(textFolderName);
        String delay = settings.getString(PIC_DURATION, MINIMUM_PIC_DURATION);
        slideShowDelay.setText(delay);
        String scrollingSpeed = settings.getString(SCROLLING_SPEED, "5");
        textScrollSpeed.setText(scrollingSpeed);
        String picsRefreshRate = settings.getString(PICS_REFRESH_RATE, "60");
        pictureFileRefreshRate.setText(picsRefreshRate);
        String textRefreshRate = settings.getString(TEXT_REFRESH_RATE, "60");
        textFileRefreshRate.setText(textRefreshRate);
        toggle.setChecked(settings.getString(START_ON_BOOT, "").equals("true"));

        if (!userCancelledSlideshow) {
            showToast(Integer.parseInt(delayString));
            final Intent intent = new Intent(this, FullscreenSlideshow.class);
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    startActivity(intent);
                }
            };
            timerAutoRestart = new Timer();
            //delaying the start of the slideshow by the amount of seconds
            timerAutoRestart.schedule(tt, (delayInt * 1000 + 1000));
        }
    }

    private void showToast(Integer delay) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMax(delay);
        mProgressDialog.setMessage("The slideshow will begin in " + delay + " seconds");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.show();
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                timerAutoRestart.cancel();
                timerAutoRestart.purge();
            }
        });

        new CountDownTimer(delay * 1000, 10) {

            @Override
            public void onTick(long millisUntilFinished) {
                mProgressDialog.setProgress((int) (millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                mProgressDialog.dismiss();

            }
        }.start();


        /*LinearLayout layout = new LinearLayout(this);
        layout.setBackgroundResource(R.color.myBackground);

        TextView tv = new TextView(this);
        // set the TextView properties like color, size etc

        tv.setTextColor(ContextCompat.getColor(this, R.color.myToastText));
        tv.setTextSize(30);

        tv.setGravity(Gravity.CENTER);

        // set the text you want to show in  Toast
        tv.setText(String.format("The Slideshow will start in %s seconds...", delay));

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
        }*/
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
                                new SearchTask(Select_Folders.this, true, false)
                                        .execute();

                            } else {
                                new AlertDialog.Builder(Select_Folders.this)
                                        .setMessage("No network connection available. " +
                                                "Make sure you are connected to the internet and try again...").show();

                            }

                        } else if (isTextCase) {
                            textFolderName = metadataResult.getMetadata().getTitle();
                            textFolderID = driveId.getResourceId();
                            editor.putString(TEXT_FOLDER_TAG, textFolderID).apply();
                            editor.putString(TEXT_FOLDER_NAME_TAG, textFolderName).apply();
                            textSelectionText.setText(textFolderName);
                            if (isDeviceOnline()) {
                                new SearchTask(Select_Folders.this, false, true)
                                        .execute();

                            } else {
                                new AlertDialog.Builder(Select_Folders.this)
                                        .setMessage("No network connection available. " +
                                                "Make sure you are connected to the internet and try again...").show();
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
        java.io.File[] files = myDir.listFiles();
        while (files.length > 0) {
            files[0].delete();
            files = myDir.listFiles();
        }

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
        if (delay.isEmpty() || Integer.parseInt(delay) < Integer.parseInt(MINIMUM_PIC_DURATION)) {
            Toast.makeText(this, "Picture duration should be at least " + MINIMUM_PIC_DURATION + " seconds", Toast.LENGTH_SHORT).show();
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
        String picsRefreshRate = pictureFileRefreshRate.getText().toString();
        String textRefreshRate = textFileRefreshRate.getText().toString();
        String delayStart = delayAfterBoot.getText().toString();
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PIC_DURATION, delay).apply();
        editor.putString(SCROLLING_SPEED, scrollingSpeed).apply();
        editor.putString(PICS_REFRESH_RATE, picsRefreshRate).apply();
        editor.putString(TEXT_REFRESH_RATE, textRefreshRate).apply();
        editor.putString(DELAY_START, delayStart).apply();
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

    public void onSwitchAccountClicked(View view) {
        mGoogleApiClient.clearDefaultAccountAndReconnect();
        startActivityForResult(
                mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);

    }
}