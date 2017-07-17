# HttpFileDownload

# Table of Contents
1. [Summary](#summary)
2. [How to Get HttpFileDownload](#how-to-get-httpfiledownload)
 1. [Single File](#single-file)
 2. [Android Project](#android-project)
3. [How to Use](#how-to-use)
 1. [Downloading a Single File](#downloading-a-single-file)
 2. [Downloading Multiple Files](#downloading-multiple-files)

## Summary
This is a complete project showing the use of the HttpFileDownload object. The purpose of this object is to simplify downloading file(s) over http and saving them to disk. Important points:
  * All the work is done by the object HttpFileDownload.
  * All downloads are perfomed using the [okhttp](http://square.github.io/okhttp/ "okhttpd") library.

## How to get HttpFileDownload
If you just want to use HttpFileDownload in your project, you can get the single file and add it to your project. If you want to play around with it first, you can download the Android project.

### Single File
You can download the source file from git and add it to your project using:
  * https://github.com/maxpower-ndrd/HttpFileDownload/blob/master/app/src/main/java/com/mpndrd/httpfiledownload/HttpFileDownloader.java

### Android Project
You can downlaod the project form GitHub using any of the following methods.
  * clone the repo, use the following command.
```
git clone https://github.com/maxpower-ndrd/HttpFileDownload.git
```
  * use the [Download Zip](https://github.com/maxpower-ndrd/HttpFileDownload/archive/master.zip) link from the project's [main page](https://github.com/maxpower-ndrd/HttpFileDownload)

## How to Use
### Downloading a Single File
the following code shows you how to download a single file.

```
try {
    // this is the url for the file you want to download
    URL url = new URL("https://www.google.com/images/nav_logo225.png");
    // this is the local path and filename on your device where the file should be stored.
    String storageLocation = getFilesDir().getAbsolutePath() + "nav_logo225.png";

    HttpFileDownloader.DownloadFile(
            new HttpFileDownloader.DownloadStatusListener() {
                @Override
                public void OnDownloadDone() {
                    progressBar.setProgress(0);
                    Log.d("MainActivity", "done");
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
                            showError(exception);
                        }
                    });
                }

                @Override
                public void OnProgress(HttpFileDownloader.DownloadProgress downloadProgress) {
                    progressBar.setProgress(downloadProgress.currentFileProgress);
                }
            }, url, storageLocation);
} catch (Exception e) {
    e.printStackTrace();
}
```

### Downloading Multiple Files
The following code shows you how to download a list of files. For this you build a list of DownloadRequest objects and pass it to HttpFileDownlaod object.

The following code uses a listview to get urls in the form of http://server/path/file.ext and to generate a list of DownloadRequest objects. URL.getFile() returns the path/file.ext portion of the url, and replaces /'s with -'s turns it into path-file.txt format. This is an example, in your own implementation choose/generate filenames that make sense for you.

```
try {
    HttpFileDownloader.DownloadRequest[] requests = new HttpFileDownloader.DownloadRequest[listAdapter.getCount()];

    for (int i = 0; i < listAdapter.getCount(); i++) {
        URL url = new URL(listAdapter.getItem(i).toString());
        requests[i] = new HttpFileDownloader.DownloadRequest(url.toString(), 
            getFilesDir().getAbsolutePath() + url.getFile().replace('/', '-'));
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
```
