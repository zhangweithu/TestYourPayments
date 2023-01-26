package com.azurelan.testyourpayments.shared.billing

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.android.billingclient.api.Purchase
import com.azurelan.testyourpayments.shared.externalvisiblelog.ExternallyVisibleLog
import com.azurelan.testyourpayments.shared.preferencevaluestore.PreferencesAccessUtils
import com.azurelan.testyourpayments.shared.utils.AzureLanLog
import com.azurelan.testyourpayments.shared.viewmodels.BillingViewModel

class BillingActionsHelper(private val activity: Activity) :
    BillingUtils.PurchaseAckedListener,
    BillingUtils.SubscriptionPurchasesQueryListener,
    BillingUtils.SubscriptionProductsQueryListener,
    BillingUtils.InAppPurchasesQueryListener,
    BillingUtils.InAppProductsQueryListener,
    BillingUtils.PurchaseConsumedListener {

    private var billingUtils: BillingUtils? = null

    private var subscriptionPurchasesQueryResultCode: Int? = null
    private var subProductsQueryResultsCode: Int? = null
    private var inAppPurchaseQueryResultCode: Int? = null
    private var inAppProductsQueryResultCode: Int? = null

    private var hasInitiatedBilling = false

    private var treeCount = 0
    private var roseCount = 0

    fun hasInitiatedBilling() = hasInitiatedBilling
    fun getTreeCount() = treeCount
    fun getRoseCount() = roseCount

    private lateinit var onPurchaseConsumedContinueRunnable: OnPurchaseConsumedContinueRunnable
    private lateinit var onPurchaseAckedContinueRunnable: OnPurchaseAckedContinueRunnable
    private lateinit var onInitializeQueryCompleteRunnable: OnInitializeQueryCompleteRunnable
    private lateinit var uiActions: BillingUtils.UiActions

    interface OnPurchaseConsumedContinueRunnable {
        fun onPurchaseAckedAndConsumedContinue()
    }

    interface OnPurchaseAckedContinueRunnable {
        fun onPurchaseAckedContinue()
    }

    interface OnInitializeQueryCompleteRunnable {
        fun onInitializeQueryComplete()
    }

    fun registerOnPurchaseConsumedContinueRunnable(listener: OnPurchaseConsumedContinueRunnable) {
        onPurchaseConsumedContinueRunnable = listener
    }

    fun registerOnPurchaseAckedContinueRunnable(listener: OnPurchaseAckedContinueRunnable) {
        onPurchaseAckedContinueRunnable = listener
    }

    fun registerOnInitializeQueryCompleteRunnable(listener: OnInitializeQueryCompleteRunnable) {
        onInitializeQueryCompleteRunnable = listener
    }

    fun registerUiActions(uiActions: BillingUtils.UiActions) {
        this.uiActions = uiActions
    }

    fun initialize() {
        treeCount = PreferencesAccessUtils.readPreferenceString(
            activity,
            activity.getString(com.azurelan.testyourpayments.shared.R.string.preference_tree_key),
            "0",
        ).toInt()
        roseCount = PreferencesAccessUtils.readPreferenceString(
            activity,
            activity.getString(com.azurelan.testyourpayments.shared.R.string.preference_rose_key),
            "0",
        ).toInt()
    }

    fun initBillingIfApplicable(
        viewModel: BillingViewModel,
        isLoading: Boolean,
        handleBillingExceptionOnLoading: () -> Unit,
    ) {
        billingUtils = BillingUtils(activity, viewModel)
        if (isLoading) {
            billingUtils?.registerOnBillingConnectFailedCallback {
                AzureLanLog.i("MainActivity: error setting up billing service")
                handleBillingExceptionOnLoading()
                // Clear itself
                billingUtils?.clearOnBillingConnectFailedCallback()
                hasInitiatedBilling = false
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        activity,
                        "Unable to set up billing",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
        billingUtils?.registerPurchaseConsumedListener(this)
        billingUtils?.registerPurchaseAckedListener(this)
        billingUtils?.registerSubscriptionPurchasesQueryListener(this)
        billingUtils?.registerSubscriptionProductsQueryListener(this)
        billingUtils?.registerInAppPurchasesQueryListener(this)
        billingUtils?.registerInAppProductsQueryListener(this)
        billingUtils?.registerUiActions(uiActions)
        // Must only call setup after the listener is successfully registered
        billingUtils?.setupBillingClient(activity)

        hasInitiatedBilling = true
    }

    override fun onPurchaseAckedAndConsumed(purchase: Purchase) {
        if (purchase.products.contains(BillingUtils.PRODUCT_ID_IN_APP_PRODUCT_TREE)) {
            AzureLanLog.i(
                "MainActivity: %s on purchase consumed for purchase %s",
                activity,
                purchase)
            treeCount++
            PreferencesAccessUtils.writePreferenceString(
                activity,
                activity.getString(com.azurelan.testyourpayments.shared.R.string.preference_tree_key),
                treeCount.toString(),
            )
        }
        if (purchase.products.contains(BillingUtils.PRODUCT_ID_IN_APP_PRODUCT_ROSE)) {
            AzureLanLog.i(
                "MainActivity: %s on purchase consumed for purchase %s",
                activity,
                purchase)
            roseCount++
            PreferencesAccessUtils.writePreferenceString(
                activity,
                activity.getString(com.azurelan.testyourpayments.shared.R.string.preference_rose_key),
                roseCount.toString(),
            )
        }
        onPurchaseConsumedContinueRunnable.onPurchaseAckedAndConsumedContinue()
    }

    override fun onPurchaseAcked(purchase: Purchase) {
        AzureLanLog.i("MainActivity: on purchase acked for purchase %s", purchase)
        onPurchaseAckedContinueRunnable.onPurchaseAckedContinue()
    }

    override fun onSubscriptionPurchasesQueryResultComplete(resultCode: Int) {
        Handler(Looper.getMainLooper()).post {
            AzureLanLog.i("MainActivity: on owned subscription purchases query result complete")
            subscriptionPurchasesQueryResultCode = resultCode
            if (subProductsQueryResultsCode != null
                && inAppPurchaseQueryResultCode != null
                && inAppProductsQueryResultCode != null) {
                onInitializeQueryCompleteRunnable.onInitializeQueryComplete()
            }
        }
    }

    override fun onSubscriptionProductsQueryResultComplete(resultCode: Int) {
        Handler(Looper.getMainLooper()).post {
            AzureLanLog.i("MainActivity: on available subscription products query result complete")
            subProductsQueryResultsCode = resultCode
            if (subscriptionPurchasesQueryResultCode != null
                && inAppPurchaseQueryResultCode != null
                && inAppProductsQueryResultCode != null) {
                onInitializeQueryCompleteRunnable.onInitializeQueryComplete()
            }
        }
    }

    override fun onInAppPurchasesQueryResultComplete(resultCode: Int) {
        Handler(Looper.getMainLooper()).post {
            inAppPurchaseQueryResultCode = resultCode
            AzureLanLog.i("MainActivity: on owned inapp purchases query result complete")
            if (subscriptionPurchasesQueryResultCode != null
                && subProductsQueryResultsCode != null
                && inAppProductsQueryResultCode != null) {
                onInitializeQueryCompleteRunnable.onInitializeQueryComplete()
            }
        }
    }

    override fun onInAppProductsQueryResultComplete(resultCode: Int) {
        Handler(Looper.getMainLooper()).post {
            inAppProductsQueryResultCode = resultCode
            AzureLanLog.i("MainActivity: on available inapp products query result complete")
            if (subscriptionPurchasesQueryResultCode != null
                && subProductsQueryResultsCode != null
                && inAppPurchaseQueryResultCode != null) {
                onInitializeQueryCompleteRunnable.onInitializeQueryComplete()
            }
        }
    }

    fun onAppResume() {
        if (hasInitiatedBilling) {
            logEvent("MA: querying purchases in onResume()")
            billingUtils?.queryOwnedSubscriptionPurchases()
            billingUtils?.queryOwnedInAppPurchases()
        }
    }

    fun handleGardenerClick() {
        if (!BillingUtils.isGardenerActive()) {
            launchPurchaseFlowFor(BillingProduct.GARDENER)
        } else {
            billingUtils?.consumeGardenerProduct()
        }
    }

    fun launchPurchaseFlowFor(product: BillingProduct) {
        when(product) {
            BillingProduct.GARDENER -> {
                billingUtils?.let {
                    val productDetails = BillingUtils.getInAppProductDetails(
                        BillingUtils.PRODUCT_ID_IN_APP_PRODUCT_BECOME_GARDENER)
                    if (productDetails != null) {
                        BillingUtils.launchPurchaseFlow(
                            activity,
                            productDetails,
                        )
                    }
                }
            }
            BillingProduct.TREE -> {
                billingUtils?.let {
                    val productDetails = BillingUtils.getInAppProductDetails(
                        BillingUtils.PRODUCT_ID_IN_APP_PRODUCT_TREE
                    )
                    if (productDetails != null) {
                        BillingUtils.launchPurchaseFlow(
                            activity,
                            productDetails,
                        )
                    }
                }
            }
            BillingProduct.ROSE -> {
                billingUtils?.let {
                    val productDetails = BillingUtils.getInAppProductDetails(
                        BillingUtils.PRODUCT_ID_IN_APP_PRODUCT_ROSE)
                    if (productDetails != null) {
                        BillingUtils.launchPurchaseFlow(
                            activity,
                            productDetails,
                        )
                    }
                }
            }
            BillingProduct.WEEKLY_JOB -> {
                billingUtils?.let {
                    val productDetails = BillingUtils.getWeeklyMembershipProductDetails()
                    val offerToken = BillingUtils.getWeeklyPlanOffer()?.offerToken
                    if (productDetails != null && offerToken != null) {
                        BillingUtils.launchPurchaseFlowForNewSubscription(
                            activity,
                            productDetails,
                            offerToken,
                        )
                    }
                }
            }
        }
    }

    fun destroy() {
        billingUtils?.cleanUpBillingClient()
        hasInitiatedBilling = false
    }

    fun clearOnBillingConnectFailedCallback() {
        billingUtils?.clearOnBillingConnectFailedCallback()
    }

    private fun logEvent(msg: String) {
        AzureLanLog.i(msg)
        ExternallyVisibleLog.appendNewLog(msg)
    }

    fun resetStatusCodes() {
        subscriptionPurchasesQueryResultCode = null
        subProductsQueryResultsCode = null
        inAppPurchaseQueryResultCode = null
        inAppProductsQueryResultCode = null
    }
}