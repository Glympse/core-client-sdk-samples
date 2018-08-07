package com.glympse.android.cardsdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.glympse.android.api.GC;
import com.glympse.android.api.GE;
import com.glympse.android.api.GEventListener;
import com.glympse.android.api.GEventSink;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GLinkedAccountsManager;
import com.glympse.android.api.GlympseFactory;
import com.glympse.android.core.GPrimitive;
import com.glympse.android.hal.Helpers;
import com.glympse.android.lib.GLinkedAccountPrivate;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public class FragmentAccountCreation extends Fragment implements GEventListener
{
    private TextView _label;
    private EditText _numberEntry;
    private Button _submitButton;

    private GPrimitive _profile;
    private PhoneNumberFormattingTextWatcher _textChangedListener;

    public static Fragment newInstance()
    {
        return new FragmentAccountCreation();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_account_creation, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        _label = (TextView) view.findViewById(R.id.label);
        _numberEntry = (EditText) view.findViewById(R.id.number_entry);
        _submitButton = (Button) view.findViewById(R.id.submit);

        _numberEntry.addTextChangedListener(_textChangedListener = new PhoneNumberFormattingTextWatcher());
        _submitButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                setEnabled(false);
                String number = _numberEntry.getText().toString().trim();
                if ( null == _profile )
                {
                    submitPhoneNumber(number);
                }
                else
                {
                    submitConfirmationCode(number);
                }
            }
        });
    }

    @Override
    public void eventsOccurred(GGlympse glympse, int listener, int events, Object obj)
    {
        if ( GE.LISTENER_CONFIRMATION_CODE == listener )
        {
            if ( 0 != ( events & GE.CONFIRMATION_CODE_SENT ) )
            {
                advanceToConfirmationCode();
                setEnabled(true);
            }
            else if ( 0 != (events & GE.CONFIRMATION_CODE_FAILED) )
            {
                Toast.makeText(getContext(), R.string.invalid_number, Toast.LENGTH_SHORT).show();
                _profile = null;
                setEnabled(true);
            }
        }

        if ( GE.LISTENER_LINKED_ACCOUNTS == listener )
        {
            if ( 0 != ( events & GE.ACCOUNT_LINK_SUCCEEDED ) )
            {
                finish();
            }
            else if ( 0 != (events & GE.ACCOUNT_LINK_FAILED) )
            {
                GLinkedAccountPrivate account = Helpers.tryCast(obj, GLinkedAccountPrivate.class);
                if ( GC.SERVER_ERROR_EXISTING_LINK == account.getError().getType() )
                {
                    // Another account already owns this address. Login as that account
                    GlympseWrapper.instance().restoreAccount(getActivity(), _profile);
                    finish();
                }
                else if ( GC.SERVER_ERROR_LINK_FAILED == account.getError().getType() )
                {
                    Toast.makeText(getContext(), R.string.invalid_code, Toast.LENGTH_SHORT).show();
                    setEnabled(true);
                }
            }
        }
    }

    private void setEnabled(boolean enabled)
    {
        _numberEntry.setEnabled(enabled);
        _submitButton.setEnabled(enabled);
    }

    private void submitPhoneNumber(String numberString)
    {
        String number = null;
        try
        {
            PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
            // Passing in unknown region code "ZZ"
            Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse("+" + numberString, "ZZ");
            number = phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
        }
        catch(Exception e)
        {
        }
        // If the returned number is null, it was not a valid phone number
        if ( null == number )
        {
            Toast.makeText(getContext(), R.string.invalid_number, Toast.LENGTH_SHORT).show();
            setEnabled(true);
            return;
        }
        _profile = GlympseFactory.createPhoneAccountProfile(number);

        GEventSink sink = GlympseWrapper.instance().getGlympse().getLinkedAccountsManager()
                .confirm(GC.LINKED_ACCOUNT_TYPE_PHONE(), _profile);
        sink.addListener(this);
    }

    private void submitConfirmationCode(String code)
    {
        _profile.put("code", code);

        GLinkedAccountsManager linkedAccountsManager = GlympseWrapper.instance().getGlympse()
                .getLinkedAccountsManager();
        linkedAccountsManager.addListener(this);
        linkedAccountsManager.link(GC.LINKED_ACCOUNT_TYPE_PHONE(), _profile);
    }

    private void advanceToConfirmationCode()
    {
        _label.setText(R.string.confirmation_code);
        _numberEntry.setText("");
        _numberEntry.setHint(R.string.confirmation_code_hint);
        _numberEntry.removeTextChangedListener(_textChangedListener);
    }

    private void finish()
    {
        ((GlympseCardsDemoActivity) getActivity()).navigate(FragmentCardsHome.class);
    }
}
