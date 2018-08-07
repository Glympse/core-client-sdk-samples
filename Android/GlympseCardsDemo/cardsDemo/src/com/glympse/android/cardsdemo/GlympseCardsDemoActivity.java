//------------------------------------------------------------------------------
//
// Copyright (c) 2017 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

package com.glympse.android.cardsdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import com.glympse.android.api.GC;
import com.glympse.android.api.GE;
import com.glympse.android.api.GEventListener;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GLinkedAccount;
import com.glympse.android.api.GLinkedAccountsManager;
import com.glympse.android.core.GArray;
import com.glympse.android.hal.GVector;
import com.glympse.android.hal.Helpers;
import com.glympse.android.lib.TicketCode;
import com.glympse.android.lib.UrlParser;

public class GlympseCardsDemoActivity extends BaseActivity implements GEventListener
{
    private View _loadingView;
    private boolean _processedIntent;

    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        _loadingView = findViewById(R.id.loading_layout);

        // Initialize our Glympse wrapper.
        GlympseWrapper.instance().start(this, null);
        GlympseWrapper.instance().getGlympse().addListener(this);

        GLinkedAccountsManager linkedAccountsManager = GlympseWrapper.instance().getGlympse().getLinkedAccountsManager();

        // If the LinkedAccountManager is synced we can determine if we're logged in or not
        if ( linkedAccountsManager.isSynced() )
        {
            navigateUsingLoginStatus();
        }
        // Otherwise we have to wait for that to sync
        else
        {
            linkedAccountsManager.addListener(this);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Intent intent = getIntent();
        if (null != intent)
        {
            setIntent(null);
            if ( !_processedIntent )
            {
                _processedIntent = true;
                processIntent(intent);
            }
        }
    }

    @Override public void eventsOccurred(GGlympse glympse, int listener, int events, Object obj)
    {
        if ( GE.LISTENER_LINKED_ACCOUNTS == listener )
        {
            if ( 0 != ( events & GE.ACCOUNT_LIST_REFRESH_SUCCEEDED ) )
            {
                navigateUsingLoginStatus();
            }
        }
    }

    private void showLoading(boolean show)
    {
        _loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void navigateUsingLoginStatus()
    {
        showLoading(false);
        if ( isLoggedIn() )
        {
            navigate(FragmentCardsHome.class);
        }
        else
        {
            navigate(FragmentAccountCreation.class);
        }
    }

    private boolean isLoggedIn()
    {
        GLinkedAccountsManager linkedAccountsManager = GlympseWrapper.instance().getGlympse()
                .getLinkedAccountsManager();
        GLinkedAccount phoneAccount = linkedAccountsManager.getAccount(GC.LINKED_ACCOUNT_TYPE_PHONE());
        if ( null != phoneAccount && GC.LINKED_ACCOUNT_STATE_LINKED == phoneAccount.getState() )
        {
            return true;
        }
        return false;
    }

    public void navigate(Class fragmentClass)
    {
        // Make sure we're not already viewing this fragment
        if ( _fragments.size() > 0 && fragmentClass == _fragments.peek().getClass() )
        {
            return;
        }

        Fragment fragment = null;
        if ( fragmentClass == FragmentCardsHome.class ) fragment = FragmentCardsHome.newInstance();
        else if ( fragmentClass == FragmentAccountCreation.class) fragment = FragmentAccountCreation.newInstance();

        addRemoveFragment(0, fragment);
    }

    private void processIntent(Intent intent)
    {
        String strAction = Helpers.safeStr(intent.getAction());

        if ( strAction.equalsIgnoreCase(Intent.ACTION_VIEW) )
        {
            final String url = intent.getDataString();
            if ( null == url )
            {
                return;
            }

            GVector<String> schemes = new GVector<>();
            schemes.add("glympse.co");

            UrlParser urlParser = new UrlParser();
            urlParser.parseUrls(url, schemes, false);
            GArray<String> inviteCodes = urlParser.getInviteCodes();
            if ( null != inviteCodes && 1 == inviteCodes.length() )
            {
                String inviteCode = TicketCode.cleanupInviteCode(inviteCodes.at(0));
                long inviteCodeInt = TicketCode.base32ToLong(inviteCode);
                int inviteAspect = TicketCode.getInviteAspect(inviteCodeInt);

                if ( inviteAspect == GC.INVITE_ASPECT_CARD )
                {
                    GlympseWrapper.instance().getGlympse().openUrl(url, GC.INVITE_MODE_DEFAULT, null);
                }
            }
        }
    }
}
