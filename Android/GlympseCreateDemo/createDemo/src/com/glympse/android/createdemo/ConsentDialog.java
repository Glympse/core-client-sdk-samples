package com.glympse.android.createdemo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.glympse.android.api.GConsentManager;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GlympseConstants;

public class ConsentDialog
{
    private static final int PARTNER_ID = 0;

    public static void showIfNeeded(final GGlympse glympse, Context context)
    {
        final GConsentManager consentManager = glympse.getConsentManager();
        // Check to see if the user has already granted consent to collect data
        if ( consentManager.hasConsent() )
        {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setMessage(R.string.glympse_gdpr_message);

        builder.setPositiveButton(R.string.glympse_gdpr_accept, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                consentManager.grantConsent(GlympseConstants.CONSENT_TYPE_SUBJECT(), PARTNER_ID);
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                if ( glympse.hasUserAccount() )
                {
                    // Glympse can continue to run if an account already exists but functionality
                    // will be limited unless they grant consent.
                    consentManager.revokeConsent(PARTNER_ID);
                }
                else
                {
                    // If the user has no user account, denying consent means that an account
                    // should not be created for them. Restart Glympse and ask for consent next
                    // time the user wants to use Glympse features.
                    GlympseWrapper.instance().stop();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }
}
