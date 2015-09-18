package com.mpndrd.httpfiledownload;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.net.URL;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    EditText fileURL = null;
    TextView status = null;
    ProgressBar progressBar = null;
    ListView list = null;
    ArrayAdapter listAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUIElements();

        Log.d("MainActivity", "path: " + getFilesDir());
    }

    private void initUIElements() {
        fileURL = (EditText) findViewById(R.id.download_url);
        status = (TextView) findViewById(R.id.status);
        progressBar = (ProgressBar) findViewById(R.id.progbar);

        // mStorageLocation.setText(getFilesDir().getAbsolutePath() + "/file.mp3");

        ((Button) findViewById(R.id.download)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startFileDownload();
                    }
                }
        );

        list = (ListView) findViewById(R.id.list);

        listAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, new ArrayList());
        list.setAdapter(listAdapter);

        ((Button) findViewById(R.id.add)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listAdapter.add(fileURL.getText().toString());
                listAdapter.notifyDataSetChanged();
            }
        });
    }

    private void startFileDownload() {
        try {
            HttpFileDownloader.DownloadRequest[] requests = new HttpFileDownloader.DownloadRequest[listAdapter.getCount()];

            // the following code uses a listview to get urls in the form of http://server/path/file.ext
            // and to generate a list of DownloadRequest objects. URL.getFile() returns the path/file.ext
            // portion of the url, and replaces /'s with -'s turns it into path-file.txt format. this is
            // an example, in your own implementation choose/generate filenames that make sense for you.
            for (int i = 0; i < listAdapter.getCount(); i++) {
                URL url = new URL(listAdapter.getItem(i).toString());
                requests[i] = new HttpFileDownloader.DownloadRequest(url.toString(), getFilesDir().getAbsolutePath() + url.getFile().replace('/', '-'));
                Log.d("MainAcvitity", i + " - file: " + url.toString() + ", local file: " + requests[i].storageLocation);
            }

            HttpFileDownloader.DownloadFiles(
                    new HttpFileDownloader.DownloadStatusListener() {
                        @Override
                        public void OnDownloadDone() {
                            showStatus("done");
                        }

                        @Override
                        public void OnError(String filename, final Exception exception, String error) {
                            // OnError can be called from doInBackground of the AsyncTask object, so
                            // make sure you run your error reporting code (showError() in this case)
                            // from the main thread. That is what the following code does. In showError()
                            // below you can manipulate UI elements.
                            new Handler(getApplicationContext().getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    showStatus("error: " + exception.getMessage());
                                }
                            });
                        }

                        @Override
                        public void OnProgress(HttpFileDownloader.DownloadProgress downloadProgress) {
                            showDataOnScreen(downloadProgress);
                        }
                    }, requests);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDataOnScreen(HttpFileDownloader.DownloadProgress downloadProgress) {
        TextView mText1 = (TextView) findViewById(R.id.currentfile_value);
        TextView mText3 = (TextView) findViewById(R.id.currentfilename_value);
        TextView mText4 = (TextView) findViewById(R.id.bytesread_value);

        if (downloadProgress.currentFile == downloadProgress.totalFiles && downloadProgress.currentFileProgress == 100) {
            progressBar.setProgress(0);
            mText1.setText("");
            mText3.setText("");
            mText4.setText("");
        } else {
            progressBar.setProgress(downloadProgress.currentFileProgress);
            mText1.setText(downloadProgress.currentFile + " / " + downloadProgress.totalFiles);
            mText3.setText(downloadProgress.currentFilename);
            mText4.setText(downloadProgress.currentFileBytesRead + " / " + downloadProgress.currentFileTotalBytes);
        }
    }

    private void showStatus(String status) {
        try {
            Log.d("MainActivity", "status: " + status);
            this.status.append(status + "\n");
        } catch (Exception mEX) {
            Log.d("MainAcvitity", "error: " + mEX.getMessage());
        }
    }
}
