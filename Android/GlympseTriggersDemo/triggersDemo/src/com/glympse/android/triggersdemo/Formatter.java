package com.glympse.android.triggersdemo;

import java.util.Locale;

import android.annotation.SuppressLint;

import com.glympse.android.api.GC;
import com.glympse.android.api.GInvite;
import com.glympse.android.api.GPlace;
import com.glympse.android.api.GTicket;
import com.glympse.android.api.GlympseTools;
import com.glympse.android.core.CC;
import com.glympse.android.core.GRegion;
import com.glympse.android.hal.Concurrent;
import com.glympse.android.hal.Helpers;
import com.glympse.android.hal.Platform;

@SuppressLint("DefaultLocale")
public class Formatter 
{    
    // Create a string representation of the trigger type
    public static String formatTriggerType(int type)
    {
        switch (type) 
        {
            case GC.TRIGGER_TYPE_GEO:
                return "GC.TRIGGER_TYPE_GEO";
        default:
            return "GC.TRIGGER_TYPE_UNKNOWN";
        }
    }
    
    // Create a string representation of the destination
    public static String formatDestination(GTicket ticket)
    {
        if ( null == ticket )
            return "No destination";        
        GPlace destination = ticket.getDestination();
        if ( null == destination )
            return "No destination";
        String dest = "";
        if ( null != destination.getName() )
            dest += destination.getName() + ": ";
        dest += ((double)((int)((destination.getLatitude() * 10000)))/10000) + ", ";
        dest += ((double)((int)((destination.getLongitude() * 10000)))/10000);
        return dest;
    }       
    
    // Create a string representation of the geofence region
    public static String formatRegion(GRegion region)
    {
        if ( null == region )
            return "No region";        
        String reg = "";
        reg += ((double)((int)((region.getLatitude() * 10000)))/10000) + ", ";
        reg += ((double)((int)((region.getLongitude() * 10000)))/10000) +", ";
        reg += region.getRadius() + "m";
        return reg;
    }         
    
    // Create a string representation of the recipient
    public static String formatRecipients(GTicket ticket)
    {
        if ( null == ticket )
            return "<< no ticket found >>";           
        String result = "";
        for ( GInvite invite : ticket.getInvites() )
        {
            String address = invite.getAddress();
            if ( Helpers.isEmpty(address) )
            {
                address = GlympseTools.inviteTypeEnumToString(invite.getType()).toUpperCase(Locale.getDefault());
            }
            result += address + "\n";
        }
        if ( result.length() > 0 )
        {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }  

    // Create a string representation of whether this geofence triggers
    // on entering or leaving the region
    public static String formatTransition(int transition)
    {
        if ( CC.GEOFENCE_TRANSITION_ENTER == transition )
        {
            return " on Enter";
        }
        else if ( CC.GEOFENCE_TRANSITION_EXIT == transition )
        {
            return " on Exit";
        }
        return "";
    }
}
