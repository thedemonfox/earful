
package com.transcendcode.earful;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class SearchDirectories extends Activity implements MyInterface.DialogReturn
{
    public static final String TAG = "SearchDirectories";
    static final String SHOW_ACTIVITY_OVERLAY = "SearchDirectories_Help";

    public static final int FOLDER_REQUEST_CODE1 = 1;
    public static final int FOLDER_REQUEST_CODE2 = FOLDER_REQUEST_CODE1 + 1;
    public static final int FOLDER_REQUEST_CODE3 = FOLDER_REQUEST_CODE2 + 1;
    public static final int FOLDER_REQUEST_CODE4 = FOLDER_REQUEST_CODE3 + 1;
    public static final int FOLDER_REQUEST_CODE5 = FOLDER_REQUEST_CODE4 + 1;

    public static final int FOLDER_REQUEST_CODE = 42;

    EarfulApplication mAppState;

    // Directory list
    private ListView lv = null;
    private static DirListAdapter mAdapter;
    List<String> mDirList = new ArrayList<String>();

    MyInterface myInterface;
    MyInterface.DialogReturn dialogReturn;

    private boolean mShowHelpOverlay = true;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_directories);

        getActionBar().setTitle("Search Directories");
        
        mAppState = (EarfulApplication) getApplicationContext();

        mShowHelpOverlay = mAppState.prefs.getBoolean(SHOW_ACTIVITY_OVERLAY, true);
        if (!mShowHelpOverlay)
        {
            LinearLayout overlay_view = (LinearLayout) findViewById(R.id.helpOverlay);
            overlay_view.setVisibility(View.INVISIBLE);
        }

        myInterface = new MyInterface();
        myInterface.setListener(this);

        lv = (ListView) findViewById(R.id.directoryList);

        LinearLayout header = (LinearLayout) getLayoutInflater().inflate(R.layout.dir_view_header, null);
        lv.setHeaderDividersEnabled(true);

        lv.setItemsCanFocus(false);
        lv.addHeaderView(header);

        //SharedPreferences.Editor editor = mAppState.prefs.edit();
        for (int i = FOLDER_REQUEST_CODE1; i <= FOLDER_REQUEST_CODE5; i++)
        {
            String str = "directory" + i;
            //final String nomedia_str = "directory" + i + "_nomedia";

            String dir_path = mAppState.prefs.getString(str, "");
            if (!dir_path.equals(""))
            {
                // Check if a nomedia exsists. If it does then set the preference token
                //File noMedia = new File(dir_path + "/.nomedia");
                //editor.putBoolean(nomedia_str, noMedia.exists());

                mDirList.add(dir_path);
            }
        }

        //editor.commit();

        mAdapter = new DirListAdapter(this, R.layout.dir_view, mDirList);

        lv.setAdapter(mAdapter);
        lv.setOnItemClickListener(new ListView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (C.D)
                    Log.i(TAG, "onItemClick() pos=" + position);
                Confirm("Remove this directory from scanning?", position - 1);
            }
        });

        final Button addButton = (Button) findViewById(R.id.addDirectory);
        addButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                Intent dirActivity = new Intent(getBaseContext(), FileChooser.class);
                dirActivity.putExtra("allow_delete", false);
                startActivityForResult(dirActivity, FOLDER_REQUEST_CODE);
            }
        });

        final ImageView overlay_img = (ImageView) findViewById(R.id.overlay_img);
        overlay_img.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                LinearLayout overlay_view = (LinearLayout) findViewById(R.id.helpOverlay);
                overlay_view.setVisibility(View.INVISIBLE);
                mShowHelpOverlay = false;

                // Save the help shown state
                SharedPreferences.Editor editor = mAppState.prefs.edit();
                editor.putBoolean(SHOW_ACTIVITY_OVERLAY, mShowHelpOverlay);
                editor.commit();
            }
        });

        final SquareButton helpButton = (SquareButton) findViewById(R.id.help);

        helpButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                mShowHelpOverlay = !mShowHelpOverlay;
                LinearLayout overlay_view = (LinearLayout) findViewById(R.id.helpOverlay);

                if (mShowHelpOverlay)
                {
                    overlay_view.setVisibility(View.VISIBLE);
                }
                else
                {
                    overlay_view.setVisibility(View.INVISIBLE);
                }

                // Save the help shown state
                SharedPreferences.Editor editor = mAppState.prefs.edit();
                editor.putBoolean(SHOW_ACTIVITY_OVERLAY, mShowHelpOverlay);
                editor.commit();
            }
        });
    }

    @Override
    public void onDialogCompleted(boolean answer)
    {
        if (answer)
        {
            SharedPreferences.Editor editor = mAppState.prefs.edit();

            mDirList.remove(myInterface.data);
            for (int i = 0; i < FOLDER_REQUEST_CODE5; i++)
            {
                String str = "directory" + (i + FOLDER_REQUEST_CODE1);
                final String nomedia_str = "directory" + (i + FOLDER_REQUEST_CODE1) + "_nomedia";

                if (i < mDirList.size())
                {
                    // Check if a nomedia exsists. If it does then set the preference token
                    File noMedia = new File(str + "/.nomedia");
                    editor.putBoolean(nomedia_str, noMedia.exists());

                    editor.putString(str, mDirList.get(i));
                }
                else
                {
                    editor.remove(nomedia_str);
                    editor.remove(str);
                }
            }
            editor.commit();
            mAdapter.notifyDataSetChanged();
        }
    }

    public void Confirm(String msg, int dir)
    {
        AlertDialog dialog = new AlertDialog.Builder(this).create();

        myInterface.data = dir;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_search_directories, menu);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FOLDER_REQUEST_CODE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                String folder = data.getStringExtra("folder");

                // Get the dropbox sync directory to check against
                String drop_dir = mAppState.prefs.getString("dropDirectoryPref", "");

                if (mDirList.contains(folder) || mDirList.contains(drop_dir))
                {
                    Toast.makeText(this, "Directory not added. It is a duplicate.", Toast.LENGTH_LONG)
                            .show();
                }
                else
                {
                    mDirList.add(folder);
                    mAdapter.notifyDataSetChanged();

                    SharedPreferences.Editor editor = mAppState.prefs.edit();

                    for (int i = 0; i < mDirList.size(); i++)
                    {
                        String str = "directory" + (i + FOLDER_REQUEST_CODE1);
                        editor.putString(str, mDirList.get(i));
                    }
                    editor.commit();
                }
            }
        }
    }
}
