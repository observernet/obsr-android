package com.observer.wallet.ui.transaction_detail_activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.observer.wallet.R;
import com.observer.wallet.ui.base.BaseActivity;

/**
 * Created by Neoperol on 6/9/17.
 */

public class TransactionDetailActivity extends BaseActivity {

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        getLayoutInflater().inflate(R.layout.transaction_detail_main, container);
        setTitle("Transaction Detail");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /*MenuItem menuItem = menu.add(0,0,0,R.string.explorer);
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);*/
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*switch (item.getItemId()) {
            case 0:
                //Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com"));
                //startActivity(browserIntent);
                return true;
        }*/
        return super.onOptionsItemSelected(item);
    }
}
