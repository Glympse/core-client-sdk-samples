package com.glympse.android.triggersdemo;

import com.glympse.android.api.GE;
import com.glympse.android.api.GEventListener;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GTrigger;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class TriggersListFragment extends ListFragment implements GEventListener
{

	TriggersAdapter _triggerAdapter;
	public TriggersListFragment()
	{
		super();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.trigger_list_fragment, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
	    // Setup our list of triggers
		_triggerAdapter = new TriggersAdapter(getActivity());
		getListView().setAdapter(_triggerAdapter);
		// Refresh the list's data
		refreshTriggers();
		
	}
	
	@Override
	public void setEmptyText(CharSequence text)
	{
		TextView emptyText = (TextView) getView().findViewById(android.R.id.empty);
		emptyText.setText(text.toString());
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
	
	/**
     * Trigger Ops
     */
    
    private void triggerActivated(GTrigger trigger)
    {
        Toast.makeText(getActivity(), "Trigger activated: " + trigger.getName(), Toast.LENGTH_LONG).show();
    }
	
	private void refreshTriggers()
    {
	    // Update our list with the latest saved triggers
        _triggerAdapter.refresh(GlympseWrapper.instance().getGlympse().getTriggersManager().getLocalTriggers());
    }

    /**
     * GEventListener section
     */
    
    public void eventsOccurred(GGlympse glympse, int listener, int events, Object obj)
    {
        if ( GE.LISTENER_TRIGGERS == listener )
        {
            if ( 0 != ( ( GE.TRIGGERS_TRIGGER_ADDED | GE.TRIGGERS_TRIGGER_REMOVED ) & events ) )
            {
                refreshTriggers();
            }
            else if ( 0 != ( GE.TRIGGERS_TRIGGER_ACTIVATED & events ) )
            {
                GTrigger trigger = (GTrigger)obj;
                triggerActivated(trigger);
            }
        }
    }
}
