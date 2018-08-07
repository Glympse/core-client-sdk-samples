package com.glympse.android.cardsdemo;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.glympse.android.api.GC;
import com.glympse.android.api.GCard;
import com.glympse.android.api.GCardManager;
import com.glympse.android.api.GE;
import com.glympse.android.api.GEventListener;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GlympseFactory;

import java.util.LinkedList;
import java.util.List;

public class FragmentCardsHome extends Fragment implements GEventListener
{
    private FloatingActionButton _fab;
    private LinkedList<CardItemViewModel> _cardList;
    private CardRecyclerAdapter _cardAdapter;
    private RecyclerView _cardRecyclerView;

    public static Fragment newInstance()
    {
        return new FragmentCardsHome();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_cards_home, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        _fab = (FloatingActionButton) view.findViewById(R.id.fab);
        _fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                showNewCardDialog();
            }
        });

        _cardList = new LinkedList<>();
        _cardAdapter = new CardRecyclerAdapter(_cardList);
        _cardRecyclerView = (RecyclerView) getView().findViewById(R.id.recycler_view);
        _cardRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        _cardRecyclerView.setAdapter(_cardAdapter);

        refreshCardList();

        GlympseWrapper.instance().getGlympse().getCardManager().addListener(this);
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();

        GlympseWrapper.instance().getGlympse().getCardManager().removeListener(this);
    }

    @Override
    public void onHiddenChanged(boolean hidden)
    {
        super.onHiddenChanged(hidden);

        if ( !hidden )
        {
            refreshCardList();
        }
    }

    @Override
    public void eventsOccurred(GGlympse glympse, int listener, int events, Object obj)
    {
        if ( GE.LISTENER_CARDS == listener )
        {
            if ( 0 != ( events & (GE.CARD_MANAGER_CARD_ADDED | GE.CARD_MANAGER_CARD_REMOVED | GE.CARD_MANAGER_SYNCED) ) )
            {
                refreshCardList();
            }
        }
    }

    private void showNewCardDialog()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        final EditText edittext = new EditText(getContext());
        alert.setMessage(R.string.name_card);
        alert.setTitle(R.string.create_a_card);

        alert.setView(edittext);

        alert.setPositiveButton(R.string.create, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                String cardName = edittext.getText().toString();
                createNewCard(cardName);
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

    private void createNewCard(String cardname)
    {
        if ( null == cardname )
        {
            return;
        }

        GCardManager cardManager = GlympseWrapper.instance().getGlympse().getCardManager();
        if ( null != cardManager )
        {
            // Create a card using the type id CARD_ID_PRIVATE_GROUP (currently the only supported card type)
            GCard card = GlympseFactory.createCard(GC.CARD_ID_PRIVATE_GROUP(), cardname);
            cardManager.createCard(card);
        }
    }

    private void refreshCardList()
    {
        _cardList.clear();

        GCardManager cardManager = GlympseWrapper.instance().getGlympse().getCardManager();
        for ( GCard card : cardManager.getCards() )
        {
            _cardList.add(new CardItemViewModel(card));
        }

        _cardAdapter.notifyDataSetChanged();
    }

    private class CardRecyclerAdapter extends RecyclerView.Adapter<CardItemViewHolder>
    {
        private List<CardItemViewModel> _cards;

        public CardRecyclerAdapter(List<CardItemViewModel> members)
        {
            _cards = members;
        }

        @Override
        public CardItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_card, parent, false);
            return new CardItemViewHolder(v);
        }

        @Override
        public void onBindViewHolder(CardItemViewHolder holder, int position)
        {
            final CardItemViewModel item = _cards.get(position);
            holder.itemView.setTag(item);
            holder.replaceView(item);
        }

        @Override
        public int getItemCount()
        {
            return _cards.size();
        }
    }

    private class CardItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        private final TextView _name;

        public CardItemViewHolder(View itemView)
        {
            super(itemView);

            _name = (TextView) itemView.findViewById(R.id.name);

            itemView.setOnClickListener(this);
        }

        public void replaceView(CardItemViewModel viewModel)
        {
            _name.setText(viewModel._name);
        }

        @Override
        public void onClick(View view)
        {
            CardItemViewModel tag = (CardItemViewModel) view.getTag();
            ((GlympseCardsDemoActivity) getActivity()).pushFragment(FragmentCardDetail.newInstance(tag._card.getId()));
        }
    }

    public class CardItemViewModel
    {
        private final GCard _card;
        private final String _name;

        public CardItemViewModel(GCard card)
        {
            _card = card;
            _name = card.getName();
        }
    }
}
