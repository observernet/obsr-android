package com.obsr.wallet.ui.base;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;

import com.obsr.wallet.CoinApplication;
import global.ObsrModule;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Created by furszy on 6/29/17.
 */

public class BaseFragment extends Fragment {

    protected CoinApplication coinApplication;
    protected ObsrModule obsrModule;
    public CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        coinApplication = coinApplication.getInstance();
        obsrModule = coinApplication.getModule();
    }

    protected boolean checkPermission(String permission) {
        int result = ContextCompat.checkSelfPermission(getActivity(),permission);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
