package io.github.controlx.dbxdemo.dropbox;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Abhishek Verma on 3/6/2017.
 */

public class UploadTask extends AsyncTask {

    private DbxClientV2 dbxClient;
    private File file;
    private Context context;
    private ProgressBar bar;
    private int i = 0;
    private String path = null;
    public UploadTask(String path, DbxClientV2 dbxClient, File file, Context context, ProgressBar bar) {
        this.dbxClient = dbxClient;
        this.file = file;
        this.context = context;
        this.bar = bar;
        this.path = path;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        bar.setVisibility(View.VISIBLE);
    }

    @Override
    protected Object doInBackground(Object[] params) {
        try{
            if(path == null || path.isEmpty())
                path = "/";
            else if(!path.equals("/"))
                path = path + "/";

            // Upload to Dropbox
            InputStream inputStream = new FileInputStream(file);
            dbxClient.files().uploadBuilder(path + file.getName()) //Path in the user's Dropbox to save the file.
                    .withMode(WriteMode.OVERWRITE) //always overwrite existing file
                    .uploadAndFinish(inputStream);

            Log.d("Upload Status", "Success");
            publishProgress(100);
        } catch (DbxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        bar.setVisibility(View.GONE);
        Toast.makeText(context, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
    }
}
