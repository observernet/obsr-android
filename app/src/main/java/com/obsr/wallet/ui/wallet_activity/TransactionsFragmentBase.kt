package com.obsr.wallet.ui.wallet_activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.obsr.wallet.R
import com.obsr.wallet.ui.base.BaseRecyclerFragment
import com.obsr.wallet.ui.base.tools.adapter.BaseRecyclerAdapter
import com.obsr.wallet.ui.base.tools.adapter.BaseRecyclerViewHolder
import com.obsr.wallet.ui.base.tools.adapter.ListItemListeners
import com.obsr.wallet.ui.transaction_detail_activity.FragmentTxDetail.IS_DETAIL
import com.obsr.wallet.ui.transaction_detail_activity.FragmentTxDetail.TX_WRAPPER
import com.obsr.wallet.ui.transaction_detail_activity.TransactionDetailActivity
import com.obsr.wallet.utils.TxUtils.getAddressOrContact
import global.ObsrRate
import global.wrappers.TransactionWrapper
import org.obsrj.core.Coin
import org.obsrj.utils.MonetaryFormat
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.Collections
import java.util.Comparator

/**
 * Created by furszy on 6/29/17.
 */

class TransactionsFragmentBase : BaseRecyclerFragment<TransactionWrapper>() {

    private var obsrRate: ObsrRate? = null
    private val coinFormat = MonetaryFormat.BTC
    private val scale = 3

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        setEmptyView(R.drawable.img_transaction_empty)
        setEmptyText(getString(R.string.no_transactions))
        setEmptyTextColor(Color.parseColor("#cccccc"))
        return view
    }

    override fun onLoading(): List<TransactionWrapper> {
        val list = obsrModule.listTx()
        Collections.sort(list, Comparator { o1, o2 ->
            if (o1.transaction.updateTime.time == o2.transaction.updateTime.time)
                return@Comparator 0
            if (o1.transaction.updateTime.time > o2.transaction.updateTime.time) -1 else 1
        })
        return list
    }

    override fun initAdapter(): BaseRecyclerAdapter<TransactionWrapper, out BaseRecyclerViewHolder> {
        val adapter = object : BaseRecyclerAdapter<TransactionWrapper, TransactionViewHolderBase>(activity) {
            override fun createHolder(itemView: View, type: Int): TransactionViewHolderBase {
                return TransactionViewHolderBase(itemView)
            }

            override fun getCardViewResource(type: Int): Int {
                return R.layout.transaction_row
            }

            override fun bindHolder(holder: TransactionViewHolderBase, data: TransactionWrapper, position: Int) {
                val amount = data.amount.toFriendlyString()
                if (amount.length <= 10) {
                    holder.txt_scale.visibility = View.GONE
                    holder.amount.text = amount
                } else {
                    // format amount
                    holder.txt_scale.visibility = View.VISIBLE
                    holder.amount.text = parseToCoinWith4Decimals(data.amount.toPlainString()).toFriendlyString()
                }

                var localCurrency: String? = null
                if (obsrRate != null) {
                    localCurrency = (coinApplication.centralFormats.format(
                            BigDecimal(data.amount.getValue() * obsrRate!!.rate.toDouble()).movePointLeft(8)
                    )
                            + " " + obsrRate!!.code)
                    holder.amountLocal.text = localCurrency
                    holder.amountLocal.visibility = View.VISIBLE
                } else {
                    holder.amountLocal.visibility = View.INVISIBLE
                }

                val memo = data.transaction.memo
                holder.description.text = memo ?: "No description"
                if (data.isSent) {
                    //holder.cv.setBackgroundColor(Color.RED);Color.GREEN
                    holder.imageView.setImageResource(R.drawable.ic_transaction_send)
                    holder.amount.setTextColor(ContextCompat.getColor(context, R.color.red))
                } else if (data.isZcSpend) {
                    holder.imageView.setImageResource(R.drawable.ic_transaction_incognito)
                    holder.amount.setTextColor(ContextCompat.getColor(context, R.color.green))
                } else if (!data.isStake) {
                    holder.imageView.setImageResource(R.drawable.ic_transaction_receive)
                    holder.amount.setTextColor(ContextCompat.getColor(context, R.color.green))
                } else {
                    holder.imageView.setImageResource(R.drawable.ic_transaction_mining)
                    holder.amount.setTextColor(ContextCompat.getColor(context, R.color.green))
                }
                holder.title.text = getAddressOrContact(obsrModule, data)

                /*if (data.getOutputLabels()!=null && !data.getOutputLabels().isEmpty()){
                    AddressLabel contact = data.getOutputLabels().get(0);
                    if (contact!=null) {
                        if (contact.getName() != null)
                            holder.title.setText(contact.getName());
                        else
                            holder.title.setText(contact.getAddresses().get(0));
                    }else {
                        holder.title.setText(data.getTransaction().getOutput(0).getScriptPubKey().getToAddress(obsrModule.getConf().getNetworkParams()).toBase58());
                    }
                }else {
                    holder.title.setText(data.getTransaction().getOutput(0).getScriptPubKey().getToAddress(obsrModule.getConf().getNetworkParams()).toBase58());
                }*/


            }
        }
        adapter.setListEventListener(object : ListItemListeners<TransactionWrapper> {
            override fun onItemClickListener(data: TransactionWrapper, position: Int) {
                val bundle = Bundle()
                bundle.putSerializable(TX_WRAPPER, data)
                bundle.putBoolean(IS_DETAIL, true)
                val intent = Intent(activity, TransactionDetailActivity::class.java)
                intent.putExtras(bundle)
                startActivity(intent)
            }

            override fun onLongItemClickListener(data: TransactionWrapper, position: Int) {

            }
        })
        return adapter
    }

    override fun onResume() {
        super.onResume()
        obsrRate = obsrModule.getRate(coinApplication.appConf.selectedRateCoin)
    }

    /**
     * Converts to a coin with max. 4 decimal places. Last place gets rounded.
     * 0.01234 -> 0.0123
     * 0.01235 -> 0.0124
     *
     * @param input
     * @return
     */
    fun parseToCoinWith4Decimals(input: String?): Coin {
        try {
            return Coin.valueOf(BigDecimal(parseToCoin(cleanInput(input)).value).setScale(-scale - 1,
                    BigDecimal.ROUND_HALF_UP).setScale(scale + 1).toBigInteger().toLong())
        } catch (t: Throwable) {
            if (input != null && input.length > 0)
                logger.warn("Exception at parseToCoinWith4Decimals: " + t.toString())
            return Coin.ZERO
        }

    }

    fun parseToCoin(input: String?): Coin {
        return if (input != null && input.length > 0) {
            try {
                coinFormat.parse(cleanInput(input)!!)
            } catch (t: Throwable) {
                logger.warn("Exception at parseToBtc: " + t.toString())
                Coin.ZERO
            }

        } else {
            Coin.ZERO
        }
    }

    private fun cleanInput(input: String?): String? {
        var input = input
        input = input!!.replace(",", ".")
        // don't use String.valueOf(Double.parseDouble(input)) as return value as it gives scientific
        // notation (1.0E-6) which screw up coinFormat.parse

        java.lang.Double.parseDouble(input)
        return input
    }

    companion object {

        private val logger = LoggerFactory.getLogger(TransactionsFragmentBase::class.java)
    }
}
