
package com.transcendcode.earful;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * This class does most of the work of wrapping the {@link PopupWindow} so it's simpler to use.
 * 
 * @author qberticus
 */
public class ChangesPopupWindow
{
    public static final String TAG = "ChangesPopupWindow";
    protected final View anchor;
    private final PopupWindow window;
    private View root;
    private Drawable background = null;
    private final WindowManager windowManager;

    InputStream is1 = null;
    BufferedReader br = null;

    /**
     * Create a BetterPopupWindow
     * 
     * @param anchor the view that the BetterPopupWindow will be displaying 'from'
     */
    public ChangesPopupWindow(View anchor)
    {
        this.anchor = anchor;
        this.window = new PopupWindow(anchor.getContext());

        // when a touch even happens outside of the window
        // make the window go away
        this.window.setTouchInterceptor(new OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                //  if (event.getAction() == MotionEvent.ACTION_OUTSIDE)
                //  {
                ChangesPopupWindow.this.window.dismiss();
                return true;
                //  }
                //  return false;
            }
        });

        this.windowManager = (WindowManager) this.anchor.getContext().getSystemService(Context.WINDOW_SERVICE);
        onCreate();
    }

    /**
     * Anything you want to have happen when created. Probably should create a view and setup the event listeners on
     * child views.
     */
    protected void onCreate()
    {
        // inflate layout
        LayoutInflater inflater =
                (LayoutInflater) this.anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.popup_grid_layout, null);

        // set the inflated view as what we want to display
        this.setContentView(root);

        String htmlStr = null;
        TextView textView = (TextView) root.findViewById(R.id.changesText);
        is1 = anchor.getContext().getResources().openRawResource(R.raw.version);

        try
        {
            br = new BufferedReader(new InputStreamReader(is1, "UTF-8"));
            char buf[] = new char[4096];
            br.read(buf);
            htmlStr = new String(buf);
        } catch (UnsupportedEncodingException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        /*
        String htmlStr = new String();
        XmlResourceParser myxml = anchor.getContext().getResources().getXml(R.xml.changes);
        try
        {
            myxml.next();
            //        ext parse event
            int eventType = myxml.getEventType(); //Get current xml event i.e., START_DOCUMENT etc.

            String NodeValue;

            while (eventType != XmlPullParser.END_DOCUMENT) //Keep going until end of xml document
            {
                if (eventType == XmlPullParser.START_DOCUMENT)
                {
                    //Start of XML, can check this with myxml.getName() in Log, see if your xml has read successfully
                }
                else if (eventType == XmlPullParser.START_TAG)
                {
                    NodeValue = myxml.getName();//Start of a Node
                    if (NodeValue.equalsIgnoreCase("update"))
                    {
                        // use myxml.getAttributeValue(x); where x is the number
                        // of the attribute whose data you want to use for this node
                        if (C.D)
                            Log.i(TAG,
                                    "update " + myxml.getAttributeValue(1) + "   "
                                            + myxml.getAttributeValue(null, "details"));

                        htmlStr += "<h2>" + myxml.getAttributeValue(null, "version") + "</h2>" + "<p>"
                                + myxml.getAttributeValue(null, "details") + "</p>";

                    }

                    if (NodeValue.equalsIgnoreCase("SecondNodeNameType"))
                    {
                        // use myxml.getAttributeValue(x); where x is the number
                        // of the attribute whose data you want to use for this node

                    }
                    //etc for each node name
                }
                else if (eventType == XmlPullParser.END_TAG)
                {
                    //End of document
                }
                else if (eventType == XmlPullParser.TEXT)
                {
                    //Any XML text
                }

                eventType = myxml.next(); //Get next event from xml parser
            }
        } catch (XmlPullParserException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }//Get n
        */
        textView.setText(Html.fromHtml(htmlStr));

    }

    /**
     * In case there is stuff to do right before displaying.
     */
    protected void onShow()
    {
    }

    @SuppressWarnings("deprecation")
		private void preShow()
    {
        if (this.root == null)
        {
            throw new IllegalStateException("setContentView was not called with a view to display.");
        }
        onShow();

        if (this.background == null)
        {
            this.window.setBackgroundDrawable(new BitmapDrawable());
        }
        else
        {
            this.window.setBackgroundDrawable(this.background);
        }

        // if using PopupWindow#setBackgroundDrawable this is the only values of the width and hight that make it work
        // otherwise you need to set the background of the root viewgroup
        // and set the popupwindow background to an empty BitmapDrawable
        this.window.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        this.window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        this.window.setTouchable(true);
        this.window.setFocusable(true);
        this.window.setOutsideTouchable(true);

        this.window.setContentView(this.root);
    }

    public void setBackgroundDrawable(Drawable background)
    {
        this.background = background;
    }

    /**
     * Sets the content view. Probably should be called from {@link onCreate}
     * 
     * @param root the view the popup will display
     */
    public void setContentView(View root)
    {
        this.root = root;
        this.window.setContentView(root);
    }

    /**
     * Will inflate and set the view from a resource id
     * 
     * @param layoutResID
     */
    public void setContentView(int layoutResID)
    {
        LayoutInflater inflator =
                (LayoutInflater) this.anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.setContentView(inflator.inflate(layoutResID, null));
    }

    /**
     * If you want to do anything when {@link dismiss} is called
     * 
     * @param listener
     */
    public void setOnDismissListener(PopupWindow.OnDismissListener listener)
    {
        this.window.setOnDismissListener(listener);
    }

    /**
     * Displays like a popdown menu from the anchor view
     */
    public void showLikePopDownMenu()
    {
        this.showLikePopDownMenu(0, 0);
    }

    /**
     * Displays like a popdown menu from the anchor view.
     * 
     * @param xOffset offset in X direction
     * @param yOffset offset in Y direction
     */
    public void showLikePopDownMenu(int xOffset, int yOffset)
    {
        this.preShow();

        this.window.setAnimationStyle(R.style.Animations_PopDownMenu);

        this.window.showAsDropDown(this.anchor, xOffset, yOffset);
    }

    /**
     * Displays like a QuickAction from the anchor view.
     */
    public void showLikeQuickAction()
    {
        this.showLikeQuickAction(0, 0);
    }

    /**
     * Displays like a QuickAction from the anchor view.
     * 
     * @param xOffset offset in the X direction
     * @param yOffset offset in the Y direction
     */
    public void showLikeQuickAction(int xOffset, int yOffset)
    {
        this.preShow();

        this.window.setAnimationStyle(R.style.Animations_GrowFromBottom);

        int[] location = new int[2];
        this.anchor.getLocationOnScreen(location);

        Rect anchorRect =
                new Rect(location[0], location[1], location[0] + this.anchor.getWidth(), location[1]
                        + this.anchor.getHeight());

        this.root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        int rootWidth = this.root.getMeasuredWidth();
        int rootHeight = this.root.getMeasuredHeight();

        int screenWidth = this.windowManager.getDefaultDisplay().getWidth();
        int screenHeight = this.windowManager.getDefaultDisplay().getHeight();

        int xPos = ((screenWidth - rootWidth) / 2) + xOffset;
        int yPos = anchorRect.top - rootHeight + yOffset;

        // display on bottom
        if (rootHeight > anchorRect.top)
        {
            yPos = anchorRect.bottom + yOffset;
            this.window.setAnimationStyle(R.style.Animations_GrowFromTop);
        }

        this.window.showAtLocation(this.anchor, Gravity.NO_GRAVITY, xPos, yPos);
    }

    public void dismiss()
    {
        this.window.dismiss();
    }
}
