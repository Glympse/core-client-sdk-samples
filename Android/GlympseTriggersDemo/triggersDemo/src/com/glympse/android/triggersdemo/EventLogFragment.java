package com.glympse.android.triggersdemo;

import java.util.Date;

import com.glympse.android.api.GE;
import com.glympse.android.api.GEventListener;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GTrigger;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class EventLogFragment extends Fragment implements GEventListener {
    
    TextView _eventView;
	
	public EventLogFragment()
	{
		super();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.event_log_fragment, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		_eventView = (TextView) view.findViewById(R.id.event_log);		
		_eventView.setMovementMethod(new ScrollingMovementMethod());
		
		updateLog();
	}
	
   @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Subscribe on events. 
        GlympseWrapper.instance().getGlympse().getTriggersManager().addListener(this);
    }
    
    @Override
    public void onDestroy()
    {
        super.onStop();
        // Unsubscribe from events. 
        GlympseWrapper.instance().getGlympse().getTriggersManager().removeListener(this);
    }

    @Override
    public void eventsOccurred(GGlympse glympse, int listener, int events, Object obj)
    {
        if ( 0 != ( GE.TRIGGERS_TRIGGER_ACTIVATED & events ) )
        {
            updateLog();
        }
        else if ( 0 != ( GE.TRIGGERS_TRIGGER_REMOVED & events ) )
        {
            updateLog();
        }
        else if ( 0 != ( GE.TRIGGERS_TRIGGER_ADDED & events ) )
        {
            updateLog();
        }
    }
    
    private void updateLog()
    {
        // Populate log screen
        String previousLogs = EventLogStorage.loadLogContent(getActivity());
        if ( !previousLogs.isEmpty() )
        {
            // Reverse the log entries so they appear as newest first
            String[] logEntries = previousLogs.split("\n");
            StringBuffer sb = new StringBuffer(previousLogs.length());
            for ( String entry : logEntries )
            {
                entry = entry + "\n";
                sb.insert(0, entry);
            }
            _eventView.setText(sb.toString());
        }
    }

}
