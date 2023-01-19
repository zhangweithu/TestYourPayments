package com.azurelan.testyourpayments.mainactivity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.azurelan.testyourpayments.R
import com.azurelan.testyourpayments.externalvisiblelog.ExternalVisibleLogActivity
import com.azurelan.testyourpayments.shared.billing.BillingActionsHelper
import com.azurelan.testyourpayments.shared.billing.BillingProduct
import com.azurelan.testyourpayments.shared.billing.BillingUtils
import com.azurelan.testyourpayments.shared.externalvisiblelog.ExternallyVisibleLog
import com.azurelan.testyourpayments.shared.utils.AzureLanLog
import com.azurelan.testyourpayments.shared.viewmodels.BillingViewModel

/**
 * Loads [MainFragment].
 */
class MainActivity : AppCompatActivity(),
    BillingActionsHelper.OnPurchaseConsumedContinueRunnable,
BillingActionsHelper.OnPurchaseAckedContinueRunnable,
BillingActionsHelper.OnInitializeQueryCompleteRunnable  {

    private var loadingIndicator: View? = null
    private var isLoading = false
    private lateinit var viewModel: BillingViewModel
    private lateinit var billingActionsHelper: BillingActionsHelper

    private var gardener: Button? = null
    private var tree: Button? = null
    private var rose: Button? = null
    private var weekly: Button? = null
    private var viewLogs: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        AzureLanLog.d("MA: onCreate. Self: %s", this)
        billingActionsHelper = BillingActionsHelper(this)

        viewModel = ViewModelProvider(this)[BillingViewModel::class.java]
        isLoading = true
        loadingIndicator = findViewById(R.id.loading_indicator)
        loadingIndicator?.visibility = View.VISIBLE

        gardener = findViewById(R.id.gardener)
        tree = findViewById(R.id.tree)
        rose = findViewById(R.id.rose)
        weekly = findViewById(R.id.weekly)
        viewLogs = findViewById(R.id.view_logs)

        billingActionsHelper.initialize()
        billingActionsHelper.registerOnPurchaseConsumedContinueRunnable(this)
        billingActionsHelper.registerOnPurchaseAckedContinueRunnable(this)
        billingActionsHelper.registerOnInitializeQueryCompleteRunnable(this)

        gardener?.setOnClickListener {
            billingActionsHelper.launchPurchaseFlowFor(BillingProduct.GARDENER)
        }
        tree?.setOnClickListener {
            billingActionsHelper.launchPurchaseFlowFor(BillingProduct.TREE)
        }
        rose?.setOnClickListener {
            billingActionsHelper.launchPurchaseFlowFor(BillingProduct.ROSE)
        }
        weekly?.setOnClickListener {
            billingActionsHelper.launchPurchaseFlowFor(BillingProduct.WEEKLY_JOB)
        }

        viewLogs?.setOnClickListener {
            startActivity(
                Intent(this, ExternalVisibleLogActivity::class.java)
            )
        }
        updateButtonTexts()
    }

    override fun onStart() {
        super.onStart()
        AzureLanLog.d("MA: onStart. Self: %s", this)

        if (!billingActionsHelper.hasInitiatedBilling()) {
            logEvent("MA: initializing Billing")
            Handler(Looper.getMainLooper()).post {
                billingActionsHelper.initBillingIfApplicable(
                    viewModel,
                    isLoading,
                    this::handleBillingExceptionOnLoading,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        billingActionsHelper.onAppResume()
    }

    override fun onPurchaseAckedAndConsumedContinue() {
        recreateUI()
    }

    override fun onPurchaseAckedContinue() {
        recreateUI()
    }

    private fun recreateUI() {
        Handler(Looper.getMainLooper()).post {
            logEvent("MA: recreate UI")
            isLoading = true
            loadingIndicator?.visibility = View.VISIBLE
            recreate()
        }
    }

    override fun onInitializeQueryComplete() {
        updateBehavior()
    }

    private fun updateBehavior() {
        isLoading = false
        loadingIndicator?.visibility = View.GONE
        billingActionsHelper.clearOnBillingConnectFailedCallback()
        updateButtonTexts()
    }

    private fun updateButtonTexts() {
        if (BillingUtils.isGardenerActive()) {
            gardener?.text = getString(
                R.string.format_with_state,
                getString(R.string.be_gardener),
                getString(R.string.active),
            )
        } else {
            gardener?.text = getString(R.string.be_gardener)
        }
        if (BillingUtils.isWeeklySubActive()) {
            weekly?.text = getString(
                R.string.format_with_state,
                getString(R.string.weekly_sub),
                getString(R.string.active),
            )
        } else {
            weekly?.text = getString(R.string.weekly_sub)
        }
        tree?.text = getString(
            R.string.format_with_state,
            getString(R.string.buy_a_tree),
            billingActionsHelper.getTreeCount().toString(),
        )
        rose?.text = getString(
            R.string.format_with_state,
            getString(R.string.buy_a_rose),
            billingActionsHelper.getRoseCount().toString(),
        )
    }

    override fun onDestroy() {
        billingActionsHelper.destroy()
        super.onDestroy()
    }

    private fun handleBillingExceptionOnLoading() {
        // Do sth about error loading
        isLoading = false
        loadingIndicator?.visibility = View.GONE
    }

    private fun logEvent(msg: String) {
        AzureLanLog.i(msg)
        ExternallyVisibleLog.appendNewLog(msg)
    }
}