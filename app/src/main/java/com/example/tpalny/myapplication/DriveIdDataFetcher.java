package com.example.tpalny.myapplication;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.drive.Contents;
import com.google.android.gms.drive.DriveId;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Tom Palny on 13/12/2015.
 */

public class DriveIdDataFetcher implements DataFetcher<InputStream> {
    //private static final Logger LOG = LoggerFactory.getLogger(DriveIdDataFetcher.class);

    private final GoogleApiClient client;
    private final DriveId driveId;

    private boolean cancelled = false;

    private File file;
    //private Contents contents;

    public DriveIdDataFetcher(GoogleApiClient client, DriveId driveId) {
        this.client = client;
        this.driveId = driveId;
    }

    public String getId() {
        return driveId.encodeToString();
    }

    public InputStream loadData(Priority priority) {
        if (cancelled) return null;
        if (client == null) {
            //Log.i("No connected client received, giving custom error image");
            return null;
        }
        file = Select_Folders.imagesList.get(0);
        if (cancelled) return null;
        try {
            HttpResponse resp =
                    Select_Folders.mGOOSvc.getRequestFactory().buildGetRequest(new GenericUrl(file.getDownloadUrl()))
                            .execute();
            return resp.getContent();
        } catch (IOException e) {
            // An error occurred.
            e.printStackTrace();
            return null;
        }
    }


        /*contents = sync(file.openContents(client, DriveFile.MODE_READ_ONLY, null)).getContents();
        if (cancelled) return null;
        return contents.getInputStream();
    }*/

    public void cancel() {
        /*cancelled = true;
        if (contents != null) {
            file.discardContents(client, contents);
        }*/
    }

    public void cleanup() {

    }

    /*private static <T extends Result> void assertSuccess(T result) {
        if (!result.getStatus().isSuccess()) {
            throw new IllegalStateException(result.getStatus().toString());
        }
    }*/

    /*private static <T extends Result> T sync(PendingResult<T> pending) {
        T result = pending.await();
        assertSuccess(result);
        return result;
    }*/
}
