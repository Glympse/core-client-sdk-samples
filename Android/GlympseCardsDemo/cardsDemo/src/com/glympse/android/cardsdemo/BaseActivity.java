package com.glympse.android.cardsdemo;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import java.util.Stack;

public abstract class BaseActivity extends AppCompatActivity
{
    protected Stack<Fragment> _fragments = new Stack<>();

    @Override public void onBackPressed()
    {
        if (_fragments.size() > 1)
        {
            popFragment();
        }
        else
        {
            finish();
        }
    }

    protected void addRemoveFragment(int truncateToDepth, Fragment fragment)
    {
        if ( (truncateToDepth < _fragments.size()) || null != fragment )
        {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

            boolean topFragmentVisible = !_fragments.isEmpty();

            while ( _fragments.size() > truncateToDepth )
            {
                fragmentTransaction.remove(_fragments.pop());

                topFragmentVisible = false;
            }

            if (null != fragment)
            {
                // If the top fragment is visible, then hide it before adding one on top of it.
                if ( topFragmentVisible )
                {
                    fragmentTransaction.hide(_fragments.peek());
                }

                _fragments.push(fragment);

                String name = fragment.getClass().getSimpleName() + "_" + _fragments.size();
                fragmentTransaction.add(R.id.fragment_container, fragment, name);
                if ( truncateToDepth != 0 )
                {
                    fragmentTransaction.addToBackStack(name);
                }

                topFragmentVisible = true;
            }

            if ( !_fragments.isEmpty() )
            {
                fragmentTransaction.show(_fragments.peek());
            }

            fragmentTransaction.commit();
        }
    }

    public void pushFragment(Fragment fragment)
    {
        addRemoveFragment(_fragments.size(), fragment);
    }

    protected void popFragment()
    {
        int size = _fragments.size();
        addRemoveFragment((size > 1) ? (size - 1) : 1, null);
    }
}
