package com.glympse.android.historydemo;

import com.glympse.android.api.GInvite;
import com.glympse.android.api.GTicket;

public class HistoryItem
{
    public String _recipients;
    public long _remainingTime;
    public int _numberWatched;
    public GTicket _ticket;
    public boolean _isExpiring;

    public HistoryItem(GTicket ticket, GlympseHistoryDemoActivity parentActivity)
    {
        update(ticket);
    }

    public void update(GTicket ticket)
    {
        this._ticket = ticket;
        _remainingTime = ticket.getExpireTime() - GlympseWrapper.instance().getGlympse().getTime();
        StringBuilder sbRecipients = new StringBuilder();
        _numberWatched = 0;
        for (GInvite invite : ticket.getInvites())
        {
            sbRecipients.append(invite.getName() != null ? invite.getName() : invite.getAddress());
            sbRecipients.append(", ");
            _numberWatched += invite.getViewers();
        }

        _recipients = (sbRecipients.length() > 0) 
            ? sbRecipients.delete(sbRecipients.length() - 2, sbRecipients.length()).toString()
            : "(No Recipient)";
    }
    

}
