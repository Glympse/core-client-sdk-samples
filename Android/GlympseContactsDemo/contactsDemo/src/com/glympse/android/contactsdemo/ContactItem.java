package com.glympse.android.contactsdemo;

import com.glympse.android.contacts.GSmartButton;

public class ContactItem
{
    public String _contactName;
    public String _contactNumber;
    public GSmartButton _smartButton;
    
    public ContactItem(String name, String number)
    {
        _contactName = name;
        _contactNumber = number;
    }
}
