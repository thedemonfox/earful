
package com.transcendcode.earful;

import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener,
        MyInterface.DialogReturn
{

    public static final String TAG = "SettingsActivity";

    public static final int FOLDER_REQUEST_DROPBOX = SearchDirectories.FOLDER_REQUEST_CODE5 + 1;

    private Context mContext = null;

    public static final String KEY = "myCustomSharedPrefs";

    // Device model
    String phoneModel = android.os.Build.MODEL;

    // Android version
    String androidVersion = android.os.Build.VERSION.RELEASE;

    String appVersion = null;
    String appName = null;

    MyInterface myInterface;
    MyInterface.DialogReturn dialogReturn;
    EarfulApplication appState = null;

    boolean mDropLinked = false;

    @SuppressWarnings("deprecation")
		@Override
    protected void onResume()
    {
        super.onResume();
        if (C.D)
            Log.i(TAG, "onResume()");
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        if (appState.mDBApi.getSession().authenticationSuccessful())
        {
            try
            {
                // MANDATORY call to complete auth.
                // Sets the access token on the session
                appState.mDBApi.getSession().finishAuthentication();

                AccessTokenPair tokens = appState.mDBApi.getSession().getAccessTokenPair();

                // Provide your own storeKeys to persist the access token pair
                // A typical way to store tokens is using SharedPreferences
                appState.storeKeys(tokens.key, tokens.secret);
            } catch (IllegalStateException e)
            {
                if (C.D)
                    Log.i(TAG, "Error authenticating", e);
            }
        }

    }

    @SuppressWarnings("deprecation")
		@Override
    protected void onPause()
    {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        updateSummaries();
        updateDropbox();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    @SuppressWarnings("deprecation")
		public void updateDropbox()
    {
        Preference dropboxButton = (Preference) findPreference("dropboxAcountPref");

        if (appState.mDBApi.getSession().isLinked())
        {
            Log.i(TAG, "updateDropbox() LINKED");
            mDropLinked = true;
            try
            {
                dropboxButton.setSummary("Linked to account " + appState.mDBApi.accountInfo().displayName);
            } catch (DropboxException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            dropboxButton.setOnPreferenceClickListener(new OnPreferenceClickListener()
            {
                public boolean onPreferenceClick(Preference preference)
                {
                    Confirm("Do you want to unlink this application from DropBox?");

                    return true;
                }
            });

            Preference dropAutoSync = (Preference) findPreference("dropAutoSyncPref");
            dropAutoSync.setEnabled(true);

            Preference dropSyncTime = (Preference) findPreference("syncTimePref");
            dropSyncTime.setEnabled(true);

            Preference dropPowerSource = (Preference) findPreference("powerSourcePref");
            dropPowerSource.setEnabled(true);

            Preference dropNetConnect = (Preference) findPreference("internetConnectionPref");
            dropNetConnect.setEnabled(true);
        }
        else
        {
            if (C.D)
                Log.i(TAG, "updateDropbox() UNLINKED");
            dropboxButton.setSummary("Tap to link device to Dropbox");
            mDropLinked = false;
            dropboxButton.setOnPreferenceClickListener(new OnPreferenceClickListener()
            {
                public boolean onPreferenceClick(Preference preference)
                {
                    appState.mDBApi.getSession().startAuthentication(SettingsActivity.this);

                    return true;
                }
            });

            Preference dropAutoSync = (Preference) findPreference("dropAutoSyncPref");
            dropAutoSync.setEnabled(false);

            Preference dropSyncTime = (Preference) findPreference("syncTimePref");
            dropSyncTime.setEnabled(false);

            Preference dropPowerSource = (Preference) findPreference("powerSourcePref");
            dropPowerSource.setEnabled(false);

            Preference dropNetConnect = (Preference) findPreference("internetConnectionPref");
            dropNetConnect.setEnabled(false);
        }
    }

    @SuppressWarnings("deprecation")
		public void updateSummaries()
    {
        SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();

        Preference dropDirectoryPref = (Preference) findPreference("dropDirectoryPref");
        String dropDirectory = prefs.getString("dropDirectoryPref", "");
        if (!dropDirectory.equals(""))
        {
            dropDirectoryPref.setTitle(dropDirectory);
            findPreference("dropDirectoryHidePref").setEnabled(true);
        }
        else
        {
            dropDirectoryPref.setTitle("Local sync path");
            findPreference("dropDirectoryHidePref").setEnabled(false);
        }
/*
        Boolean sleepOn = prefs.getBoolean("sleepOn", false);
        if (sleepOn)
        {
            findPreference("sleepOnTime").setEnabled(true);
        }
        else
        {
            findPreference("sleepOnTime").setEnabled(false);
        }
*/        
    }

    @Override
    public void onDialogCompleted(boolean answer)
    {
        if (answer)
        {
            appState.logOut();
            updateDropbox();
        }
    }

    @Override
    protected void onDestroy()
    {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
		@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FOLDER_REQUEST_DROPBOX)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                String folder = data.getStringExtra("folder");

                SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();
                editor.putString("dropDirectoryPref", folder);
                editor.commit();

                if (C.D)
                    Log.i(TAG, "Saving " + folder + " as dropDirectoryPref");
            }
            else if (resultCode == Activity.RESULT_FIRST_USER)
            {
                SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();
                editor.putString("dropDirectoryPref", "");
                editor.commit();
            }
        }
    }

    public void Confirm(String msg)
    {
        AlertDialog dialog = new AlertDialog.Builder(mContext).create();

        dialog.setTitle("Confirmation");
        dialog.setMessage(msg);
        dialog.setCancelable(false);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int buttonId)
            {

                myInterface.getListener().onDialogCompleted(true);
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int buttonId)
            {
                myInterface.getListener().onDialogCompleted(false);
            }
        });
        dialog.setIcon(android.R.drawable.ic_dialog_alert);
        dialog.show();
    }

    @SuppressWarnings("deprecation")
		@Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        appState = ((EarfulApplication) getApplicationContext());

        mContext = this;

        myInterface = new MyInterface();
        myInterface.setListener(this);

        final PackageManager pm = getApplicationContext().getPackageManager();
        ApplicationInfo ai;
        try
        {
            ai = pm.getApplicationInfo(this.getPackageName(), 0);
        } catch (final NameNotFoundException e)
        {
            ai = null;
        }
        appName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");

        try
        {
            appVersion = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e)
        {
            Log.v("SETTINGS", e.getMessage());
        }

        Preference dropDirectoryPref = (Preference) findPreference("dropDirectoryPref");
        dropDirectoryPref.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                Intent dirActivity = new Intent(getBaseContext(), FileChooser.class);
                dirActivity.putExtra("allow_delete", true);
                if (C.D)
                    Log.i(TAG, "onPreferenceClick created for dropDirectoryPref");
                startActivityForResult(dirActivity, FOLDER_REQUEST_DROPBOX);

                return true;
            }
        });

        //Preference versionButton = (Preference) findPreference("version");
        //versionButton.setTitle(appName);
        //versionButton.setSummary(appVersion);

        Preference dirButton = (Preference) findPreference("directories");
        dirButton.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                Intent i = new Intent(getBaseContext(), SearchDirectories.class);
                //    Bundle extras = new Bundle();
                //    extras.putString(C.EXTRA_SOURCE, C.SOURCE_MAIN);
                //    playbackActivity.putExtras(extras);
                startActivity(i);

                return true;
            }
        });

        Preference resetButton = (Preference) findPreference("resetlibscan");
        resetButton.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {

                appState.clearLibrary();
                Toast.makeText(SettingsActivity.this, "Library Scan Cache Cleared", Toast.LENGTH_SHORT)
                        .show();
                return true;
            }
        });

        Preference resetdropButton = (Preference) findPreference("resetdropscan");
        resetdropButton.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {

                appState.clearDropboxCache();
                Toast.makeText(SettingsActivity.this, "Dropbox Cache Cleared", Toast.LENGTH_SHORT)
                        .show();
                return true;
            }
        });

        Preference resethelpButton = (Preference) findPreference("resethelp");
        resethelpButton.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();

                editor.putBoolean(PlayFragment.SHOW_ACTIVITY_OVERLAY, true);
                editor.putBoolean(SearchDirectories.SHOW_ACTIVITY_OVERLAY, true);
                editor.putBoolean(MainActivity.SHOW_ACTIVITY_OVERLAY, true);
                editor.commit();

                Toast.makeText(SettingsActivity.this, "All tutorial screens reset", Toast.LENGTH_SHORT)
                        .show();
                return true;
            }
        });
/*
        Preference whatsNewButton = (Preference) findPreference("whatsnew");
        whatsNewButton.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                ChangesPopupWindow dw = new ChangesPopupWindow(getListView());
                dw.showLikeQuickAction(0, 30);

                return true;
            }
        });

        Preference browserButton = (Preference) findPreference("website");
        browserButton.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                Intent browserIntent =
                        new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.transcendcode.com/"));
                startActivity(browserIntent);

                return true;
            }
        });

        Preference mailButton = (Preference) findPreference("contact");
        mailButton.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL, new String[] {
                        "earful@transcendcode.com"
                });
                i.putExtra(Intent.EXTRA_SUBJECT, "Customer Feedback");

                StringBuilder sb = new StringBuilder("");

                sb.append('\n');
                sb.append('\n');
                sb.append(appName + " " + appVersion + '\n');
                sb.append("Phone Model: " + phoneModel + '\n');
                sb.append("Android Version: " + androidVersion + '\n');

                i.putExtra(android.content.Intent.EXTRA_TEXT, sb.toString());
                try
                {
                    startActivity(Intent.createChooser(i, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ex)
                {
                    Toast.makeText(SettingsActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT)
                            .show();
                }

                return true;
            }
        });
*/
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        if (C.D)
            Log.i(TAG, "SettingsActivity: onSharedPreferenceChanged, key=" + key);

        updateSummaries();
        updateDropbox();

        if (key.equals("dropAutoSyncPref"))
        {
            boolean autosync = prefs.getBoolean(key, false);
            int time = Integer.parseInt(prefs.getString("syncTimePref", "0"));

            if (autosync && time > 0)
            {
                Alarm.setAlarm(this, time);
            }
            else
            {
                Alarm.cancelAlarm(this);
            }
        }

        // Change timing of the drop box sync service
        if (key.equals("syncTimePref"))
        {
            int time = Integer.parseInt(prefs.getString(key, "0"));

            if (C.D)
                Log.i(TAG, "Drop sync time changed to " + time + " mins");

            if (time > 0)
            {
                Alarm.setAlarm(this, time);
            }
            else
            {
                if (C.D)
                    Log.i(TAG, "Sync time was set to 0 so we aren't going start autosync");
                Alarm.cancelAlarm(this);
            }
        }
    }
}
