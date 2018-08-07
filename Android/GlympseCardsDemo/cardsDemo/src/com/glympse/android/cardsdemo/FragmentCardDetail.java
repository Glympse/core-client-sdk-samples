package com.glympse.android.cardsdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.glympse.android.api.GAddress;
import com.glympse.android.api.GC;
import com.glympse.android.api.GCard;
import com.glympse.android.api.GCardEvent;
import com.glympse.android.api.GCardInvite;
import com.glympse.android.api.GCardMember;
import com.glympse.android.api.GCardMemberDescriptor;
import com.glympse.android.api.GCardObject;
import com.glympse.android.api.GCardObjectInvite;
import com.glympse.android.api.GCardObjectPoi;
import com.glympse.android.api.GCardTicket;
import com.glympse.android.api.GCardTicketBuilder;
import com.glympse.android.api.GE;
import com.glympse.android.api.GEventListener;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GInvite;
import com.glympse.android.api.GPoi;
import com.glympse.android.api.GPoiBuilder;
import com.glympse.android.api.GTicket;
import com.glympse.android.api.GUser;
import com.glympse.android.api.GUserManager;
import com.glympse.android.api.GlympseFactory;
import com.glympse.android.api.GlympseTools;
import com.glympse.android.hal.Helpers;
import com.glympse.android.lib.LibFactory;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FragmentCardDetail extends Fragment implements GEventListener
{
    private static final String CARD_ID_KEY = "card_id";

    private AppCompatEditText _cardName;
    private TextView _memberList;
    private TextView _inviteList;
    private TextView _objectList;
    private TextView _activitylist;

    private GCard _card;

    public static Fragment newInstance(String cardId)
    {
        Fragment fragment = new FragmentCardDetail();
        Bundle bundle = new Bundle();
        bundle.putString(CARD_ID_KEY, cardId);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_card_detail, container, false);
    }

    @SuppressLint("WrongViewCast")
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        String cardId = getArguments().getString(CARD_ID_KEY);
        _card = GlympseWrapper.instance().getGlympse().getCardManager().findCardByCardId(cardId);

        _cardName = (AppCompatEditText) view.findViewById(R.id.card_name);
        _memberList = (TextView) view.findViewById(R.id.member_list);
        _inviteList = (TextView) view.findViewById(R.id.invite_list);
        _objectList = (TextView) view.findViewById(R.id.object_list);
        _activitylist = (TextView) view.findViewById(R.id.activity_list);

        refreshMemberList();
        refreshInviteList();
        refreshObjectList();
        refreshActivityList();

        _cardName.setText(_card.getName());
        _cardName.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override public void onFocusChange(View v, boolean hasFocus)
            {
                if (!hasFocus)
                {
                    updateCardNameIfChanged();
                }
            }
        });
        _cardName.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (0 != ( EditorInfo.IME_MASK_ACTION & actionId))
                {
                    updateCardNameIfChanged();
                }
                return false;
            }
        });
        _cardName.setOnKeyListener(new View.OnKeyListener()
        {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                if ( ( KeyEvent.ACTION_DOWN == event.getAction() ) && ( KeyEvent.KEYCODE_ENTER == keyCode ) )
                {
                    updateCardNameIfChanged();
                }
                return false;
            }
        });

        view.findViewById(R.id.kick_members).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                showKickMembersDialog();
            }
        });

        view.findViewById(R.id.leave_card).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                showLeaveCardDialog();
            }
        });

        view.findViewById(R.id.send_invite).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                showSendInviteDialog();
            }
        });

        view.findViewById(R.id.create_poi).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                showCreatePoiDialog();
            }
        });

        view.findViewById(R.id.share_location).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                showShareLocationDialog();
            }
        });

        view.findViewById(R.id.request_location).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                showRequestLocationDialog();
            }
        });

        _card.addListener(this);
        _card.getActivity().addListener(this);
        for ( GCardMember cardMember : _card.getMembers() )
        {
            cardMember.addListener(this);
        }
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        _card.removeListener(this);
        _card.getActivity().removeListener(this);
        for ( GCardMember cardMember : _card.getMembers() )
        {
            cardMember.removeListener(this);
        }
    }

    private void updateCardNameIfChanged()
    {
        // Get the name from our edit box.
        String name = _cardName.getText().toString().trim();

        if ( !Helpers.safeEquals(name, _card.getName()) && !Helpers.isEmpty(name) )
        {
            _card.updateName(name);
        }
    }

    private void refreshMemberList()
    {
        GUserManager userManager = GlympseWrapper.instance().getGlympse().getUserManager();

        StringBuilder sb = new StringBuilder();
        for ( GCardMember cardMember : _card.getMembers() )
        {
            GUser user = userManager.findUserByUserId(cardMember.getUserId());
            sb.append("Name: ").append(null != user ? user.getNickname() : "").append('\n');

            GCardMemberDescriptor invitingMember = cardMember.getInviter();
            GUser inviter = null != invitingMember ? userManager.findUserByUserId(invitingMember.getUserId()) : null;
            sb.append("Invited by: ").append(null != inviter ? inviter.getNickname() : "").append('\n');

            GCardTicket sharingTicket = cardMember.getTicket();
            GTicket ticket = null != sharingTicket ? sharingTicket.getTicket() : null;
            sb.append("Sharing: ").append(null != ticket ? ticket.getId(): "").append('\n');

            GCardTicket requestingTicket = cardMember.getRequest();
            GTicket request = null != requestingTicket ? requestingTicket.getTicket() : null;
            sb.append("Request: ").append(null != request ? request.getId(): "").append('\n');

            sb.append('\n');
        }

        if ( sb.length() >= 2 )
        {
            sb.delete(sb.length() - 2, sb.length() - 1);
        }

        _memberList.setText(sb.toString());
    }

    private void refreshInviteList()
    {
        StringBuilder sb = new StringBuilder();
        for ( GCardInvite cardInvite : _card.getInvites() )
        {
            sb.append("Code: ").append(cardInvite.getInvite().getCode()).append('\n');
            sb.append("Created: ").append(cardInvite.getCreatedTime()).append('\n');

            sb.append('\n');
        }

        if ( sb.length() >= 2 )
        {
            sb.delete(sb.length() - 2, sb.length() - 1);
        }

        _inviteList.setText(sb.toString());
    }

    private String getAddressAsString(GAddress address)
    {
        if ( null == address )
        {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(address.getLine1()).append(' ')
                .append(address.getLine2()).append(' ')
                .append(address.getCity()).append(' ')
                .append(address.getState()).append(' ')
                .append(address.getZip()).append(' ')
                .append(address.getCountry());
        return sb.toString();
    }

    private void refreshObjectList()
    {
        StringBuilder sb = new StringBuilder();
        for ( GCardObject cardObject : _card.getObjects() )
        {
            sb.append("Type: ").append(cardObject.getClass().getSimpleName()).append('\n');
            if ( cardObject instanceof GCardObjectPoi )
            {
                GPoi poi = ((GCardObjectPoi) cardObject).getPoi();
                sb.append("Name: ").append(poi.getName()).append('\n');
                sb.append("Location: ")
                        .append(poi.getLocation().getLatitude())
                        .append(',')
                        .append(poi.getLocation().getLongitude())
                        .append('\n');
                sb.append("Address: " ).append(getAddressAsString(poi.getAddress())).append('\n');
            }
            else if ( cardObject instanceof GCardObjectInvite )
            {
                sb.append("Code: ").append(( (GCardObjectInvite) cardObject ).getInviteCode()).append('\n');
            }

            sb.append('\n');
        }

        if ( sb.length() >= 2 )
        {
            sb.delete(sb.length() - 2, sb.length() - 1);
        }

        _objectList.setText(sb.toString());
    }

    private void refreshActivityList()
    {
        long currentTime = GlympseWrapper.instance().getGlympse().getTime();
        GUserManager userManager = GlympseWrapper.instance().getGlympse().getUserManager();

        StringBuilder sb = new StringBuilder();
        for ( GCardEvent event : _card.getActivity().getEvents() )
        {
            long createdTime = event.getCreatedTime();
            if ( 0 == createdTime )
            {
                // Don't have data yet... Saying "Now" is fine
                createdTime = currentTime;
            }

            GCardMember cardMember = _card.findMemberByMemberId(event.getCardMemberId());
            GUser user = null != cardMember ? userManager.findUserByUserId(cardMember.getUserId()) : null;

            Date date = new Date(createdTime);
            sb.append("Created: ").append((new SimpleDateFormat("MM/dd' 'HH:mm:ss")).format(date)).append('\n');
            sb.append("Member: ").append(null != user ? user.getNickname() : "").append('\n');
            sb.append("Type: ").append(event.getType()).append('\n');

            sb.append('\n');
        }

        if ( sb.length() >= 2 )
        {
            sb.delete(sb.length() - 2, sb.length() - 1);
        }

        _activitylist.setText(sb.toString());
    }

    private void showKickMembersDialog()
    {
        final List<CheckBox> checkBoxes = new ArrayList<>(_card.getMembers().length() - 1);
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setTitle(R.string.kick_members);
        alert.setMessage(R.string.select_members);

        LinearLayout checkList = new LinearLayout(getContext());
        checkList.setOrientation(LinearLayout.VERTICAL);

        GUserManager userManager = GlympseWrapper.instance().getGlympse().getUserManager();
        for ( GCardMember cardMember : _card.getMembers() )
        {
            // Skip the self member since there is a separate leave button
            if ( cardMember.isSelf() )
            {
                continue;
            }

            GUser user = userManager.findUserByUserId(cardMember.getUserId());
            CheckBox checkBox = new CheckBox(getContext());
            checkBox.setText(user.getNickname());
            checkBox.setTag(cardMember);
            checkList.addView(checkBox);
            checkBoxes.add(checkBox);
        }

        alert.setView(checkList);

        alert.setPositiveButton(R.string.kick_members, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                // Remove members that were selected
                for ( CheckBox checkBox : checkBoxes )
                {
                    if ( checkBox.isChecked() )
                    {
                        GCardMember cardMember = (GCardMember) checkBox.getTag();
                        _card.deleteMember(cardMember);
                    }
                }
            }
        });

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
            }
        });

        alert.show();
    }

    private void showLeaveCardDialog()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setTitle(R.string.leave_card);
        alert.setMessage(R.string.confirm_leave_card);

        alert.setPositiveButton(R.string.leave_card, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                // The method deleteMember() is used for removing any member including the self member
                _card.deleteMember(_card.getSelfMember());
                ((GlympseCardsDemoActivity) getActivity()).popFragment();
            }
        });

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
            }
        });

        alert.show();
    }

    private void showSendInviteDialog()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setTitle(R.string.send_invite);
        alert.setMessage(R.string.enter_number_to_invite);

        final EditText editText = new EditText(getContext());
        alert.setView(editText);

        alert.setPositiveButton(R.string.send_invite, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                String number = editText.getText().toString().trim();
                // Check to see if Glympse recognizes this string as a phone number
                if ( GlympseTools.guessInviteType(number) == GC.INVITE_TYPE_SMS )
                {
                    GInvite invite = GlympseFactory.createInvite(GC.INVITE_TYPE_SMS, "", number);
                    final GCardInvite cardInvite = GlympseFactory.createCardInvite(invite);

                    Dexter.withActivity(getActivity())
                            .withPermission(Manifest.permission.SEND_SMS)
                            .withListener(new PermissionListener() {
                                @Override public void onPermissionGranted(PermissionGrantedResponse response)
                                {
                                    _card.sendCardInvite(cardInvite);
                                }
                                @Override public void onPermissionDenied(PermissionDeniedResponse response) {/* ... */}
                                @Override public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {/* ... */}
                            }).check();
                }
            }
        });

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
            }
        });

        alert.show();
    }

    private RadioButton createRadioButton(String locationName, double latitude, double longitude)
    {
        RadioButton button = new RadioButton(getContext());
        button.setText(locationName);
        GPoiBuilder poiBuilder = GlympseFactory.createPoiBuilder();
        poiBuilder.setLabel("custom");
        poiBuilder.setName(locationName);
        poiBuilder.setLocation(GlympseFactory.createPlace(latitude, longitude, locationName));
        poiBuilder.setAddress(LibFactory.createAddress(null, null, null, null, null, null));
        button.setTag(GlympseFactory.createCardObjectPoi(poiBuilder.getPoi()));
        return button;
    }

    private void showCreatePoiDialog()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setTitle(R.string.create_poi);
        alert.setMessage(R.string.select_place);

        final RadioGroup radioGroup = new RadioGroup(getContext());
        radioGroup.setOrientation(RadioGroup.VERTICAL);

        radioGroup.addView(createRadioButton("Seattle", 47.6129432, -122.4821462));
        radioGroup.addView(createRadioButton("San Francisco", 37.7576948, -122.4726194));
        radioGroup.addView(createRadioButton("New York", 40.6974034, -74.1197635));
        radioGroup.addView(createRadioButton("Miami", 25.7823907, -80.2994983));

        alert.setView(radioGroup);

        alert.setPositiveButton(R.string.create_poi, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                // Find which poi was selected
                int radioButtonID = radioGroup.getCheckedRadioButtonId();
                View radioButton = radioGroup.findViewById(radioButtonID);
                GCardObjectPoi cardObjectPoi = (GCardObjectPoi) radioButton.getTag();

                // Create a card object from the selected poi
                _card.addObject(cardObjectPoi);
            }
        });

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
            }
        });

        alert.show();
    }

    private void showRequestLocationDialog()
    {
        final List<CheckBox> checkBoxes = new ArrayList<>(_card.getMembers().length() - 1);
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setTitle(R.string.request_location);
        alert.setMessage(R.string.select_members_or_request_all);

        LinearLayout checkList = new LinearLayout(getContext());
        checkList.setOrientation(LinearLayout.VERTICAL);

        GUserManager userManager = GlympseWrapper.instance().getGlympse().getUserManager();
        for ( GCardMember cardMember : _card.getMembers() )
        {
            if ( cardMember.isSelf() )
            {
                continue;
            }

            GUser user = userManager.findUserByUserId(cardMember.getUserId());
            CheckBox checkBox = new CheckBox(getContext());
            checkBox.setText(user.getNickname());
            checkBox.setTag(cardMember);
            checkList.addView(checkBox);
            checkBoxes.add(checkBox);
        }

        alert.setView(checkList);

        alert.setPositiveButton(R.string.request_from_members, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                GCardTicketBuilder cardTicketBuilder = GlympseFactory.createCardTicketBuilder(GC.INVITE_ASPECT_REQUEST);

                boolean hasRequestee = false;
                // For each selected member, add them to the request
                for ( CheckBox checkBox : checkBoxes )
                {
                    if ( checkBox.isChecked() )
                    {
                        hasRequestee = true;
                        GCardMember cardMember = (GCardMember) checkBox.getTag();
                        cardTicketBuilder.addCardMember(cardMember);
                    }
                }

                if ( hasRequestee )
                {
                    GTicket requestTicket = GlympseFactory.createTicket((int) (5 * Helpers.MS_PER_MINUTE), null, null);
                    cardTicketBuilder.setTicket(requestTicket);
                    _card.startRequesting(cardTicketBuilder.getCardTicket());
                }
            }
        });

        alert.setNeutralButton(R.string.request_all, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                // To request from all members we send the request without specifying members
                GCardTicketBuilder cardTicketBuilder = GlympseFactory.createCardTicketBuilder(GC.INVITE_ASPECT_REQUEST);
                GTicket requestTicket = GlympseFactory.createTicket((int) (5 * Helpers.MS_PER_MINUTE), null, null);
                cardTicketBuilder.setTicket(requestTicket);
                _card.startRequesting(cardTicketBuilder.getCardTicket());
            }
        });

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
            }
        });

        alert.show();
    }

    private void showShareLocationDialog()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setTitle(R.string.share_location);
        alert.setMessage(R.string.confirm_share);

        alert.setPositiveButton(R.string.share_location, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                // Create a ticket that is set to share for 5 minutes
                GTicket ticket = GlympseFactory.createTicket((int) (5 * Helpers.MS_PER_MINUTE), "Glympse Cards Demo", null);
                final GCardTicketBuilder cardTicketBuilder = GlympseFactory.createCardTicketBuilder(GC.INVITE_ASPECT_TICKET);
                cardTicketBuilder.setTicket(ticket);

                Dexter.withActivity(getActivity())
                        .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        .withListener(new PermissionListener() {
                            @Override public void onPermissionGranted(PermissionGrantedResponse response)
                            {
                                _card.startSharing(cardTicketBuilder.getCardTicket());
                            }
                            @Override public void onPermissionDenied(PermissionDeniedResponse response) {/* ... */}
                            @Override public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {/* ... */}
                        }).check();
            }
        });

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
            }
        });

        alert.show();
    }

    @Override
    public void eventsOccurred(GGlympse glympse, int listener, int events, Object obj)
    {
        if ( GE.LISTENER_CARD == listener )
        {
            if ( 0 != (events & (GE.CARD_MEMBER_ADDED | GE.CARD_MEMBER_REMOVED)) )
            {
                refreshMemberList();
            }
            else if ( 0 != (events & (GE.CARD_INVITE_CREATED | GE.CARD_INVITE_ADDED | GE.CARD_INVITE_REMOVED)) )
            {
                refreshInviteList();
            }
            else if ( 0 != (events & (GE.CARD_OBJECT_ADDED | GE.CARD_OBJECT_REMOVED | GE.CARD_OBJECT_UPDATED)) )
            {
                refreshObjectList();
            }
        }
        else if ( GE.LISTENER_CARD_ACTIVITY == listener )
        {
            refreshActivityList();
        }
        else if ( GE.LISTENER_CARD_MEMBER == listener )
        {
            refreshMemberList();
        }
    }
}
