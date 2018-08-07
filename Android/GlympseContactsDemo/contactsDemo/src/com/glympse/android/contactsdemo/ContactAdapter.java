package com.glympse.android.contactsdemo;

import java.util.List;

import com.glympse.android.api.GC;
import com.glympse.android.contacts.GSmartButton;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ContactAdapter extends ArrayAdapter<ContactItem>
{
    private LayoutInflater _inflater;
    
    public ContactAdapter(ListView listView, List<ContactItem> contactItems)
    {
        super(listView.getContext(), 0, contactItems);
        _inflater = LayoutInflater.from(listView.getContext());
    }

    @Override public View getView(int position, View view, ViewGroup parent)
    {
        // Get the historyItem we are on.
        final ContactItem contactItem = ((position >= 0) && (position < getCount())) ? getItem(position) : null;

        // Create a new view if we are not recycling an old view.
        if (null == view)
        {
            view = _inflater.inflate(R.layout.contact_item, parent, false);
        }
        
        TextView contactName = (TextView) view.findViewById(R.id.contact_name);
        contactName.setText(contactItem._contactName + " (" + contactItem._contactNumber + ")");

        // Initialize Smart G-Button. 
        // This should be done, when Smart G-Button is constructed.
        if(contactItem._smartButton == null)
        {
            contactItem._smartButton = (GSmartButton) view.findViewById(R.id.smart_button);
            contactItem._smartButton.attachGlympse(GlympseWrapper.instance().getGlympse());
            contactItem._smartButton.attachPerson(contactItem._contactName, contactItem._contactNumber, GC.INVITE_TYPE_SMS);
        }
        
        return view;
    }
    
    

}
