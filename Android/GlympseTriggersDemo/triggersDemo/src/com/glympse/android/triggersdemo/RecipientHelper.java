package com.glympse.android.triggersdemo;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.widget.Toast;

public class RecipientHelper
{
    
    public static void getAddressData(Context context, Uri contactData, final RecipientListener listener)
    {
        if ( null != contactData )
        {
            String contactId = null;
            String address = null;
            String name = null;
            boolean hasNumber = false;
            
            // Get the name and photo
            String[] projection = 
            { 
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.HAS_PHONE_NUMBER
            };
            Cursor cursor = context.getContentResolver().query(contactData, projection, null, null, null);
            if( cursor.moveToFirst() )
            {
                // Get the contact id
                int column = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID);
                contactId = cursor.getString(column);
                
                // Get name
                column = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY);
                name = cursor.getString(column);
                
                // Has a phone number?
                column = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER);
                hasNumber = cursor.getInt(column) == 1 ? true : false;
            }
            cursor.close();
            
            // Gather available phone numbers
            final Vector<AddressData> addressData = new Vector<AddressData>();
            if ( hasNumber )
            {
                String[] phoneProjection =
                {
                    Phone.NUMBER
                };
                cursor = context.getContentResolver().query(Phone.CONTENT_URI, phoneProjection, 
                    Phone.CONTACT_ID + " = " + contactId, null, null);
                Vector<String> numbers = new Vector<String>();
                if ( cursor.moveToFirst() )
                {
                    do
                    {
                        int column = cursor.getColumnIndexOrThrow(Phone.NUMBER);
                        address = cursor.getString(column);
                        if ( numbers.contains(address) )
                        {
                            continue;
                        }
                        numbers.add(address);
                        addressData.add(new AddressData(address, name));
                    } while ( cursor.moveToNext() );
                }
                cursor.close();
            }
    
            // Gather available emails
            String[] emailProjection =
            {
                Email.ADDRESS
            };
            cursor = context.getContentResolver().query(Email.CONTENT_URI, emailProjection, 
                Phone.CONTACT_ID + " = " + contactId, null, null);
            Vector<String> emails = new Vector<String>();
            if ( cursor.moveToFirst() )
            {
                do 
                {
                    int column = cursor.getColumnIndexOrThrow(Email.ADDRESS);
                    address = cursor.getString(column);
                    if ( emails.contains(address) )
                    {
                        continue;
                    }
                    emails.add(address);
                    addressData.add(new AddressData(address, name));
                } while ( cursor.moveToNext() );
            }
            cursor.close();
            
            if ( 0 == addressData.size() )
            {
                // They didn't have any useful contact addresses
                Toast.makeText(context, "Contact has no email or number", Toast.LENGTH_LONG).show();
                listener.onRecipientSelected(null);
                return;
            }
            else if ( 1 == addressData.size() )
            {
                // Only one contact method, don't show a dialog
                listener.onRecipientSelected(addressData.get(0));
                return;
            }
            else
            {
                // Display dialog to pick an address
                List<String> addressList = new ArrayList<String>();
                for ( AddressData currentData : addressData )
                {
                    addressList.add(currentData.getAddress());
                }
                final CharSequence[] addresses = addressList.toArray(new String[addressList.size()]);
                
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Pick an address");
                builder.setItems(addresses, new DialogInterface.OnClickListener() 
                {
                    public void onClick(DialogInterface dialog, int which) 
                    {
                        AddressData data = addressData.get(which);
                        listener.onRecipientSelected(data);
                    }
                });
                builder.create().show();
                return;
            }
        }
    }
    
    public interface RecipientListener
    {
        public void onRecipientSelected(AddressData data);
    }
    
    public static class AddressData
    {
        private String _name;
        private String _address;
        
        public AddressData(String address, String name)
        {
            _address = address;
            _name = name;
        }
        
        public String getAddress()
        {
            return _address;
        }
        
        public String getName()
        {
            return _name;
        }
    }
}
