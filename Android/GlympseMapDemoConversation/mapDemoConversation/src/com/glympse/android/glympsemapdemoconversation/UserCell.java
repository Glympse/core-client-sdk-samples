package com.glympse.android.glympsemapdemoconversation;

import com.glympse.android.api.GUser;
import com.glympse.android.ui.GLYAvatarView;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class UserCell extends LinearLayout
{
	public interface OnUserCellTappedListener
	{
		public void onUserCellTapped(UserCell userCell);
	}
	
	OnUserCellTappedListener _cellTappedListener;
	
	TextView _textViewNickname;
	GLYAvatarView _avatarView;
	GUser _user;
	
	public UserCell(Context context)
	{
		super(context);
		commonInit();
	}
    public UserCell(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        commonInit();
    }
    
    private void commonInit()
    {
    	LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	inflater.inflate(R.layout.user_cell, this, true);
    	
		_textViewNickname = (TextView)this.findViewById(R.id.textViewNickname);
		_avatarView = (GLYAvatarView)this.findViewById(R.id.avatarViewUser);
		
		this.setOnClickListener(new OnClickListener()
		{
			@Override public void onClick(View v)
			{
				if ( null != _cellTappedListener )
				{
					_cellTappedListener.onUserCellTapped(UserCell.this);
				}
			}
		});
		
		setActiveState(false);
    }
    
    public void setOnUserCellTappedListener(OnUserCellTappedListener listener)
    {
    	_cellTappedListener = listener;
    }
    
    public void setUser(GUser user)
    {
    	_user = user;
    	if( null != _textViewNickname )
    	{
    		String title = "-----";

    		if ( null != user.getNickname() )
    		{
    			title = user.getNickname();
    		}
    		
    		if( user.isSelf() )
    		{
    			title = "Me (" + title + ")";
    		}

    		_textViewNickname.setText(title);
    	}
    	
    	if( null != _avatarView )
    	{
    		_avatarView.attachImage(user.getAvatar());
    	}
    	
    }
    
    public GUser getUser()
    {
    	return _user;
    }

    public void setActiveState(boolean active)
    {
    	if (active)
    	{
    		this.setBackgroundColor(Color.argb(180, 255, 186, 0));
    	}
    	else
    	{
    		this.setBackgroundColor(Color.argb(180, 55, 55, 55));
    	}
    }


}
