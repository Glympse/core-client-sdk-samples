package com.glympse.android.triggersdemo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

import com.glympse.android.api.GTrigger;

import android.content.Context;

public class EventLogStorage
{
    final private static String FILENAME = "event_log";

    public static String loadLogContent(Context context)
    {
        StringBuffer fileBuffer = new StringBuffer("");
        byte[] buffer = new byte[1024];
        int byteCount = 0;
        FileInputStream fis;
        
        try
        {
            fis = context.openFileInput(FILENAME);
        }
        catch (FileNotFoundException e)
        {
            return "";
        }

        try
        {
            while ((byteCount = fis.read(buffer)) != -1) 
            { 
                fileBuffer.append(new String(buffer, 0, byteCount)); 
            }
        }
        catch (IOException e)
        {
        }
        
        return fileBuffer.toString();
    }
    
    public static void appendLog(Context context, String entry)
    {
        FileOutputStream fos;
        try
        {
            fos = context.openFileOutput(FILENAME, Context.MODE_APPEND);
        }
        catch (FileNotFoundException e)
        {
            return;
        }
        
        OutputStreamWriter osw = new OutputStreamWriter(fos);
        try
        {
            osw.write(entry);
            osw.flush();
        }
        catch (IOException e)
        {
        }
        finally
        {
            try
            {
                fos.close();
                osw.close();
            }
            catch (IOException e)
            {
            }
        }
    }

    public static String createNewEntry(Date date, GTrigger trigger, String event)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(date.toString());
        sb.append(": ");
        sb.append("Trigger ");
        sb.append(trigger.getName());
        sb.append(" ");
        sb.append(event);
        sb.append("\n");
        return sb.toString();
    }

}
