package com.example.tpalny.myapplication;

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

    private GoogleApiClient mGoogleApiClient;
    private IntentSender intentSender;
    private Button slideShowButton;
    //protected static final int REQUEST_CODE_RESOLUTION = 1;
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
    private static Drive mGOOSvc;
    //private static boolean mConnected;
    private static String email = null;
    private static final String[] SCOPES = {DriveScopes.DRIVE_READONLY};
    private static GoogleAccountCredential mCredential;
    private static String folderName = null;
    private static String folderID = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select__folders);
        slideShowButton = (Button) findViewById(R.id.Start_Slideshow_Button);
        slideShowButton.setEnabled(false);
        slideShowButton.setAlpha(.5f);
        pictureSelectionText = (TextView) findViewById(R.id.Selected_Image_Folder);
        textSelectionText = (TextView) findViewById(R.id.Selected_Text_Folder);

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

    }


    @Override
    protected void onResume() {
        super.onResume();
        //mConnected = false;
        if (isGooglePlayServicesAvailable()) {
            refreshResults();
        } else {
            new AlertDialog.Builder(getApplicationContext())
                    .setMessage("Google Play Services required: " +
                            "after installing, close and relaunch this app.").show();
        }
        /*new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... nadas) {
                try {
                    // GoogleAuthUtil.getToken(mAct, email, DriveScopes.DRIVE_FILE);   SO 30122755
                    mGOOSvc.files().get("root").setFields("title").execute();
                    mConnected = true;
                } catch (UserRecoverableAuthIOException uraIOEx) {  // standard authorization failure - user fixable
                    return uraIOEx;
                } catch (GoogleAuthIOException gaIOEx) {  // usually PackageName /SHA1 mismatch in DevConsole
                    return gaIOEx;
                } catch (IOException e) {   // '404 not found' in FILE scope, consider connected
                    if (e instanceof GoogleJsonResponseException) {
                        if (404 == ((GoogleJsonResponseException) e).getStatusCode())
                            mConnected = true;
                    }
                } catch (Exception e) {  // "the name must not be empty" indicates
                    Log.i("", "Something else went wrong in onResume()");        // UNREGISTERED / EMPTY account in 'setSelectedAccountName()' above
                }
                return null;
            }
        }.execute();*/


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

    private class SearchTask extends AsyncTask<Void, Void, List<String>> {

        private Exception mLastError = null;
        private String mFolderId = null;
        private Boolean mIsImage = false;
        private Boolean mIsText = false;

        public SearchTask(GoogleAccountCredential credential, String folderId, Boolean isImage, Boolean isText) {
            mGOOSvc = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                    .setApplicationName("Technion Slideshow App").build();
            mFolderId = folderId;
            mIsImage = isImage;
            mIsText = isText;
        }

        /**
         * Background task to call Drive API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of up to 10 file names and IDs.
         *
         * @return List of Strings describing files, or an empty list if no files
         * found.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            List<String> fileInfo = new ArrayList<>();
            FileList result = null;
            if (mFolderId != null) {
                if (mIsImage){
                    result = mGOOSvc.files().list().setQ("'" + mFolderId +
                            "' in parents and mimeType contains 'image/' and trashed = false")
                            .execute();
                }
                if(mIsText){
                    result = mGOOSvc.files().list().setQ("'" + mFolderId + "'" +
                            " in parents and mimeType = 'text/plain'" +
                            " and mimeType = 'application/msword'" +
                            " and mimeType = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'" +
                            " and trashed = false")
                            .execute();
                }

            }
            else{
                result = mGOOSvc.files().list().setQ("mimeType contains 'image/' and trashed = false")
                        .execute();
            }
            List<File> files = result.getItems();
            if (files != null) {
                for (File file : files) {
                    fileInfo.add(String.format("%s (%s)\n",
                            file.getTitle(), file.getId()));
                }
            }
            return fileInfo;
        }


        @Override
        protected void onPostExecute(List<String> output) {
            if (output == null || output.size() == 0) {
                new AlertDialog.Builder(getApplicationContext())
                        .setMessage("No results returned.").show();
            } else {
                output.add(0, "Data retrieved using the Drive API:");
                new AlertDialog.Builder(Select_Folders.this)
                        .setMessage(TextUtils.join("\n", output)).show();
            }
        }

        @Override
        protected void onCancelled() {
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            Select_Folders.REQUEST_AUTHORIZATION);
                } else {
                    new AlertDialog.Builder(Select_Folders.this)
                            .setMessage("The following error occurred:\n"
                                    + mLastError.getMessage()).show();
                }
            } else {
                new AlertDialog.Builder(getApplicationContext())
                        .setMessage("Request cancelled.").show();
            }
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
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
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
        mGoogleApiClient.disconnect();
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
                    email =
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
                    new AlertDialog.Builder(getApplicationContext())
                            .setMessage("Account unspecified.").show();
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode != RESULT_OK) {
                    chooseAccount();
                }
                break;
            default:
                final DriveId driveId = data.getParcelableExtra(
                        OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);

                final DriveFolder driveFolder = driveId.asDriveFolder();
                driveFolder.getMetadata(mGoogleApiClient)
                        .setResultCallback(new ResultCallback<DriveResource.MetadataResult>() {
                            @Override
                            public void onResult(DriveResource.MetadataResult metadataResult) {
                                folderName = metadataResult.getMetadata().getTitle();
                                folderID = driveId.getResourceId();

                                if (requestCode == REQUEST_CODE_IMAGE_OPENER) {
                                    pictureSelectionText.setText(folderName);
                                    slideShowButton.setEnabled(true);
                                    slideShowButton.setAlpha(1);
                                    picturesWereSelected = true;
                                    if (isDeviceOnline()) {
                                        new SearchTask(mCredential, folderID, true, false).execute();
                                    } else {
                                        new AlertDialog.Builder(getApplicationContext())
                                                .setMessage("No network connection available.").show();
                                    }

                                /*Query query = new Query.Builder()
                                        .addFilter(Filters.eq(SearchableField.MIME_TYPE, "image/jpeg"))
                                        .build();
                                driveFolder.queryChildren(mGoogleApiClient, query)
                                        .setResultCallback(metadataCallback);*/
                                } else if (requestCode == REQUEST_CODE_TEXT_OPENER) {
                                    textSelectionText.setText(folderName);
                                    textWasSelected = true;
                                    if (isDeviceOnline()) {
                                        new SearchTask(mCredential, folderID, false, true).execute();
                                    } else {
                                        new AlertDialog.Builder(getApplicationContext())
                                                .setMessage("No network connection available.").show();
                                    }
                                }


                            }
                        });
                break;

        }
    }

    /*private ResultCallback<MetadataBufferResult> metadataCallback = new ResultCallback<MetadataBufferResult>() {
        @Override
        public void onResult(MetadataBufferResult metadataBufferResult) {
            if (!metadataBufferResult.getStatus().isSuccess()) {
                Log.i("onResult", " Problem retrieving files");
            }
            //TODO: Get the file list and URLs
            MetadataBuffer mb = metadataBufferResult.getMetadataBuffer();


        }
    };*/


    public void onPicturesSelectClicked(View view) {
        if (!mGoogleApiClient.isConnecting() &&
                !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
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
        } else if (result.hasResolution()) {
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
        dialogFragment.show(getFragmentManager(), "errorDialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
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

    /************************************************************************************************
     * find file/folder in GOODrive
     *
     * @param prnId parent ID (optional), null searches full drive, "root" searches Drive root
     * @param titl  file/folder name (optional)
     * @param mime  file/folder mime type (optional)
     * @return arraylist of found objects
     */
    static ArrayList<ContentValues> search(String prnId, String titl, String mime) {
        ArrayList<ContentValues> gfs = new ArrayList<>();
        if (mGOOSvc != null) try {
            // add query conditions, build query
            String qryClause = "'me' in owners and ";
            if (prnId != null) qryClause += "'" + prnId + "' in parents and ";
            if (titl != null) qryClause += "title = '" + titl + "' and ";
            if (mime != null) qryClause += "mimeType = '" + mime + "' and ";
            qryClause = qryClause.substring(0, qryClause.length() - " and ".length());
            Drive.Files.List qry = mGOOSvc.files().list().setQ(qryClause)
                    .setFields("items(id,mimeType,labels/trashed,title),nextPageToken");
            String npTok = null;
            if (qry != null) do {
                FileList gLst = qry.execute();
                if (gLst != null) {
                    for (File gFl : gLst.getItems()) {
                        if (gFl.getLabels().getTrashed()) continue;
                        gfs.add(newCVs(gFl.getTitle(), gFl.getId(), gFl.getMimeType()));
                    }                                                                 //else UT.lg("failed " + gFl.getTitle());
                    npTok = gLst.getNextPageToken();
                    qry.setPageToken(npTok);
                }
            }
            while (npTok != null && npTok.length() > 0);                     //UT.lg("found " + vlss.size());
        } catch (Exception e) {
            Log.e("_X_", Log.getStackTraceString(e));
        }
        return gfs;
    }

    static ContentValues newCVs(String titl, String gdId, String mime) {
        ContentValues cv = new ContentValues();
        if (titl != null) cv.put("titl", titl);
        if (gdId != null) cv.put("gdid", gdId);
        if (mime != null) cv.put("mime", mime);
        return cv;
    }
}