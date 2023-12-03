package com.azurelan.testyourpayments.mainactivity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.azurelan.testyourpayments.R
import com.azurelan.testyourpayments.externalvisiblelog.ExternalVisibleLogActivity
import com.azurelan.testyourpayments.shared.R as sharedR
import com.azurelan.testyourpayments.shared.billing.BillingActionsHelper
import com.azurelan.testyourpayments.shared.billing.BillingProduct
import com.azurelan.testyourpayments.shared.billing.BillingUtils
import com.azurelan.testyourpayments.shared.externalvisiblelog.ExternallyVisibleLog
import com.azurelan.testyourpayments.shared.utils.AzureLanLog
import com.azurelan.testyourpayments.shared.utils.ContactUtils
import com.azurelan.testyourpayments.shared.viewmodels.BillingViewModel

/**
 * Loads [MainFragment].
 */
class MainActivity : AppCompatActivity(),
    BillingActionsHelper.OnPurchaseConsumedContinueRunnable,
BillingActionsHelper.OnPurchaseAckedContinueRunnable,
BillingActionsHelper.OnInitializeQueryCompleteRunnable,
BillingUtils.UiActions {

    private var loadingIndicator: View? = null
    private var isLoading = false
    private lateinit var viewModel: BillingViewModel
    private lateinit var billingActionsHelper: BillingActionsHelper

    private var gardener: Button? = null
    private var tree: Button? = null
    private var rose: Button? = null
    private var weekly: Button? = null
    private var viewLogs: Button? = null
    private var contactButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        AzureLanLog.d("MA: onCreate. Self: %s", this)
        billingActionsHelper = BillingActionsHelper(this)

        viewModel = ViewModelProvider(this)[BillingViewModel::class.java]
        loadingIndicator = findViewById(R.id.loading_indicator)
        uiStartLoading()

        gardener = findViewById(R.id.gardener)
        tree = findViewById(R.id.tree)
        rose = findViewById(R.id.rose)
        weekly = findViewById(R.id.weekly)
        viewLogs = findViewById(R.id.view_logs)
        contactButton = findViewById(R.id.contact_us)

        billingActionsHelper.initialize()
        billingActionsHelper.registerOnPurchaseConsumedContinueRunnable(this)
        billingActionsHelper.registerOnPurchaseAckedContinueRunnable(this)
        billingActionsHelper.registerOnInitializeQueryCompleteRunnable(this)
        billingActionsHelper.registerUiActions(this)

        gardener?.setOnClickListener {
            billingActionsHelper.handleGardenerClick()
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
        contactButton?.setOnClickListener {
            ContactUtils.contactByEmail(
                this,
                com.azurelan.testyourpayments.BuildConfig.VERSION_CODE,
                ContactUtils.FormFactor.TV)
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
        // reinitButtonTexts()
    }

    override fun onPurchaseAckedContinue() {
        recreateUI()
        // reinitButtonTexts()
    }

    private fun reinitButtonTexts() {
        Handler(Looper.getMainLooper()).post {
            logEvent("MA: update display text")
            updateButtonTexts()
            uiEndLoading()
        }
    }

    private fun recreateUI() {
        Handler(Looper.getMainLooper()).post {
            logEvent("MA: recreate UI")
            uiStartLoading()
            recreate()
        }
    }

    override fun onInitializeQueryComplete() {
        updateBehavior()
        billingActionsHelper.resetStatusCodes()
    }

    override fun uiStartLoading() {
        isLoading = true
        loadingIndicator?.visibility = View.VISIBLE
    }

    override fun uiEndLoading() {
        logEvent("MA: ending loading")
        isLoading = false
        loadingIndicator?.visibility = View.GONE
    }

    private fun updateBehavior() {
        logEvent("MA: update behavior")
        uiEndLoading()
        billingActionsHelper.clearOnBillingConnectFailedCallback()
        updateButtonTexts()
    }

    private fun updateButtonTexts() {
        if (BillingUtils.isGardenerActive()) {
            gardener?.text = getString(sharedR.string.reset_gardener)
        } else {
            gardener?.text = getString(sharedR.string.be_gardener)
        }
        if (BillingUtils.isWeeklySubActive()) {
            weekly?.text = getString(
                sharedR.string.format_with_state,
                getString(sharedR.string.weekly_sub),
                getString(sharedR.string.active),
            )
        } else {
            weekly?.text = getString(sharedR.string.weekly_sub)
        }
        tree?.text = getString(
            sharedR.string.format_with_state,
            getString(sharedR.string.buy_a_tree),
            billingActionsHelper.getTreeCount().toString(),
        )
        rose?.text = getString(
            sharedR.string.format_with_state,
            getString(sharedR.string.buy_a_rose),
            billingActionsHelper.getRoseCount().toString(),
        )
    }

    override fun onDestroy() {
        billingActionsHelper.destroy()
        super.onDestroy()
    }

    private fun handleBillingExceptionOnLoading() {
        // Do sth about error loading
        logEvent("MA: handle billing exception")
        uiEndLoading()
    }

    private fun logEvent(msg: String) {
        AzureLanLog.i(msg)
        ExternallyVisibleLog.appendNewLog(msg)
    }
}