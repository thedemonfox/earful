
package com.transcendcode.earful;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class FileChooser extends ListActivity
{

    private File currentDir;
    private FileArrayAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        currentDir = new File(Environment.getExternalStorageDirectory().getPath());
        fill(currentDir);

        setContentView(R.layout.filechooser);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null)
        {
            boolean allow_delete = extras.getBoolean("allow_delete", true);
            if (!allow_delete)
            {
                final Button removeButton = (Button) findViewById(R.id.removeButton);
                removeButton.setVisibility(Button.INVISIBLE);
            }
        }

        TextView curDirText = (TextView) findViewById(R.id.curDir);
        curDirText.setText(currentDir.toString());

        final Button selectButton = (Button) findViewById(R.id.SelectButton);
        selectButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                Intent intent = getIntent();

                intent.putExtra("folder", currentDir.toString());
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });

        final Button removeButton = (Button) findViewById(R.id.removeButton);
        removeButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                setResult(Activity.RESULT_FIRST_USER);
                finish();
            }
        });
    }

    private void fill(File f)
    {
        File[] dirs = f.listFiles();
        this.setTitle("Current Dir: " + f.getName());
        List<Option> dir = new ArrayList<Option>();
        List<Option> fls = new ArrayList<Option>();
        try
        {
            for (File ff : dirs)
            {
                if (ff.isDirectory())
                    dir.add(new Option(ff.getName(), "Folder", ff.getAbsolutePath()));
                //else
                //{
                //	fls.add(new Option(ff.getName(),"File Size: "+ff.length(),ff.getAbsolutePath()));
                //}
            }
        } catch (Exception e)
        {

        }
        Collections.sort(dir);
        Collections.sort(fls);
        dir.addAll(fls);
        if (!f.getName().equalsIgnoreCase("sdcard"))
            dir.add(0, new Option("..", "Parent Directory", f.getParent()));
        adapter = new FileArrayAdapter(FileChooser.this, R.layout.file_view, dir);
        this.setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        super.onListItemClick(l, v, position, id);
        Option o = adapter.getItem(position);
        if (o.getData().equalsIgnoreCase("folder") || o.getData().equalsIgnoreCase("parent directory"))
        {
            currentDir = new File(o.getPath());
            fill(currentDir);

            TextView curDirText = (TextView) findViewById(R.id.curDir);
            curDirText.setText(currentDir.toString());
        }
        //else
        //{
        //    onFileClick(o);
        //}
    }

}
