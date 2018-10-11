package com.obsr.wallet.ui.settings_rates;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.obsr.wallet.R;
import com.obsr.wallet.ui.base.BaseRecyclerFragment;
import com.obsr.wallet.ui.base.tools.adapter.BaseRecyclerAdapter;
import com.obsr.wallet.ui.base.tools.adapter.BaseRecyclerViewHolder;
import com.obsr.wallet.ui.base.tools.adapter.ListItemListeners;

import java.util.List;

import global.ObsrRate;

/**
 * Created by furszy on 7/2/17.
 */

public class RatesFragment extends BaseRecyclerFragment<ObsrRate> implements ListItemListeners<ObsrRate> {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        setEmptyText("No rate available");
        setEmptyTextColor(Color.parseColor("#cccccc"));
        return view;
    }

    @Override
    protected List<ObsrRate> onLoading() {
        return obsrModule.listRates();
    }

    @Override
    protected BaseRecyclerAdapter<ObsrRate, ? extends CoinRateHolder> initAdapter() {
        BaseRecyclerAdapter<ObsrRate, CoinRateHolder> adapter = new BaseRecyclerAdapter<ObsrRate, CoinRateHolder>(getActivity()) {
            @Override
            protected CoinRateHolder createHolder(View itemView, int type) {
                return new CoinRateHolder(itemView,type);
            }

            @Override
            protected int getCardViewResource(int type) {
                return R.layout.rate_row;
            }

            @Override
            protected void bindHolder(CoinRateHolder holder, ObsrRate data, int position) {
                holder.txt_name.setText(data.getCode());
                if (list.get(0).getCode().equals(data.getCode()))
                    holder.view_line.setVisibility(View.GONE);
            }
        };
        adapter.setListEventListener(this);
        return adapter;
    }

    @Override
    public void onItemClickListener(ObsrRate data, int position) {
        coinApplication.getAppConf().setSelectedRateCoin(data.getCode());
        Toast.makeText(getActivity(),R.string.rate_selected,Toast.LENGTH_SHORT).show();
        getActivity().onBackPressed();
    }

    @Override
    public void onLongItemClickListener(ObsrRate data, int position) {

    }

    private  class CoinRateHolder extends BaseRecyclerViewHolder{

        private TextView txt_name;
        private View view_line;

        protected CoinRateHolder(View itemView, int holderType) {
            super(itemView, holderType);
            txt_name = (TextView) itemView.findViewById(R.id.txt_name);
            view_line = itemView.findViewById(R.id.view_line);
        }
    }
}
