
package com.transcendcode.earful;

import java.io.File;
import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class DirListAdapter extends ArrayAdapter<String>
{
    public static final String TAG = "DirListAdapter";

    private Context c;
    private int id;
    private List<String> items;
    private EarfulApplication mAppState;

    public DirListAdapter(Context context, int textViewResourceId,
            List<String> objects)
    {
        super(context, textViewResourceId, objects);
        mAppState = ((EarfulApplication) context.getApplicationContext());
        c = context;
        id = textViewResourceId;
        items = objects;
    }

    @Override
    public int getCount()
    {
        return items.size();
    }

    @Override
    public String getItem(int i)
    {
        return items.get(i);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View v = convertView;
        if (v == null)
        {
            LayoutInflater vi = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
        }

        TextView t1;
        CheckBox checkBox;

        checkBox = (CheckBox) v.findViewById(R.id.noMediaCheckBox);

        final String file_str = "directory" + (position + 1);
        File noMedia = new File(items.get(position) + "/.nomedia");
        checkBox.setChecked(noMedia.exists());
        
        // If CheckBox is toggled, update the planet it is tagged with.  
        checkBox.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
            	// TODO: Clean up to use position instead prefs
                CheckBox cb = (CheckBox) v;
                String file = mAppState.prefs.getString(file_str, "");
                File noMedia = new File(file + "/.nomedia");
                try
                {
                    if (cb.isChecked())
                    {
                        if (C.Save)
                            Log.i(TAG, ".nomedia created at " + noMedia.toString());
                        noMedia.createNewFile();
                    }
                    else
                    {
                        if (C.Save)
                            Log.i(TAG, ".nomedia deleted at " + noMedia.toString());

                        noMedia.delete();
                    }
                    mAppState.StartMediaScanner(file);
                    
                } catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        final String o = items.get(position);

        if (o != null)
        {
            t1 = (TextView) v.findViewById(R.id.TextView01);

            if (t1 != null)
                t1.setText(o);

        }

        return v;
    }

}
