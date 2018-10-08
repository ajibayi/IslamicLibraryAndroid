package com.fekracomputers.islamiclibrary;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fekracomputers.islamiclibrary.appliation.IslamicLibraryApplication;
import com.fekracomputers.islamiclibrary.browsing.activity.BrowsingActivity;
import com.fekracomputers.islamiclibrary.databases.BooksInformationDbHelper;
import com.fekracomputers.islamiclibrary.databases.UserDataDBHelper;
import com.fekracomputers.islamiclibrary.download.model.DownloadFileConstants;
import com.fekracomputers.islamiclibrary.download.model.DownloadsConstants;
import com.fekracomputers.islamiclibrary.download.service.UnZipIntentService;
import com.fekracomputers.islamiclibrary.settings.SettingsActivity;
import com.fekracomputers.islamiclibrary.utility.StorageUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import timber.log.Timber;

import static com.fekracomputers.islamiclibrary.utility.StorageUtils.getIslamicLibraryBaseDirectory;

public class SplashActivity extends AppCompatActivity {
    private static final int WRITE_EXTERNAL_STORAGE_PERMESSION = 0;

    private static final long SPLASH_TIME_OUT = 300;
    private static final String ERROR_CHANNEL_ID = "error_channel";
    private static final String BOOKS_UPDATED_TO_V_4 = "booksUpdatedToV4";
    ProgressBar mProgressBar;
    @Nullable
    AlertDialog permissionsDialog;
    private TextView mTextView;
    private TextView mProgressValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        if (intent.getBooleanExtra(SettingsActivity.KEY_KILL_APP, false)) {
            finish();
            System.exit(0);
        }
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        ((IslamicLibraryApplication) getApplication()).refreshLocale(this, false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mProgressBar = findViewById(R.id.progressBar1);
        mTextView = findViewById(R.id.progressTextView);
        mProgressValue = findViewById(R.id.progressValueTextView);
        checkStorage();
    }


    private boolean canWriteSdcardAfterPermissions() {
        String location = getIslamicLibraryBaseDirectory(this);
        if (location != null) {
            try {
                if (new File(location).exists() || StorageUtils.makeIslamicLibraryShamelaDirectory(this)) {
                    File f = new File(location, "" + System.currentTimeMillis());
                    if (f.createNewFile()) {
                        f.delete();
                        return true;
                    }
                }
            } catch (Exception e) {
                Log.e("SplashActivity", e.getMessage(), e);
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == WRITE_EXTERNAL_STORAGE_PERMESSION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (!canWriteSdcardAfterPermissions()) {
                    Toast.makeText(this,
                            R.string.storage_permission_please_restart, Toast.LENGTH_LONG).show();
                }
                checkBookInformationDatabase();
            } else {
                final File fallbackFile = getExternalFilesDir(null);
                if (fallbackFile != null) {
                    StorageUtils.setAppCustomLocation(fallbackFile.getAbsolutePath(), this);
                    checkBookInformationDatabase();
                } else {
                    // set to null so we can try again next launch
                    StorageUtils.setAppCustomLocation(null, this);
                    finishSplashAndLaunchMainActivity();
                }
            }
        }
    }

    private void finishSplashAndLaunchMainActivity() {
        Intent intent = new Intent(this, BrowsingActivity.class);
        startActivity(intent);
        finish();
    }

    private String statusMessage(@NonNull Cursor c) {
        String msg;

        switch (c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
            case DownloadManager.STATUS_FAILED:
                msg = "DownloadInfo failed";
                break;

            case DownloadManager.STATUS_PAUSED:
                msg = "DownloadInfo paused";
                break;

            case DownloadManager.STATUS_PENDING:
                msg = "DownloadInfo pending";
                break;

            case DownloadManager.STATUS_RUNNING:
                msg = "DownloadInfo in progress";
                break;

            case DownloadManager.STATUS_SUCCESSFUL:
                msg = "DownloadInfo complete";
                break;

            default:
                msg = "DownloadInfo is nowhere in sight";
                break;
        }

        return (msg);
    }

    private void checkStorage() {
        final String path = StorageUtils.getAppCustomLocation(this);
        final File fallbackFile = getExternalFilesDir(null);

        boolean usesExternalFileDir = path != null && path.contains(BuildConfig.APPLICATION_ID);

        if ((path == null) || (usesExternalFileDir && (fallbackFile == null))) {
            // suggests that we're on m+ and getExternalFilesDir returned null at some point
            finishSplashAndLaunchMainActivity();
            return;
        }

        boolean needsPermission = !usesExternalFileDir || !path.equals(fallbackFile.getAbsolutePath());

        if (needsPermission && !StorageUtils.haveWriteExternalStoragePermission(this)) {
            // request permission
            //show permission rationale dialog
            permissionsDialog = new AlertDialog.Builder(this)
                    .setMessage(R.string.storage_permission_rationale)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        dialog.dismiss();
                        permissionsDialog = null;
                        requestExternalSdcardPermission();
                    })
                    .setNegativeButton(android.R.string.no, (dialog, which) -> {
                        dialog.dismiss();
                        permissionsDialog = null;

                        // fall back if we can
                        if (fallbackFile != null) {
                            StorageUtils.setAppCustomLocation(fallbackFile.getAbsolutePath(), SplashActivity.this);
                            checkBookInformationDatabase();
                        } else {
                            // set to null so we can try again next launch
                            StorageUtils.setAppCustomLocation(null, SplashActivity.this);
                            finishSplashAndLaunchMainActivity();
                        }
                    })
                    .create();
            permissionsDialog.show();

        } else {
            checkBookInformationDatabase();
        }

    }

    private void checkBookInformationDatabase() {
        checkUserDatabase();
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        if (BooksInformationDbHelper.databaseFileExists(SplashActivity.this)) {
            BooksInformationDbHelper instance = BooksInformationDbHelper.getInstance(this);
            if (!doesBookInfoContentNeedsUpdate(preferences) && (instance != null && instance.isValid())) {
                updateBooksIfNeeded();
            } else {
                BooksInformationDbHelper.deleteBookInformationFile();
                new getBooksInformationFromAssets(this).execute();
            }


        } else if (StorageUtils.isOldDirectoriesExists(this)) {
            handleOldDirectory();
        } else {
            new getBooksInformationFromAssets(this).execute();

        }
    }

    private void checkUserDatabase() {
        File oldUserDatabase = getDatabasePath(UserDataDBHelper.DATABASE_NAME);
        File newUserDatabasePath = new File(UserDataDBHelper.getDatabasePath(this));
        if (oldUserDatabase.exists() && !newUserDatabasePath.exists()) {
            try {
                StorageUtils.copyFile(oldUserDatabase, newUserDatabasePath);
                SQLiteDatabase.deleteDatabase(oldUserDatabase);
            } catch (IOException e) {
                Timber.e(e);
            }
        } else if (oldUserDatabase.exists() && newUserDatabasePath.exists()) {
            SQLiteDatabase.deleteDatabase(oldUserDatabase);
        }
    }

    private void updateBooksIfNeeded() {
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        if (!preferences.getBoolean(BOOKS_UPDATED_TO_V_4, false) || doesBookInfoContentNeedsUpdate(preferences)) {
            new UpdateBooksAsyncTask(this).execute();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(BOOKS_UPDATED_TO_V_4, true);
            editor.putInt(DownloadsConstants.PREF_BOOKS_INFO_CONTENT_VERSION,
                    DownloadsConstants.CURRENT_BOOKS_INFO_CONTENT_VERSION);
            editor.apply();
        } else {
            finishSplashAndLaunchMainActivity();
        }

    }

    private boolean doesBookInfoContentNeedsUpdate(SharedPreferences preferences) {
        return preferences.getInt(DownloadsConstants.PREF_BOOKS_INFO_CONTENT_VERSION, 0) <
                DownloadsConstants.CURRENT_BOOKS_INFO_CONTENT_VERSION;
    }

    private void handleOldDirectory() {
        Timber.d("isOldDirectoriesExists");
        new AsyncTask<Void, Integer, Void>() {
            @Override
            protected void onPreExecute() {
                String oldBooksPath = getIslamicLibraryBaseDirectory(SplashActivity.this);
                if (oldBooksPath == null) return;
                File oldPath = new File(oldBooksPath);
                if (oldPath.exists() && oldPath.isDirectory()) {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mTextView.setVisibility(View.VISIBLE);
                    mProgressBar.setIndeterminate(false);
                    mTextView.setText(R.string.info_changing_file_structure);
                    mProgressBar.setMax(oldPath.list().length);

                }
            }

            @Nullable
            @Override
            protected Void doInBackground(Void... params) {
                StorageUtils.makeIslamicLibraryShamelaDirectory(SplashActivity.this);
                String oldBooksPath = getIslamicLibraryBaseDirectory(SplashActivity.this);
                if (oldBooksPath == null) return null;
                File oldPath = new File(oldBooksPath);
                if (oldPath.exists() && oldPath.isDirectory()) {
                    String[] files = oldPath.list();
                    for (int i = 0; i < files.length; i++) {
                        String book = files[i];
                        File from = new File(oldBooksPath + File.separator + book);
                        if (!from.isDirectory()) {
                            File to = new File(oldBooksPath +
                                    File.separator +
                                    DownloadFileConstants.SHAMELA_BOOKS_DIR +
                                    File.separator +
                                    book);
                            from.renameTo(to);
                        }
                        publishProgress(i);
                    }
                    return null;
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                mProgressBar.setProgress(values[0]);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                SplashActivity.this.finishSplashAndLaunchMainActivity();
            }
        }.execute();
    }

    private void requestExternalSdcardPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                WRITE_EXTERNAL_STORAGE_PERMESSION);
        StorageUtils.setSdcardPermissionsDialogPresented(this);
    }

    public interface DownloadProgressCallBack {
        void accept(int i);
    }

    public interface RefreshBooksProgressCallBack {
        void accept(int i);
    }

    private static class getBooksInformationFromAssets extends AsyncTask<Void, Integer, Boolean> {
        private static final double MAX_MANI_DB_SIZE = 4264929L;
        private double downloadSoFar = 0;
        private WeakReference<SplashActivity> activityReference;

        private getBooksInformationFromAssets(SplashActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            SplashActivity activity = activityReference.get();
            if (activity != null) {
                activity.mProgressBar.setVisibility(View.VISIBLE);
                activity.mTextView.setVisibility(View.VISIBLE);
                activity.mProgressBar.setIndeterminate(false);
                activity.mProgressBar.setMax(100);
                activity.mProgressBar.setProgress(0);
                activity.mTextView.setText(R.string.info_unzipping_book_information_database);
            }
        }

        @NonNull
        @Override
        protected Boolean doInBackground(Void... voids) {
            SplashActivity activity = activityReference.get();
            StorageUtils.makeIslamicLibraryShamelaDirectory(activity);
            if (activity != null) {
                AssetManager assetManager = activity.getAssets();
                InputStream in;
                try {
                    in = assetManager.open(DownloadFileConstants.COMPRESSED_ONLINE_DATABASE_NAME);
                    if (!UnZipIntentService.unzip(in,
                            StorageUtils.getIslamicLibraryShamelaBooksDir(activity),
                            this::publishProgress)) {
                        throw new IOException("unzip failed for main database");
                    }
                } catch (IOException e) {
                    Timber.e(e);
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            SplashActivity activity = activityReference.get();
            downloadSoFar += (values[0]);
            if (activity != null)
                activity.mProgressBar.setProgress((int) (downloadSoFar * 100f / MAX_MANI_DB_SIZE));

        }

        @Override
        protected void onPostExecute(Boolean success) {
            SplashActivity activity = activityReference.get();
            if (activity != null)
                if (success) {
                    activity.updateBooksIfNeeded();
                } else {
                    activity.finish();
                }
        }
    }

    private static class UpdateBooksAsyncTask extends AsyncTask<Void, Integer, Boolean> {
        private WeakReference<SplashActivity> activityReference;
        private BooksInformationDbHelper booksInformationDbHelper;
        private int numberOfStoredBooks;

        private UpdateBooksAsyncTask(SplashActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            SplashActivity activity = activityReference.get();
            booksInformationDbHelper = BooksInformationDbHelper.getInstance(activity);
            if (booksInformationDbHelper != null) {
                activity.mProgressBar.setVisibility(View.VISIBLE);
                activity.mTextView.setVisibility(View.VISIBLE);
                activity.mProgressValue.setVisibility(View.VISIBLE);
                activity.mProgressBar.setIndeterminate(false);
                numberOfStoredBooks = booksInformationDbHelper.getNumberOfStoredBooks(activity);
                activity.mProgressBar.setMax(numberOfStoredBooks);
                activity.mProgressBar.setProgress(0);
                activity.mTextView.setText(R.string.updating_books_please_wait);
                activity.mProgressValue.setText(activity.getString(R.string.updating_books_progress, 0, numberOfStoredBooks));

            }
        }

        @NonNull
        @Override
        protected Boolean doInBackground(Void... voids) {
            if (numberOfStoredBooks == 0) return true;
            SplashActivity activity = activityReference.get();
            booksInformationDbHelper.refreshBooksDbWithDirectory(activity, this::publishProgress);
            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            SplashActivity activity = activityReference.get();
            if (activity != null) {
                activity.mProgressBar.setProgress(values[0]);
                activity.mProgressValue
                        .setText(activity.getString(R.string.updating_books_progress,
                                values[0],
                                numberOfStoredBooks));
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            SplashActivity activity = activityReference.get();
            if (activity != null)
                if (success) {
                    activity.mProgressBar.setVisibility(View.GONE);
                    activity.mTextView.setVisibility(View.GONE);
                    activity.mProgressValue.setVisibility(View.GONE);
                    activity.finishSplashAndLaunchMainActivity();
                } else {
                    activity.finish();
                }
        }
    }


}
    

