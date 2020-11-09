package org.mkonchady.sslcenglish.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import org.mkonchady.sslcenglish.Constants;
import org.mkonchady.sslcenglish.Log;

import org.mkonchady.sslcenglish.utils.UtilsFile;
import org.mkonchady.sslcenglish.utils.UtilsMisc;

import java.io.File;
import java.io.IOException;

public class MailActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void sendMail(Context context) {

        //Uri attachment = FileProvider.getUriForFile(this, "my_fileprovider", myFile);
        //emailIntent.putExtra(Intent.EXTRA_STREAM, attachment);

        Intent gmail = new Intent(Intent.ACTION_VIEW);
        gmail.setClassName("com.google.android.gm","com.google.android.gm.ComposeActivityGmail");
        gmail.putExtra(Intent.EXTRA_EMAIL, new String[] { "mandroid1960@gmail.com" });
        gmail.putExtra(Intent.EXTRA_SUBJECT, "enter something");
        gmail.putExtra(Intent.EXTRA_TEXT, "hi android jack!");
        gmail.setDataAndType(Uri.parse("mandroid1960@gmail.com"), "plain/text");
        startActivity(gmail);

    }

    public class BackgroundMail extends AsyncTask<Object, Integer, String> {
        Context context;
        final String noNet = "Net connection not available";
        final static String TAG = "UtilMail";

        @Override
        protected String doInBackground(Object... params) {
            context = (Context) params[0];
            if (!UtilsMisc.isNetworkAvailable(context))
                return noNet;
            //new ExportFile(context, Constants.EXPORT_ALL);
            return "Finished";
        }

        @Override
        protected void onPreExecute() {
            String status = "Started download ...";
        }

        @Override
        protected void onPostExecute(String retval) {
            if (retval.equals(noNet)) {
                return;
            }

            // check if the backup file exists
            File file = null;
            try {
                file = UtilsFile.getFile(context, Constants.BACKUP_FILE);
            } catch (IOException ie) {
                Log.e(TAG, "Could not open output file " + ie.getMessage(), 2);
            }
            if (file != null && (!file.exists() || !file.canRead())) return;

            // email an attachment with the backup file
            /*
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"mandroid1960@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Alumni List");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "See attached file ...");

            //Uri uri = Uri.fromFile(file);
            //emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivityForResult(Intent.createChooser(emailIntent, "Pick an Email provider"), Constants.EMAIL_CODE);
            //startActivityForResult(emailIntent, Constants.EMAIL_CODE);
            */



        }
    }
}