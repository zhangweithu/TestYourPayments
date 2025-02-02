package com.azurelan.testyourpayments.mainactivity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView.rootView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. This solution sets
            // only the bottom, left, and right dimensions, but you can apply whichever
            // insets are appropriate to your layout. You can also update the view padding
            // if that's more appropriate.
            v.setPadding(
                insets.left,
                insets.top,
                insets.right,
                insets.bottom,
            )
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

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
            val intent = Intent(this, ExternalVisibleLogActivity::class.java)
            intent.setPackage(packageName)
            startActivity(intent)
        }
        contactButton?.setOnClickListener {
            ContactUtils.contactByEmail(
                this,
                com.azurelan.testyourpayments.BuildConfig.VERSION_CODE,
                ContactUtils.FormFactor.PHONE)
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
        //reinitButtonTexts()
    }

    override fun onPurchaseAckedContinue() {
        recreateUI()
        //reinitButtonTexts()
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
        logEvent("MA: starting loading")
        isLoading = true
        Handler(Looper.getMainLooper()).post {
            loadingIndicator?.visibility = View.VISIBLE
        }
    }

    override fun uiEndLoading() {
        logEvent("MA: ending loading")
        isLoading = false
        Handler(Looper.getMainLooper()).post {
            loadingIndicator?.visibility = View.GONE
        }
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
        logEvent("MA: handling billing exception")
        uiEndLoading()
    }

    private fun logEvent(msg: String) {
        AzureLanLog.i(msg)
        ExternallyVisibleLog.appendNewLog(msg)
    }
}