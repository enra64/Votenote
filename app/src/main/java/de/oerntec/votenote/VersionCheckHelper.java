package de.oerntec.votenote;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class VersionCheckHelper {
    private static Activity mResultCall;

    public static void checkVersion(Activity c, boolean stealth) {
        VersionCheckTask task = new VersionCheckTask(c, stealth);
        mResultCall = c;
        task.execute("");
    }

    public static void checkVersion(Activity c) {
        checkVersion(c, false);
    }

    public static void checkVersionStealth(Activity c) {
        checkVersion(c, true);
    }

    private static void onResult(String result) {
        if (mResultCall instanceof SettingsActivity)
            ((SettingsActivity) mResultCall).onVersionResult(result);
        if (mResultCall instanceof MainActivity)
            ((MainActivity) mResultCall).onVersionResult(result);

        mResultCall = null;
    }

    private static class VersionCheckTask extends AsyncTask<String, Void, Void> {
        private final HttpClient mClient = new DefaultHttpClient();
        private String mContent;
        private String mError = null;
        private Context mContext;
        private ProgressDialog mDialog = null;

        public VersionCheckTask(Context context, boolean isStealth) {
            mContext = context;
            if (!isStealth)
                mDialog = new ProgressDialog(mContext);
        }

        protected void onPreExecute() {
            //UI Element
            if (mDialog != null) {
                mDialog.setMessage(mContext.getString(R.string.version_check_message));
                mDialog.show();
            }
        }

        // Call after onPreExecute method
        protected Void doInBackground(String... urls) {
            try {
                // Server url call by GET method
                HttpGet httpget = new HttpGet("https://www.dropbox.com/s/ailm7k13b14ujul/latest.txt?dl=1");
                ResponseHandler<String> responseHandler = new MyResponseHandler();
                mContent = mClient.execute(httpget, responseHandler);
            } catch (IOException e) {
                mError = e.getMessage();
                cancel(true);
            }
            return null;
        }

        protected void onPostExecute(Void unused) {
            if (mDialog != null)
                mDialog.dismiss();
            mContext = null;
            onResult(mError == null ? mContent : mError);
        }
    }

    static class MyResponseHandler extends BasicResponseHandler {
        @Override
        public String handleResponse(HttpResponse response) throws IOException {
            InputStream in = response.getEntity().getContent();
            Scanner inScanner = new Scanner(in, "UTF-8").useDelimiter("\\A");
            String responseString = inScanner.next();
            in.close();
            inScanner.close();
            return responseString;
        }
    }
}
