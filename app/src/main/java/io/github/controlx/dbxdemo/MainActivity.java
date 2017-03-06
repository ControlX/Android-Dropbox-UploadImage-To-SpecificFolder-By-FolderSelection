package io.github.controlx.dbxdemo;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DeletedMetadata;
import com.dropbox.core.v2.files.ListFolderContinueErrorException;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import io.github.controlx.dbxdemo.dropbox.DropboxClient;
import io.github.controlx.dbxdemo.dropbox.UploadTask;
import io.github.controlx.dbxdemo.dropbox.UserAccountTask;
import io.github.controlx.dbxdemo.utils.CircleTransform;
import io.github.controlx.dbxdemo.utils.URI_to_Path;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_WRITE = 1;
    private static final int IMAGE_REQUEST_CODE = 101;
    private String ACCESS_TOKEN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!tokenExists()) {
            //No token
            //Back to LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        }

        ACCESS_TOKEN = retrieveAccessToken();
        getUserAccount();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_WRITE);
                } else {
                    upload();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_WRITE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    upload();
                }
                break;

            default:
                break;
        }
    }

    protected void getUserAccount() {
        if (ACCESS_TOKEN == null) return;
        new UserAccountTask(DropboxClient.getClient(ACCESS_TOKEN), new UserAccountTask.TaskDelegate() {
            @Override
            public void onAccountReceived(FullAccount account) {
                //Print account's info
                Log.d("User", account.getEmail());
                Log.d("User", account.getName().getDisplayName());
                Log.d("User", account.getAccountType().name());
                updateUI(account);
            }

            @Override
            public void onError(Exception error) {
                Log.d("User", "Error receiving account details.");
            }
        }).execute();
    }

    private void updateUI(FullAccount account) {
        ImageView profile = (ImageView) findViewById(R.id.imageView);
        TextView name = (TextView) findViewById(R.id.tv_name_value);
        TextView email = (TextView) findViewById(R.id.tv_email_value);

        name.setText(account.getName().getDisplayName());
        email.setText(account.getEmail());
        Picasso.with(this)
                .load(account.getProfilePhotoUrl())
                .resize(400, 400)
                .transform(new CircleTransform())
                .into(profile);
    }

    private void upload() {
        if (ACCESS_TOKEN == null) return;
        //Select image to upload
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        if(intent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(Intent.createChooser(intent,
                    "Upload to Dropbox"), IMAGE_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        // Check which request we're responding to
        if (requestCode == IMAGE_REQUEST_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                File file = new File(URI_to_Path.getPath(getApplication(), data.getData()));
                FolderScanTask folderScanTask = new FolderScanTask(DropboxClient.getClient(ACCESS_TOKEN), file);
                folderScanTask.execute();

            }
        }
    }

    private boolean tokenExists() {
        SharedPreferences prefs = getSharedPreferences("com.example.valdio.dropboxintegration", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("access-token", null);
        return accessToken != null;
    }

    private String retrieveAccessToken() {
        //check if ACCESS_TOKEN is stored on previous app launches
        SharedPreferences prefs = getSharedPreferences("com.example.valdio.dropboxintegration", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("access-token", null);
        if (accessToken == null) {
            Log.d("AccessToken Status", "No token found");
            return null;
        } else {
            //accessToken already exists
            Log.d("AccessToken Status", "Token exists");
            return accessToken;
        }
    }

    class FolderScanTask extends AsyncTask {
        DbxClientV2 dbxClient;
        ListFolderResult result = null;
        CharSequence[] cs;
        File file;
        ArrayList<String> arrayList;

        public FolderScanTask(DbxClientV2 dbxClient, File file){
            this.dbxClient = dbxClient;
            this.file = file;
        }

        @Override
        protected Object doInBackground(Object[] params) {
            String path = "";
            //DbxClientV2 dbxClient = DropboxClient.getClient(ACCESS_TOKEN);
            TreeMap<String, Metadata> children = new TreeMap<String, Metadata>();

            try {
                try {
                    result = dbxClient.files()
                            .listFolder(path);
                } catch (ListFolderErrorException ex) {
                    ex.printStackTrace();
                }

                List<Metadata> list = result.getEntries();
                cs = new CharSequence[list.size()];
                arrayList = new ArrayList<>();
                arrayList.add("/");
                while (true) {
                    int i = 0;
                    for (Metadata md : result.getEntries()) {
                        if (md instanceof DeletedMetadata) {
                            children.remove(md.getPathLower());
                        } else {
                            String fileOrFolder = md.getPathLower();
                            children.put(fileOrFolder, md);
                            if(!fileOrFolder.contains("."))
                                arrayList.add(fileOrFolder);
                        }
                        i++;
                    }

                    if (!result.getHasMore()) break;

                    try {
                        result = dbxClient.files()
                                .listFolderContinue(result.getCursor());
                    } catch (ListFolderContinueErrorException ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (DbxException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            cs = arrayList.toArray(new CharSequence[arrayList.size()]);
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Select folder");
            builder.setIcon(android.R.drawable.sym_def_app_icon);
            builder.setCancelable(false);
            builder.setItems(cs, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    if (file != null) {
                        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressbar);
                        UploadTask task = new UploadTask(cs[item].toString(), DropboxClient.getClient(ACCESS_TOKEN), file, getApplicationContext(), progressBar);
                        task.execute();
                    }
                   dialog.dismiss();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }
}
