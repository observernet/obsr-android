package com.observer.wallet.ui.splash_activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.observer.wallet.R
import com.observer.wallet.CoinApplication
import com.observer.wallet.ui.pincode_activity.PincodeActivity
import com.observer.wallet.ui.start_activity.StartActivity
import com.observer.wallet.ui.wallet_activity.WalletActivity
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

/**
 * Created by Neoperol on 6/13/17.
 */

class SplashActivity : AppCompatActivity() {
    val REQ_CODE_CHECK_PIN = 660

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Observable.timer(1500, TimeUnit.MILLISECONDS).subscribe({
            jump()
        }, {})
    }

    private fun jump() {
        if (CoinApplication.getInstance().appConf.isAppInit) {
            // startup check pin
            val intent = Intent(this@SplashActivity, PincodeActivity::class.java)
            intent.putExtra(PincodeActivity.CHECK_PIN, true)
            startActivityForResult(intent, REQ_CODE_CHECK_PIN)
        } else {
            // Jump to your Next Activity or MainActivity
            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQ_CODE_CHECK_PIN) {
            if (resultCode == Activity.RESULT_OK) {
                val intent = Intent(this, WalletActivity::class.java)
                startActivity(intent)
            }
            finish()
        }
    }
}
