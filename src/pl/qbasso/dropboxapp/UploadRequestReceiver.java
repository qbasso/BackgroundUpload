package pl.qbasso.dropboxapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

public class UploadRequestReceiver extends BroadcastReceiver {

	private DropboxAPI<AndroidAuthSession> mDBApi;
	private Context mContext;
	private volatile boolean mUploadEnabled = true;
	public static final String EXTRA_FILENAME = "extra_filename";
	public static final String ACTION_SCAN_FOR_UPLOAD = "pl.qbasso.action_scan_for_upload";
	public static final String ACTION_RETRY_UPLOAD = "pl.qbasso.action_retry_upload";

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		mContext = arg0;
		AppKeyPair appKeys = new AppKeyPair(Main.APP_KEY, Main.APP_SECRET);
		AndroidAuthSession session = new AndroidAuthSession(appKeys,
				Main.ACCESS_TYPE);
		mDBApi = new DropboxAPI<AndroidAuthSession>(session);
		AccessTokenPair accessTokenPair = getStoredToken();
		if (accessTokenPair != null) {
			mDBApi.getSession().setAccessTokenPair(getStoredToken());
			if (ACTION_SCAN_FOR_UPLOAD.equals(arg1.getAction())) {
				if (Environment.getExternalStorageState().equals(
						Environment.MEDIA_MOUNTED)) {
					List<String> files = Utils.exploreToMaxDepth(new File(
							Utils.DIR_PATH));
					for (String f : files) {
						if (!checkIfFileExistsInDropbox(f.substring(f
								.lastIndexOf('/') + 1)) && mUploadEnabled) {
							uploadFileToDropbox(f);
						}
					}
				}
			} else if (ACTION_RETRY_UPLOAD.equals(arg1.getAction())) {
				uploadFileToDropbox(arg1.getStringExtra(EXTRA_FILENAME));
			}
		}
	}

	private void uploadFileToDropbox(final String f) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				boolean retry = true;
				FileInputStream inputStream = null;
				String fileName = f.substring(f.lastIndexOf('/') + 1);
				try {
					File file = new File(f);
					inputStream = new FileInputStream(file);
					Entry newEntry = mDBApi.putFile(fileName, inputStream,
							file.length(), null, null);
					Log.i("DbExampleLog", "The uploaded file's rev is: "
							+ newEntry.rev);
					retry = false;
				} catch (DropboxUnlinkedException e) {
					mUploadEnabled = false;
					clearSharedPrefs();
					Intent i = new Intent(mContext, Main.class);
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
							| Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					mContext.startActivity(i);
					Log.e("DbExampleLog", "User has unlinked.");
				} catch (DropboxException e) {
					Log.e("DbExampleLog",
							"Something went wrong while uploading.");
				} catch (FileNotFoundException e) {
					Log.e("DbExampleLog", "File not found.");
				} finally {
					if (inputStream != null) {
						try {
							inputStream.close();
						} catch (IOException e) {

						}
					}
				}
				if (retry) {
					Intent i = new Intent(ACTION_RETRY_UPLOAD);
					i.putExtra(EXTRA_FILENAME, f);
					mContext.sendBroadcast(i);
				}
			}
		}).start();

	}

	private AccessTokenPair getStoredToken() {
		AccessTokenPair result = null;
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		String appKey = pref.getString("app_key", "");
		String appSecret = pref.getString("app_secret", "");
		if (!appKey.equals("") && !appSecret.equals("")) {
			result = new AccessTokenPair(appKey, appSecret);
		}
		return result;
	}

	private void clearSharedPrefs() {
		SharedPreferences.Editor pref = PreferenceManager
				.getDefaultSharedPreferences(mContext).edit();
		pref.remove("app_key");
		pref.remove("app_secret");
	}

	private boolean checkIfFileExistsInDropbox(String fileName) {
		boolean result = false;
		try {
			Entry existingEntry = mDBApi.metadata(fileName, 1, null, false,
					null);
			if (existingEntry != null) {
				result = true;
			}
			Log.i("DbExampleLog", "The file's rev is now: " + existingEntry.rev);
		} catch (DropboxException e) {
			Log.e("DbExampleLog",
					"Something went wrong while getting metadata.");
		}
		return result;
	}

}
