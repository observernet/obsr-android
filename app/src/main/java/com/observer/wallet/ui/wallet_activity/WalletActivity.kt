package com.observer.wallet.ui.wallet_activity

import android.Manifest.permission.CAMERA
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import chain.BlockchainState
import com.github.clans.fab.FloatingActionMenu
import com.observer.wallet.R
import com.observer.wallet.data.api.GithubUpdateApi
import com.observer.wallet.service.IntentsConstants.*
import com.observer.wallet.ui.base.BaseDrawerActivity
import com.observer.wallet.ui.base.dialogs.SimpleTextDialog
import com.observer.wallet.ui.base.dialogs.SimpleTwoButtonsDialog
import com.observer.wallet.ui.qr_activity.QrActivity
import com.observer.wallet.ui.settings_backup_activity.SettingsBackupActivity
import com.observer.wallet.ui.transaction_request_activity.RequestActivity
import com.observer.wallet.ui.transaction_send_activity.SendActivity
import com.observer.wallet.ui.transaction_send_activity.SendActivity.*
import com.observer.wallet.ui.upgrade.UpgradeWalletActivity
import com.observer.wallet.utils.AnimationUtils
import com.observer.wallet.utils.DialogsUtil
import com.observer.wallet.utils.scanner.ScanActivity
import com.observer.wallet.utils.scanner.ScanActivity.INTENT_EXTRA_RESULT
import com.observer.wallet.utils.versionCompare
import global.CoinCoreContext
import global.ObsrRate
import global.exceptions.NoPeerConnectedException
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import org.obsrj.core.Transaction
import org.obsrj.uri.ObsrURI
import java.math.BigDecimal

/**
 * Created by Neoperol on 5/11/17.
 */

class WalletActivity : BaseDrawerActivity() {

    private var container_txs: View? = null

    private var txt_value: TextView? = null
    private var txt_unnavailable: TextView? = null
    private var txt_local_currency: TextView? = null
    private var txt_watch_only: TextView? = null
    private var view_background: View? = null
    private var container_syncing: View? = null
    private var obsrRate: ObsrRate? = null
    private var txsFragment: TransactionsFragmentBase? = null

    // Receiver
    private var walletBroadcastManager: LocalBroadcastManager? = null

    private val obsrServiceFilter = IntentFilter(ACTION_NOTIFICATION)
    private val obsrServiceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == ACTION_NOTIFICATION) {
                if (intent.getStringExtra(INTENT_BROADCAST_DATA_TYPE) == INTENT_BROADCAST_DATA_ON_COIN_RECEIVED) {
                    // Check if the app is on foreground to update the view.
                    if (!isOnForeground) return
                    updateBalance()
                    txsFragment!!.refresh()
                }
            }

        }
    }

    override fun beforeCreate() {
        /*
        if (!appConf.isAppInit()){
            Intent intent = new Intent(this, SplashActivity.class);
            startActivity(intent);
            finish();
        }
        // show report dialog if something happen with the previous process
        */
        walletBroadcastManager = LocalBroadcastManager.getInstance(this)
    }

    override fun onCreateView(savedInstanceState: Bundle?, container: ViewGroup) {
        setTitle(R.string.my_wallet)
        val root = layoutInflater.inflate(R.layout.fragment_wallet, container)
        val containerHeader = layoutInflater.inflate(R.layout.fragment_coin_amount, header_container)
        header_container.visibility = View.VISIBLE
        txt_value = containerHeader.findViewById<View>(R.id.coinValue) as TextView
        txt_unnavailable = containerHeader.findViewById<View>(R.id.txt_unnavailable) as TextView
        container_txs = root.findViewById(R.id.container_txs)
        txt_local_currency = containerHeader.findViewById<View>(R.id.txt_local_currency) as TextView
        txt_watch_only = containerHeader.findViewById<View>(R.id.txt_watch_only) as TextView
        view_background = root.findViewById(R.id.view_background)
        container_syncing = root.findViewById(R.id.container_syncing)
        // Open Send
        root.findViewById<View>(R.id.fab_add).setOnClickListener(View.OnClickListener { v ->
            if (obsrModule.isWalletWatchOnly) {
                Toast.makeText(v.context, R.string.error_watch_only_mode, Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            startActivity(Intent(v.context, SendActivity::class.java))
        })
        root.findViewById<View>(R.id.fab_request)?.setOnClickListener { v -> startActivity(Intent(v.context, RequestActivity::class.java)) }

        val floatingActionMenu = root.findViewById<View>(R.id.fab_menu) as FloatingActionMenu
        floatingActionMenu.setOnMenuToggleListener { opened ->
            if (opened) {
                AnimationUtils.fadeInView(view_background!!, 200)
            } else {
                AnimationUtils.fadeOutGoneView(view_background!!, 200)
            }
        }

        txsFragment = supportFragmentManager.findFragmentById(R.id.transactions_fragment) as TransactionsFragmentBase
        checkUpdate()
    }

    override fun onResume() {
        super.onResume()
        // to check current activity in the navigation drawer
        setNavigationMenuItemChecked(0)

        init()

        // register
        walletBroadcastManager?.registerReceiver(obsrServiceReceiver, obsrServiceFilter)

        updateState()
        updateBalance()

        // check if this wallet need an update:
        try {
            if (obsrModule.isBip32Wallet && obsrModule.isSyncWithNode) {
                if (!obsrModule.isWalletWatchOnly && obsrModule.availableBalanceCoin.isGreaterThan(Transaction.DEFAULT_TX_FEE)) {
                    val intent = UpgradeWalletActivity.createStartIntent(
                            this,
                            getString(R.string.upgrade_wallet),
                            "An old wallet version with bip32 key was detected, in order to upgrade the wallet your coins are going to be sweeped" +
                                    " to a new wallet with bip44 account.\n\nThis means that your current mnemonic code and" +
                                    " backup file are not going to be valid anymore, please write the mnemonic code in paper " +
                                    "or export the backup file again to be able to backup your coins." +
                                    "\n\nPlease wait and not close this screen. The upgrade + blockchain sychronization could take a while."
                                    + "\n\nTip: If this screen is closed for user's mistake before the upgrade is finished you can find two backups files in the 'Download' folder" +
                                    " with prefix 'old' and 'upgrade' to be able to continue the restore manually."
                                    + "\n\nThanks!",
                            "sweepBip32"
                    )
                    startActivity(intent)
                }
            }
        } catch (e: NoPeerConnectedException) {
            e.printStackTrace()
        }

    }

    private fun updateState() {
        txt_watch_only!!.visibility = if (obsrModule.isWalletWatchOnly) View.VISIBLE else View.GONE
    }

    private fun init() {
        // Start service if it's not started.
        coinApplication.startCoinService()

        if (!coinApplication.appConf.hasBackup()) {
            val now = System.currentTimeMillis()
            if (coinApplication.lastTimeRequestedBackup + 1800000L < now) {
                coinApplication.setLastTimeBackupRequested(now)
                val reminderDialog = DialogsUtil.buildSimpleTwoBtnsDialog(
                        this,
                        getString(R.string.reminder_backup),
                        getString(R.string.reminder_backup_body),
                        object : SimpleTwoButtonsDialog.SimpleTwoBtnsDialogListener {
                            override fun onRightBtnClicked(dialog: SimpleTwoButtonsDialog) {
                                startActivity(Intent(this@WalletActivity, SettingsBackupActivity::class.java))
                                dialog.dismiss()
                            }

                            override fun onLeftBtnClicked(dialog: SimpleTwoButtonsDialog) {
                                dialog.dismiss()
                            }
                        }
                )
                reminderDialog.setLeftBtnText(getString(R.string.button_dismiss))
                reminderDialog.setLeftBtnTextColor(Color.BLACK)
                reminderDialog.setRightBtnText(getString(R.string.button_ok))
                reminderDialog.show()
            }
        }
    }

    private fun checkUpdate(){
        if (!CoinCoreContext.IS_TEST) {
            compositeDisposable += GithubUpdateApi.getGithubUpdateData().observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        if (it.prerelease == false && versionCompare(it.tagName, coinApplication.versionName) > 0) {
                            showUpdateDialog(it.tagName)
                        }
                    }, { it.printStackTrace() })
        }
    }

    private fun showUpdateDialog(versionName: String) {
        val succedDialog = DialogsUtil.buildSimpleTextDialog(
                this,
                getString(R.string.update_dialog_title),
                getString(R.string.update_dialog_content, versionName)
        )
        succedDialog.setOkBtnBackgroundColor(ContextCompat.getColor(this, R.color.lightGreen))
        succedDialog.setOkBtnClickListener { }
        succedDialog.show(fragmentManager, "update_dialog")
    }
    override fun onStop() {
        super.onStop()
        // unregister
        //localBroadcastManager.unregisterReceiver(localReceiver);
        localBroadcastManager?.unregisterReceiver(obsrServiceReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_qr) {
            startActivity(Intent(this, QrActivity::class.java))
            return true
        } else if (item.itemId == R.id.action_scan) {
            if (!checkPermission(CAMERA)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val permsRequestCode = 200
                    val perms = arrayOf("android.permission.CAMERA")
                    requestPermissions(perms, permsRequestCode)
                }
            }
            startActivityForResult(Intent(this, ScanActivity::class.java), SCANNER_RESULT)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == SCANNER_RESULT) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    val address = data.getStringExtra(INTENT_EXTRA_RESULT)
                    val usedAddress: String
                    if (obsrModule.chechAddress(address)) {
                        usedAddress = address
                    } else {
                        val obsrURI = ObsrURI(address)
                        usedAddress = obsrURI.address!!.toBase58()
                        val amount = obsrURI.amount
                        if (amount != null) {
                            val memo = obsrURI.message
                            val text = StringBuilder()
                            text.append(getString(R.string.amount)).append(": ").append(amount.toFriendlyString())
                            if (memo != null) {
                                text.append("\n").append(getString(R.string.description)).append(": ").append(memo)
                            }

                            val dialogFragment = DialogsUtil.buildSimpleTextDialog(this,
                                    getString(R.string.payment_request_received),
                                    text.toString())
                                    .setOkBtnClickListener { v ->
                                        val intent = Intent(v.context, SendActivity::class.java)
                                        intent.putExtra(INTENT_ADDRESS, usedAddress)
                                        intent.putExtra(INTENT_EXTRA_TOTAL_AMOUNT, amount)
                                        intent.putExtra(INTENT_MEMO, memo)
                                        startActivity(intent)
                                    }
                            dialogFragment.setImgAlertRes(R.drawable.ic_send_action)
                            dialogFragment.setAlignBody(SimpleTextDialog.Align.LEFT)
                            dialogFragment.setImgAlertRes(R.drawable.ic_fab_recieve)
                            dialogFragment.show(fragmentManager, "payment_request_dialog")
                            return
                        }

                    }
                    DialogsUtil.showCreateAddressLabelDialog(this, usedAddress)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Bad address", Toast.LENGTH_LONG).show()
                }

            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    private fun checkPermission(permission: String): Boolean {
        val result = ContextCompat.checkSelfPermission(applicationContext, permission)

        return result == PackageManager.PERMISSION_GRANTED
    }


    private fun updateBalance() {
        val availableBalance = obsrModule.availableBalanceCoin
        txt_value!!.text = if (!availableBalance.isZero) availableBalance.toFriendlyString() else "0 OBSR"
        val unnavailableBalance = obsrModule.unnavailableBalanceCoin
        txt_unnavailable!!.text = if (!unnavailableBalance.isZero) unnavailableBalance.toFriendlyString() else "0 OBSR"
        if (obsrRate == null)
            obsrRate = obsrModule.getRate(coinApplication.appConf.selectedRateCoin)
        if (obsrRate != null) {
            txt_local_currency!!.text = (coinApplication.centralFormats.format(
                    BigDecimal(availableBalance.getValue() * obsrRate!!.rate.toDouble()).movePointLeft(8)
            )
                    + " " + obsrRate!!.code)
        } else {
            txt_local_currency!!.text = "0"
        }
    }

    override fun onBlockchainStateChange() {
        if (blockchainState == BlockchainState.SYNCING) {
            AnimationUtils.fadeInView(container_syncing!!, 500)
        } else if (blockchainState == BlockchainState.SYNC) {
            AnimationUtils.fadeOutGoneView(container_syncing!!, 500)
        } else if (blockchainState == BlockchainState.NOT_CONNECTION) {
            AnimationUtils.fadeInView(container_syncing!!, 500)
        }
    }

    companion object {

        private val SCANNER_RESULT = 122
    }
}
