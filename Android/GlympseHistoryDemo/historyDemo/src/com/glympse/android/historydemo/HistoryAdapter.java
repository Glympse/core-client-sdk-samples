package com.glympse.android.historydemo;

import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.glympse.android.api.GTicket;
import com.glympse.android.hal.Helpers;

public class HistoryAdapter extends ArrayAdapter<HistoryItem>
{
    private LayoutInflater _inflater;
    private GlympseHistoryDemoActivity _parentActivity;
    
    public HistoryAdapter(ListView listView, List<HistoryItem> historyItems, GlympseHistoryDemoActivity activity)
    {
        super(listView.getContext(), 0, historyItems);
        _inflater = LayoutInflater.from(listView.getContext());
        _parentActivity = activity;
    }

    @Override public View getView(int position, View view, ViewGroup parent)
    {
        // Get the historyItem we are on.
        final HistoryItem historyItem = ((position >= 0) && (position < getCount())) ? getItem(position) : null;

        // Create a new view if we are not recycling an old view.
        if (null == view)
        {
            view = _inflater.inflate(R.layout.history_item, parent, false);
        }
        
        ((Button)view.findViewById(R.id.btn_expire)).setOnClickListener(new OnClickListener()
        {
            @Override public void onClick(View view)
            {
                expireGlympse(historyItem._ticket);
                historyItem._isExpiring = true;
            }
        });
        
        ((Button)view.findViewById(R.id.btn_plus15)).setOnClickListener(new OnClickListener()
        {
            @Override public void onClick(View view)
            {
                plusFifteenMins(historyItem._ticket);
            }
        });
        
        ((Button)view.findViewById(R.id.btn_modify)).setOnClickListener(new OnClickListener()
        {
            @Override public void onClick(View view)
            {
                modifyGlympse(historyItem._ticket);
            }
        });
        
        if (historyItem != null)
        {
            if (historyItem._remainingTime > 0L)
            {
                // Active ticket
                view.findViewById(R.id.glympse_details).setVisibility(View.VISIBLE);
                view.findViewById(R.id.glympse_buttons).setVisibility(View.VISIBLE);
                view.findViewById(R.id.layout_expired_info).setVisibility(View.GONE);

                TextView recipients = (TextView) view.findViewById(R.id.line1);
                TextView numberWatched = (TextView) view.findViewById(R.id.line2);
                
                recipients.setText(historyItem._recipients);
                if(historyItem._isExpiring)
                {
                    numberWatched.setText("Expiring...");
                }
                else
                {
                    numberWatched.setText(historyItem._numberWatched + " watched");
                }
                
                long expireTime = historyItem._ticket.getExpireTime();
                long currentTime = GlympseWrapper.instance().getGlympse().getTime();
                ((TextView) view.findViewById(R.id.line3))
                    .setText(Helpers.formatDuration(expireTime - currentTime) + " remaining");
            }
            else
            {
                // Expired ticket
                view.findViewById(R.id.glympse_details).setVisibility(View.GONE);
                view.findViewById(R.id.glympse_buttons).setVisibility(View.GONE);
                view.findViewById(R.id.layout_expired_info).setVisibility(View.VISIBLE);
                
                TextView recipients = (TextView) view.findViewById(R.id.history_recipients);
                TextView numberWatched = (TextView) view.findViewById(R.id.history_number_watched);
                TextView timeRemaining = (TextView) view.findViewById(R.id.history_time_remaining);

                recipients.setText(historyItem._recipients);
                numberWatched.setText(historyItem._numberWatched + " watched");
                timeRemaining.setText("Expired");
            }
        }
        
        return view;
    }
    
    public void expireGlympse(GTicket ticket)
    {
        if (ticket != null)
        {
            ticket.modify(0, null, null);
            //stopTimer();
        }
    }

    public void plusFifteenMins(GTicket ticket)
    {
        if (ticket != null)
        {
            ticket.modify(ticket.getDuration() + (int) Helpers.MS_PER_MINUTE * 15, ticket.getMessage(), null);
        }
    }

    public void modifyGlympse(GTicket ticket)
    {
        if (ticket != null)
        {
            ticket.modify(_parentActivity.getDuration(), _parentActivity.getMessage(), null);
        }
    }

    public void updateTicket(GTicket ticket)
    {
        for(int position = 0; position < this.getCount(); position++)
        {
            HistoryItem item = this.getItem(position);
            if(item._ticket.getStartTime() == ticket.getStartTime())
            {
                item.update(ticket);
                break;
            }
        }
        this.notifyDataSetChanged();
    }

}
