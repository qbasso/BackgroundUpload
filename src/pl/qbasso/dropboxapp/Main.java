package pl.qbasso.dropboxapp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

public class Main extends Activity {

	public final static String APP_KEY = "7zixduqxgv95fqk";
	public final static String APP_SECRET = "hp8p1fo64cvoqdd";
	public final static AccessType ACCESS_TYPE = AccessType.APP_FOLDER;

	private DropboxAPI<AndroidAuthSession> mDBApi;
	private SharedPreferences mPref;
	private AlarmManager mAlarmManager;
	private PendingIntent mAlarmPendingIntent;

	@TargetApi(11)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Intent i = new Intent(UploadRequestReceiver.ACTION_SCAN_FOR_UPLOAD);
		mAlarmPendingIntent = PendingIntent.getBroadcast(this, 0, i,
				PendingIntent.FLAG_UPDATE_CURRENT);
		mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		mAlarmManager.cancel(mAlarmPendingIntent);
		mPref = PreferenceManager.getDefaultSharedPreferences(this);
		if (!mPref.contains("app_key") && !mPref.contains("app_secret")) {
			initDropboxSession();
		} else {
			scheduleAlarms();
			moveTaskToBack(false);
		}
	}

	private void initDropboxSession() {
		AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
		AndroidAuthSession session = new AndroidAuthSession(appKeys,
				ACCESS_TYPE);
		mDBApi = new DropboxAPI<AndroidAuthSession>(session);
		mDBApi.getSession().startAuthentication(Main.this);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if (mDBApi!=null) {
			if (mDBApi.getSession().authenticationSuccessful()) {
				try {
					mDBApi.getSession().finishAuthentication();
					AccessTokenPair tokens = mDBApi.getSession()
							.getAccessTokenPair();
					storeKeys(tokens.key, tokens.secret);
					scheduleAlarms();
					moveTaskToBack(false);
				} catch (IllegalStateException e) {
					Log.i("DbAuthLog", "Error authenticating", e);
				}
			} 
		}
	}

	private void scheduleAlarms() {
		if (Utils.DEBUG_FLAG) {
			mAlarmManager.set(AlarmManager.RTC_WAKEUP,
					System.currentTimeMillis(), mAlarmPendingIntent);
		} else {
			mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
					System.currentTimeMillis(), Utils.SCAN_INTERVAL,
					mAlarmPendingIntent);
		}
	}

	private void storeKeys(String key, String secret) {
		if (mPref == null) {
			mPref = PreferenceManager.getDefaultSharedPreferences(this);
		}
		SharedPreferences.Editor pref = mPref.edit();
		pref.putString("app_key", key);
		pref.putString("app_secret", secret);
		pref.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
