package com.mpndrd.httpfiledownload;

import android.os.AsyncTask;
import android.util.Log;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * Created by maxpower on 7/18/2015.
 * <p>
 * this class downloads a file from a given url and saves it to the given absolute location.
 * HttpFileDownloader just wraps the entire AsyncTask call into a single static method that takes
 * a notification interface to the user can provide callback methods. You can think of this as using
 * the wrapper and callback patterns.
 * </p>
 * <p>
 * the inner class:
 * <ul>
 * <li>extends AsyncTask for background work</li>
 * <li>uses the basic run method provided by okhttp tutorial for downloading data.</li>
 * <li>uses the code from the following recipe to add a network interceptor for progress reporting.<br/>
 * https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/com/squareup/okhttp/recipes/Progress.java</li>
 * </ul>
 * </p>
 * <p><strong>notes</strong>: to save a response to file use: responseBody.source().readAll(Okio.sink(file));</p>
 */
public class HttpFileDownloader {
    /**
     * this static methods allows you to download a file to disk
     *
     * @param callback    - this is the callback interface that allows listening for progress.
     * @param url         - this is the complete url for the file to download
     * @param storageFile - this is the absolute path where the file will be saved
     */
    public static void DownloadFile(DownloadStatusListener callback, URL url, String storageFile) {
        // you could pass the information many different ways. the url/storageFile combo could be passed
        // as key-value pari to the execute() method blah blah. this was the easiest way.
        new DownloadFileTask(callback).execute(new DownloadRequest(url.toString(), storageFile));
    }

    /**
     * this static methods allows you to download a file to disk
     *
     * @param callback        - this is the callback interface that allows listening for progress.
     * @param downloadRequest - the DownloadRequest object(s) representing the file(s) to be downloaded.
     */
    public static void DownloadFiles(DownloadStatusListener callback, DownloadRequest... downloadRequest) {
        // you could pass the information many different ways. the url/storageFile combo could be passed
        // as key-value pari to the execute() method blah blah. this was the easiest way.
        new DownloadFileTask(callback).execute(downloadRequest);
    }

    /**
     * this class is the asynctask that handles the network read on the background thread. it catches
     * all exceptions, saves them and pass them to onPostExecution() for reporting.
     * <p/>
     * I know doInBackground() takes a DownloadRequest... so we can pass a list of files to download
     * but for now, this just processes a single request.
     */
    private static class DownloadFileTask extends AsyncTask<DownloadRequest, DownloadProgress, ResponseBundle> {
        /**
         * okhttp client used for downloading files
         */
        private OkHttpClient client = new OkHttpClient();

        /**
         * this is the callback interface used to notify the user
         */
        private DownloadStatusListener callback = null;

        /**
         * this is the list of files to be downlaoded
         */
        private DownloadRequest[] downloadRequests = null;

        /**
         * this is the current file counter
         */
        private int currentFile = 0;  // this var keeps track of current file to be downloaded

        /**
         * constructor requiring a callback
         *
         * @param callback
         */
        public DownloadFileTask(DownloadStatusListener callback) {
            this.callback = callback;
        }

        /**
         * this method processes a single requests. it access class members when needed.
         */
        public ResponseBundle downloadCurrentFile() throws IOException {
            final DownloadRequest downloadRequest = downloadRequests[currentFile];

            Log.d("HttpFileDownloader", "downloadSingleFile - url: " + downloadRequest.URL);

            Request request = new Request.Builder()
                    .url(downloadRequest.URL)
                    .build();

            Response response = client.newCall(request).execute();
            Log.d("HttpFileDownloader", "response is: " + ((response == null) ? "null" : "not null"));

            // this is what saves the body to file
            response.body().source().readAll(Okio.sink(new File(downloadRequest.storageLocation)));

            // yes, response.code!=200 isn't an exception. it's an error and it's easier to re-use ResponseBundle
            if (response.isSuccessful())
                return new ResponseBundle(true, null);
            else
                return new ResponseBundle(false, new Exception("error code: " + response.code()));
        }

        protected ResponseBundle doInBackground(DownloadRequest... reqs) {
            try {
                Log.d("HttpFileDownloader", "doInBackground - reqs: " + (reqs == null ? "null" : reqs.length));

                if (reqs == null || reqs.length == 0)
                    return new ResponseBundle(false, new Exception("no work submitted"));

                downloadRequests = reqs;

                // this listener is for the ProgressResponseBody used in the networkInterceptor.
                // this interceptor is added once to the client, and uses class member variables for
                // status updates
                final ProgressListener listener = new ProgressListener() {
                    @Override
                    public void update(long bytesRead, long contentLength, boolean done) {
                        int prog = (int) (100f * bytesRead / contentLength);
                        publishProgress(new DownloadProgress(currentFile + 1, downloadRequests.length,
                                downloadRequests[currentFile].URL, prog, bytesRead, contentLength));
                    }
                };

                // create a networkInterceptor
                client.networkInterceptors().add(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Response originalResponse = chain.proceed(chain.request());
                        return originalResponse.newBuilder()
                                .body(new ProgressResponseBody(originalResponse.body(), listener))
                                .build();
                    }
                });

                Log.d("HttpFileDownloader", "kicking off requests.... ");
                ResponseBundle res = null;
                for (int i = 0; i < downloadRequests.length; i++) {
                    currentFile = i;
                    res = downloadCurrentFile();

                    if (res.Status == false && callback != null)
                        callback.OnError(downloadRequests[i].URL, res.Exception, res.Exception.getMessage());
                }

                return new ResponseBundle(true, null);
            } catch (IOException e) {
                Log.d("HttpFileDownloader", "IOException - error: " + e.getMessage());
                e.printStackTrace();
                return new ResponseBundle(false, e);
            } catch (Exception e) {
                Log.d("HttpFileDownloader", "Exception - error: " + e.getMessage());
                e.printStackTrace();
                return new ResponseBundle(false, e);
            }
        }

        protected void onProgressUpdate(DownloadProgress... progress) {
            // Log.d("HttpFileDownloader", "onProgressUpdate");
            if (callback != null)
                callback.OnProgress(progress[0]);
        }

        protected void onPostExecute(ResponseBundle result) {
            // parse the result
            Log.d("HttpFileDownloader", "onPostExecute. result: " + result.Status);

            // the callback wasn't provide, exit.
            if (callback == null) return;

            // could also check status==false
            if (result.Status == true)
                // make sure to try/catch your OnDownloadDone
                callback.OnDownloadDone();
            else
                callback.OnError("", result.Exception, "error retrieving data");
        }
    }

    /**
     * just a key value pair. sure Map.Entry<K, V> but, meh. also getters, setters, meh. It's a simple
     * object.
     */
    public static class DownloadRequest {
        /**
         * this is the complete URL for the file to download
         */
        public String URL = "";
        /**
         * this is the absoluate path for where to store the downloaded file
         */
        public String storageLocation = "";

        public DownloadRequest(String URL, String storageLocation) {
            this.URL = URL;
            this.storageLocation = storageLocation;
        }
    }

    /**
     * <p>this object represents the download progress being reported.
     * <ul>
     * <li>currentFile - this is index of the file being downloaded</li>
     * <li>totalFiles - this is the number of total files</li>
     * <li>currentFilename - this is the URL of the current file being downloaded</li>
     * <li>currentFileBytesRead - this the number of bytes read so far for the current file</li>
     * <li>currentFileTotalBytes - this is the total length (in bytes) of the current file as
     * reported by the server</li>
     * </ul>
     * </p>
     */
    public static class DownloadProgress {
        public int currentFile = 0;  // current file in list
        public int totalFiles = 0;   // total files in list
        public String currentFilename = "";
        public int currentFileProgress = 0;
        public long currentFileBytesRead = 0;
        public long currentFileTotalBytes = 0;

        public DownloadProgress(int currentFile, int totalFiles, String currentFilename,
                                int currentFileProgress, long currentFileBytesReady,
                                long currentFileTotalBytes) {
            this.currentFile = currentFile;
            this.totalFiles = totalFiles;
            this.currentFilename = currentFilename;
            this.currentFileProgress = currentFileProgress;
            this.currentFileBytesRead = currentFileBytesReady;
            this.currentFileTotalBytes = currentFileTotalBytes;
        }
    }

    /**
     * <p>this interface is passed to HttpFileDownloader, and provides the notification for
     * <ol type="a">
     * <li>completion</li>
     * <li>error(s)/exceptions</li>
     * <li>progress</li>
     * </ol>
     * </p>
     */
    public interface DownloadStatusListener {
        /**
         * this means all the files in the list have been processed and there were no exceptions
         * thrown in the processing logic. exceptions can still be thrown while downloading
         * individual files and those will be propagated to the user via OnError
         */
        public void OnDownloadDone();

        /**
         * this means there was an error while downloading the file represented by filename. this method
         * can be called for an individual file while processing a list of files. this means this method
         * can be called from the background thread. if you're going to access UI elements from this method
         * make sure to post the request to the main thrad. here's a sample code to do that.
         * <p/>
         * <code>
         * new Handler(getApplicationContext().getMainLooper()).post(new Runnable() {
         *
         * @param filename  - this is the file that was being downloaded. if the filename is blank the
         *                  exception was thrown in the main loop, and OnDownloadDone will not be called.
         * @param exception - this is the exception that was caught or generated by this class to represent the error.
         * @param error     - this a simple string message representing the error.
         * @Override public void run() {
         * errorHandler();  // this is your error handler
         * }
         * });
         * </code>getApplicationContext.getMainLooper().post(new Runnable())
         */
        public void OnError(String filename, Exception exception, String error);

        /**
         * this will called to give you the download progress as the file(s) is/are being downloaded.
         * .currentFile will tell you which file the progress is for out of a total of totalFiles.
         *
         * @param downloadProgress
         */
        public void OnProgress(DownloadProgress downloadProgress);
    }

    protected static class ResponseBundle {
        public Boolean Status = false;
        public Exception Exception = null;

        public ResponseBundle(Boolean status, Exception e) {
            this.Status = status;
            this.Exception = e;
        }
    }

    /**
     * this class is implemented per the following recipe. (almost copy)
     * https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/com/squareup/okhttp/recipes/Progress.java
     */
    private static class ProgressResponseBody extends ResponseBody {
        private final ResponseBody responseBody;
        private final ProgressListener progressListener;
        private BufferedSource bufferedSource;

        public ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener) {
            this.responseBody = responseBody;
            this.progressListener = progressListener;
        }

        @Override
        public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override
        public long contentLength() throws IOException {
            return responseBody.contentLength();
        }

        @Override
        public BufferedSource source() throws IOException {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override
                public long read(Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                    progressListener.update(totalBytesRead, responseBody.contentLength(), bytesRead == -1);
                    // Log.d("ProgressResponseBody", "totayBytesRead: " + totalBytesRead + ", length: " + responseBody.contentLength());
                    return bytesRead;
                }
            };
        }
    }

    /**
     * this interface is implemented per the following recipe.
     * https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/com/squareup/okhttp/recipes/Progress.java
     */
    interface ProgressListener {
        void update(long bytesRead, long contentLength, boolean done);
    }
}
