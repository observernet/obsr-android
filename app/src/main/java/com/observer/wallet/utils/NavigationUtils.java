package com.observer.wallet.utils;

import android.app.Activity;
import android.content.Intent;

import com.observer.wallet.ui.wallet_activity.WalletActivity;

/**
 * Created by furszy on 10/19/17.
 */

public class NavigationUtils {

    public static void goBackToHome(Activity activity){
        Intent upIntent = new Intent(activity,WalletActivity.class);
        upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(upIntent);
        activity.finish();
    }

}
