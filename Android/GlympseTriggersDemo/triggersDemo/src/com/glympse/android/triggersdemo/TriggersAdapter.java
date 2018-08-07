package com.glympse.android.triggersdemo;

import com.glympse.android.api.GGeoTrigger;
import com.glympse.android.api.GTicket;
import com.glympse.android.api.GTrigger;
import com.glympse.android.api.GTriggersManager;
import com.glympse.android.core.GArray;
import com.glympse.android.hal.GVector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class TriggersAdapter extends BaseAdapter
{    
    private LayoutInflater _inflater;    
    private GArray<GTrigger> _triggers;
    
    public TriggersAdapter(Context context)
    {       
        _inflater = LayoutInflater.from(context);        
        _triggers = new GVector<GTrigger>();
    }
    
    public void refresh(GArray<GTrigger> triggers)
    {
        if ( null == triggers )
        {
            return;
        }
        
        _triggers = triggers;
        
        notifyDataSetChanged();
    }

    @Override public int getCount() 
    {
        return _triggers.length();
    }

    @Override public Object getItem(int position) 
    {
        return _triggers.at(position);
    }

    @Override public long getItemId(int position) 
    {
        return position;
    }

    @Override public View getView(int position, View view, ViewGroup parent) 
    {
        if ( null == view )
        {
            view = _inflater.inflate(R.layout.item_trigger, parent, false);
        }        
        
        final GGeoTrigger trigger = (GGeoTrigger)getItem(position);
        GTicket ticket = trigger.getTicket();        
        
        // Refresh the content.
        ((TextView)view.findViewById(R.id.name)).setText("Trigger: " + trigger.getName());
        ((TextView)view.findViewById(R.id.type)).setText("T: " + Formatter.formatTriggerType(trigger.getType())
            + Formatter.formatTransition(trigger.getTransition()));
        ((TextView)view.findViewById(R.id.region)).setText("R: " + Formatter.formatRegion(trigger.getRegion()));
        ((TextView)view.findViewById(R.id.auto_send)).setText("Auto send: " + trigger.autoSend());
        ((TextView)view.findViewById(R.id.message)).setText("M: " + ticket.getMessage());
        ((TextView)view.findViewById(R.id.recipients)).setText(Formatter.formatRecipients(ticket));  
        
        ImageView deleteButton = (ImageView) view.findViewById(R.id.delete);
        deleteButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                GTriggersManager triggerManager = GlympseWrapper.instance().getGlympse().getTriggersManager();
                triggerManager.removeLocalTrigger(trigger);
            }
        });
        
        return view;
    }
}
