package com.azurelan.testyourpayments.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.azurelan.testyourpayments.R
import com.azurelan.testyourpayments.utils.AzureLanLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingUtils(
    private val context: Context,
    private val viewModel: ViewModel
):
    PurchasesUpdatedListener, DefaultLifecycleObserver, BillingClientStateListener {
    companion object {
        const val PRODUCT_ID_WEEKLY_MEMBERSHIP = "com.azurelan.testyourpay.weekly_sub_1"
        const val PRODUCT_ID_MONTHLY_MEMBERSHIP = "com.azurelan.testyourpay.monthly_sub_1"
        private const val WEEKLY_BASE_PLAN_TAG = "weekly-sub-1"
        private const val MONTHLY_BASE_PLAN_TAG = "monthly-sub-1"
        const val PRODUCT_ID_IN_APP_PRODUCT_BECOME_GARDENER = "gardener_1"
        const val PRODUCT_ID_IN_APP_PRODUCT_TREE = "tree_1"
        const val PRODUCT_ID_IN_APP_PRODUCT_ROSE = "rose_1"

        var billingClient: BillingClient? = null

        var availableInAppProductDetailsList: List<ProductDetails>? = null
        var availableSubscriptionDetailsList: List<ProductDetails>? = null
        var purchasedSubscriptionsList: List<Purchase>? = null
        var ownedInAppPurchasesList: List<Purchase>? = null

        fun getSubscriptionBasePlan(productId: String, offerTag: String):
                ProductDetails.SubscriptionOfferDetails? {
            availableSubscriptionDetailsList?.let {
                for (productDetails in it) {
                    val subscriptionOfferDetails = productDetails.subscriptionOfferDetails
                    if (productDetails.productId == productId
                        && subscriptionOfferDetails != null) {
                        for (offer in subscriptionOfferDetails) {
                            if (offer.offerTags.contains(offerTag)) {
                                return offer
                            }
                        }
                    }
                }
            }
            return null
        }

        /**
         *  Fetch the formatted price display for Monthly Sub item, from already initiated
         *  premiumPlanSubscriptionDetailsList. If premiumPlanSubscriptionDetailsList has not been
         *  initiated yet or if it cannot find qualified items, return null.
         */

        fun getWeeklyPlanPrice(): String? = getBasePlanPrice(
            PRODUCT_ID_WEEKLY_MEMBERSHIP, WEEKLY_BASE_PLAN_TAG)
        fun getWeeklyPlanOffer(): ProductDetails.SubscriptionOfferDetails? =
            getSubscriptionBasePlan(PRODUCT_ID_WEEKLY_MEMBERSHIP, WEEKLY_BASE_PLAN_TAG)

        fun getMonthlyPlanPrice(): String? = getBasePlanPrice(
            PRODUCT_ID_MONTHLY_MEMBERSHIP, MONTHLY_BASE_PLAN_TAG)
        fun getMonthlyPlanOffer(): ProductDetails.SubscriptionOfferDetails? =
            getSubscriptionBasePlan(PRODUCT_ID_MONTHLY_MEMBERSHIP, MONTHLY_BASE_PLAN_TAG)

        fun getWeeklyMembershipProductDetails(): ProductDetails? {
            availableSubscriptionDetailsList?.let {
                for (productDetails in it) {
                    if (productDetails.productId == PRODUCT_ID_WEEKLY_MEMBERSHIP) {
                        return productDetails
                    }
                }
            }
            return null
        }

        fun getMonthlyMembershipProductDetails(): ProductDetails? {
            availableSubscriptionDetailsList?.let {
                for (productDetails in it) {
                    if (productDetails.productId == PRODUCT_ID_MONTHLY_MEMBERSHIP) {
                        return productDetails
                    }
                }
            }
            return null
        }

        private fun getBasePlanPrice(productId: String, offerTag: String): String? {
            val pricingPhases = getSubscriptionBasePlan(productId, offerTag)?.pricingPhases
            if (pricingPhases != null && pricingPhases.pricingPhaseList.isNotEmpty()) {
                return pricingPhases.pricingPhaseList[pricingPhases.pricingPhaseList.size - 1].formattedPrice
            }
            return null
        }

        fun getWeeklyPlanFreeTrialPeriod(): String? = getFreeTrialPeriod(
            PRODUCT_ID_WEEKLY_MEMBERSHIP, WEEKLY_BASE_PLAN_TAG)

        fun getMonthlyPlanFreeTrialPeriod(): String? = getFreeTrialPeriod(
            PRODUCT_ID_MONTHLY_MEMBERSHIP, MONTHLY_BASE_PLAN_TAG)

        private fun getFreeTrialPeriod(productId: String, offerTag: String): String? {
            val pricingPhases = getSubscriptionBasePlan(productId, offerTag)?.pricingPhases
            if (pricingPhases != null && pricingPhases.pricingPhaseList.isNotEmpty()) {
                if (pricingPhases.pricingPhaseList[0].priceAmountMicros == 0L) {
                    return pricingPhases.pricingPhaseList[0].billingPeriod
                }
            }
            return null
        }

        fun isWeeklySubActive(): Boolean {
            purchasedSubscriptionsList?.let {
                for (purchase in it) {
                    if ((purchase.products.contains(PRODUCT_ID_WEEKLY_MEMBERSHIP))
                        && purchase.isAcknowledged
                        && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        return true
                    }
                }
            }
            return false
        }

        fun isGardenerActive(): Boolean {
            ownedInAppPurchasesList?.let {
                for (purchase in it) {
                    if ((purchase.products.contains(PRODUCT_ID_IN_APP_PRODUCT_BECOME_GARDENER))
                        && purchase.isAcknowledged
                        && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        return true
                    }
                }
            }
            return false
        }

        /** Fetches the purchase state of Subscription.*/
        fun getSubscriptionStatus(): SubscriptionActiveStatus {
            purchasedSubscriptionsList?.let {
                for (purchase in it) {
                    if ((purchase.products.contains(PRODUCT_ID_MONTHLY_MEMBERSHIP))
                        && purchase.isAcknowledged
                        && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        AzureLanLog.d(
                            "BillingUtils: purchase details: %s, %s",
                            purchase.toString(),
                            purchase.originalJson)
                        return SubscriptionActiveStatus.MONTHLY
                    } else if ((purchase.products.contains(PRODUCT_ID_WEEKLY_MEMBERSHIP))
                        && purchase.isAcknowledged
                        && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        AzureLanLog.d(
                            "BillingUtils: purchase details: %s, %s",
                            purchase.toString(),
                            purchase.originalJson)
                        return SubscriptionActiveStatus.ANNUAL
                    }
                }
            }
            return SubscriptionActiveStatus.INACTIVE
        }

        fun getInAppProductDetails(id: String): ProductDetails? {
            availableSubscriptionDetailsList?.let {
                for (productDetails in it) {
                    if (productDetails.productId == id) {
                        return productDetails
                    }
                }
            }
            return null
        }

        /** Fetches the purchase state of Remove-Ads product.*/
        fun isAGardenerNow(): Boolean {
            ownedInAppPurchasesList?.let {
                for (purchase in it) {
                    if (purchase.products.contains(PRODUCT_ID_IN_APP_PRODUCT_BECOME_GARDENER)
                        && purchase.isAcknowledged
                        && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        return true
                    }
                }
            }
            return false
        }

        fun getActiveMonthlyPlanPurchaseToken(): String? =
            getPurchaseToken(PRODUCT_ID_MONTHLY_MEMBERSHIP)

        fun getActiveAnnualPlanPurchaseToken(): String? =
            getPurchaseToken(PRODUCT_ID_WEEKLY_MEMBERSHIP)

        private fun getPurchaseToken(productId: String): String? {
            purchasedSubscriptionsList?.let {
                for (purchase in it) {
                    if ((purchase.products.contains(productId))
                        && purchase.isAcknowledged
                        && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        return purchase.purchaseToken
                    }
                }
            }
            return null
        }

        /** Launch the purchase flow for the given product */
        fun launchPurchaseFlowForNewSubscription(
            activity: Activity, productDetails: ProductDetails, offerToken: String) {
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            // Launch the billing flow
            val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
            if (billingResult != null
                && billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                val errorMessage = getToastMessage(billingResult.responseCode)
                errorMessage?.let {
                    Handler(Looper.getMainLooper()).post {
                        Toast
                            .makeText(
                                activity,
                                activity.getString(it),
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }
            }
        }

        /** Launch the purchase flow for the given product */
        fun launchPurchaseFlowForUpdatingSubscription(
            activity: Activity,
            productDetails: ProductDetails,
            newOfferToken: String,
            oldPurchaseToken: String) {
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                    .setProductDetails(productDetails)
                    .setOfferToken(newOfferToken)
                    .build()
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .setSubscriptionUpdateParams(
                    BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                        .setOldPurchaseToken(oldPurchaseToken)
                        .setReplaceProrationMode(
                            BillingFlowParams.ProrationMode.DEFERRED)
                        .build())
                .build()

            // Launch the billing flow
            val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
            if (billingResult != null
                && billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                val errorMessage = getToastMessage(billingResult.responseCode)
                errorMessage?.let {
                    Handler(Looper.getMainLooper()).post {
                        Toast
                            .makeText(
                                activity,
                                activity.getString(it),
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }
            }
        }

        /** Launch the purchase flow for the given product */
        fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                    .setProductDetails(productDetails)
                    .build()
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            // Launch the billing flow
            val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
            if (billingResult != null
                && billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                val errorMessage = getToastMessage(billingResult.responseCode)
                errorMessage?.let {
                    Handler(Looper.getMainLooper()).post {
                        Toast
                            .makeText(
                                activity,
                                activity.getString(it),
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }
            }
        }

        @StringRes
        private fun getToastMessage(billingResponseCode: Int): Int? {
            return when (billingResponseCode) {
                BillingClient.BillingResponseCode.OK -> null
                BillingClient.BillingResponseCode.USER_CANCELED -> R.string.billing_user_cancelled
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> R.string.billing_item_unavailable
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> R.string.billing_already_owned
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                BillingClient.BillingResponseCode.SERVICE_TIMEOUT,
                -> R.string.billing_service_unavailable
                else -> R.string.billing_generic_error
            }
        }
    }

    interface PurchaseAckedListener {
        fun onPurchaseAcked(purchase: Purchase)
    }

    interface SubscriptionPurchasesQueryListener {
        fun onSubscriptionPurchasesQueryResultComplete(resultCode: Int)
    }

    interface SubscriptionProductsQueryListener {
        fun onSubscriptionProductsQueryResultComplete(resultCode: Int)
    }

    interface InAppPurchasesQueryListener {
        fun onInAppPurchasesQueryResultComplete(resultCode: Int)
    }

    interface InAppProductsQueryListener {
        fun onInAppProductsQueryResultComplete(resultCode: Int)
    }

    private val purchaseAckedListeners = mutableSetOf<PurchaseAckedListener>()
    private val subscriptionPurchasesQueryListeners = mutableSetOf<SubscriptionPurchasesQueryListener>()
    private val subscriptionProductsQueryListeners = mutableSetOf<SubscriptionProductsQueryListener>()
    private val inAppPurchasesQueryListeners = mutableSetOf<InAppPurchasesQueryListener>()
    private val inAppProductsQueryListeners = mutableSetOf<InAppProductsQueryListener>()
    private var onBillingConnectFailedCallback: Runnable? = null

    fun registerPurchaseAckedListener(purchaseAckedListener: PurchaseAckedListener) {
        purchaseAckedListeners.add(purchaseAckedListener)
    }

    fun registerSubscriptionPurchasesQueryListener(subscriptionPurchasesQueryListener: SubscriptionPurchasesQueryListener) {
        subscriptionPurchasesQueryListeners.add(subscriptionPurchasesQueryListener)
    }

    fun registerSubscriptionProductsQueryListener(subscriptionProductsQueryListener: SubscriptionProductsQueryListener) {
        AzureLanLog.d("BillingUtils: added listener: %s", subscriptionProductsQueryListener)
        subscriptionProductsQueryListeners.add(subscriptionProductsQueryListener)
    }

    fun registerOnBillingConnectFailedCallback(callback: Runnable) {
        onBillingConnectFailedCallback = callback
    }

    fun registerInAppProductsQueryListener(inAppProductsQueryListener: InAppProductsQueryListener) {
        AzureLanLog.d("BillingUtils: added listener: %s", inAppProductsQueryListener)
        inAppProductsQueryListeners.add(inAppProductsQueryListener)
    }

    fun registerInAppPurchasesQueryListener(inAppPurchasesQueryListener: InAppPurchasesQueryListener) {
        inAppPurchasesQueryListeners.add(inAppPurchasesQueryListener)
    }

    fun clearOnBillingConnectFailedCallback() {
        onBillingConnectFailedCallback = null
    }

    fun setupBillingClient(context: Context) {
        if (billingClient == null) {
            initBillingClient(context)
        }
        billingClient!!.startConnection(this)
    }

    /** Disconnect the billing service when no longer used. */
    fun cleanUpBillingClient() {
        AzureLanLog.d("BillingUtils: clean up")
        billingClient?.endConnection()
        purchaseAckedListeners.clear()
        subscriptionPurchasesQueryListeners.clear()
        subscriptionProductsQueryListeners.clear()
        inAppPurchasesQueryListeners.clear()
        onBillingConnectFailedCallback = null
        billingClient = null
        availableSubscriptionDetailsList = null
        availableInAppProductDetailsList = null
        purchasedSubscriptionsList = null
    }

    private fun initBillingClient(context: Context) {
        if (billingClient == null) {
            // Use application context for singleton
            billingClient = BillingClient.newBuilder(context.applicationContext)
                .setListener(this)
                .enablePendingPurchases()
                .build()
        }
    }

    /** Implements BillingClientStateListener */
    override fun onBillingSetupFinished(setupBillingResult: BillingResult) {
        if (setupBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            // The BillingClient is ready. You can query purchases here.
            queryAvailableSubscriptions()
            queryOwnedSubscriptionPurchases()
            queryOwnedInAppPurchases()
            queryAvailableInAppProducts()
        } else {
            AzureLanLog.e("BillingUtils: error on setting up billing %d", setupBillingResult.responseCode)
            val callback = onBillingConnectFailedCallback
            onBillingConnectFailedCallback = null
            callback?.run()
            // Log error
            Handler(Looper.getMainLooper()).post {
                Toast
                    .makeText(
                        context,
                        context.getString(R.string.billing_cannot_setup),
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
        }
    }

    /** Query products available for sale */
    private fun queryAvailableInAppProducts() {
        val queryProductDetailsParams =
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build(),
                    )
                )
                .build()
        billingClient?.queryProductDetailsAsync(queryProductDetailsParams) { queryBillingResult,
                                                                             productDetailsList ->
            // check billingResult
            // process returned productDetailsList
            if (queryBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (productDetailsList.isNotEmpty()) {
                    availableInAppProductDetailsList = productDetailsList
                }
            } else {
                AzureLanLog.e("BillingUtils: error on query available inapp products %d", queryBillingResult.responseCode)
                val errorMessage = getToastMessage(queryBillingResult.responseCode)
                errorMessage?.let {
                    Handler(Looper.getMainLooper()).post {
                        Toast
                            .makeText(
                                context,
                                context.getString(it),
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }
            }
            for (inAppProductsQueryListener in inAppProductsQueryListeners) {
                inAppProductsQueryListener
                    .onInAppProductsQueryResultComplete(queryBillingResult.responseCode)
            }
            AzureLanLog.d(
                "BillingUtils: in app products result list %s", productDetailsList)
        }
    }

    /** Query subscriptions available for sale */
    private fun queryAvailableSubscriptions() {
        val queryProductDetailsParams =
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(PRODUCT_ID_WEEKLY_MEMBERSHIP)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build(),
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(PRODUCT_ID_MONTHLY_MEMBERSHIP)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build(),
                    )
                )
                .build()
        billingClient?.queryProductDetailsAsync(queryProductDetailsParams) { queryBillingResult,
                                                                             productDetailsList ->
            // check billingResult
            // process returned productDetailsList
            if (queryBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (productDetailsList.isNotEmpty()) {
                    availableSubscriptionDetailsList = productDetailsList
                }
            } else {
                AzureLanLog.e("BillingUtils: error on query available subscription products %d", queryBillingResult.responseCode)
                val errorMessage = getToastMessage(queryBillingResult.responseCode)
                errorMessage?.let {
                    Handler(Looper.getMainLooper()).post {
                        Toast
                            .makeText(
                                context,
                                context.getString(it),
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }
            }
            for (subscriptionProductsQueryListener in subscriptionProductsQueryListeners) {
                subscriptionProductsQueryListener
                    .onSubscriptionProductsQueryResultComplete(queryBillingResult.responseCode)
            }
            AzureLanLog.d(
                "BillingUtils: subscription products result list %s", productDetailsList)
        }
    }

    /** Query subscription purchases currently owned */
    fun queryOwnedSubscriptionPurchases() {
        val queryPurchasesParams =
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        billingClient?.queryPurchasesAsync(queryPurchasesParams) { queryBillingResult,
                                                                   purchases ->
            AzureLanLog.i("BillingUtils: query owned purchases async result: %s", purchases)
            // check billingResult
            // process returned productDetailsList
            if (queryBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (purchases.isNotEmpty()) {
                    purchasedSubscriptionsList = purchases
                    // If we found purchases not yet acked, ack them here now
                    for (purchase in purchases) {
                        if (!purchase.isAcknowledged) {
                            AzureLanLog.i("BillingUtils: acking purchase not yet acked: %s", purchase)
                            handlePurchase(purchase)
                        }
                    }
                }
            } else {
                AzureLanLog.e("BillingUtils: error on query owned subscription purchases %d", queryBillingResult.responseCode)
                val errorMessage = getToastMessage(queryBillingResult.responseCode)
                errorMessage?.let {
                    Handler(Looper.getMainLooper()).post {
                        Toast
                            .makeText(
                                context,
                                context.getString(it),
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }
            }
            for (subscriptionPurchasesQueryListener in subscriptionPurchasesQueryListeners) {
                subscriptionPurchasesQueryListener
                    .onSubscriptionPurchasesQueryResultComplete(queryBillingResult.responseCode)
            }
            AzureLanLog.i("BillingUtils: subscription purchases result list %s", purchases)
        }
    }

    /** Query in-app purchases */
    fun queryOwnedInAppPurchases() {
        val queryPurchasesParams =
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        billingClient?.queryPurchasesAsync(queryPurchasesParams) { queryBillingResult,
                                                                   purchases ->
            // check billingResult
            // process returned productDetailsList
            if (queryBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                AzureLanLog.i("BillingUtils: acking inapp purchase not yet acked: %s", purchases)
                if (purchases.isNotEmpty()) {
                    ownedInAppPurchasesList = purchases
                    // If we found purchases not yet acked, ack them here now
                    for (purchase in purchases) {
                        if (!purchase.isAcknowledged) {
                            AzureLanLog.i("BillingUtils: acking inapp purchase not yet acked: %s", purchase)
                            handlePurchase(purchase)
                        }
                    }
                }
            } else {
                AzureLanLog.e("BillingUtils: error on query owned in app purchases %d", queryBillingResult.responseCode)
                val errorMessage = getToastMessage(queryBillingResult.responseCode)
                errorMessage?.let {
                    Handler(Looper.getMainLooper()).post {
                        Toast
                            .makeText(
                                context,
                                context.getString(it),
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }
            }
            for (inAppPurchasesQueryListener in inAppPurchasesQueryListeners) {
                inAppPurchasesQueryListener
                    .onInAppPurchasesQueryResultComplete(queryBillingResult.responseCode)
            }
            AzureLanLog.d("BillingUtils: in app purchases result list %s", purchases)
        }
    }

    override fun onBillingServiceDisconnected() {
        // Try to restart the connection on the next request to
        // Google Play by calling the startConnection() method.
        AzureLanLog.e("BillingUtils: connecting to Play Billing Service failed")
        Handler(Looper.getMainLooper()).post {
            Toast
                .makeText(
                    context,
                    context.getString(R.string.billing_cannot_connect),
                    Toast.LENGTH_SHORT
                )
                .show()
        }
        val callback = onBillingConnectFailedCallback
        onBillingConnectFailedCallback = null
        callback?.run()
    }

    /** Implements PurchasesUpdatedListener */
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        AzureLanLog.i("BillingUtils: purchase updated: %s", purchases.toString())
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                AzureLanLog.i("BillingUtils: handling purchase not yet acked: %s", purchase)
                handlePurchase(purchase)
            }
        } else {
            AzureLanLog.e("BillingUtils: error on purchase updated %d", billingResult.responseCode)
            val errorMessage = getToastMessage(billingResult.responseCode)
            errorMessage?.let {
                Handler(Looper.getMainLooper()).post {
                    Toast
                        .makeText(
                            context,
                            context.getString(it),
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                viewModel.viewModelScope.launch {
                    val ackPurchaseResult = withContext(Dispatchers.IO) {
                        billingClient?.acknowledgePurchase(acknowledgePurchaseParams.build())
                    }
                    if (ackPurchaseResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                        if (isPurchaseConsumable(purchase)) {
                            billingClient?.consumeAsync(
                                ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken)
                                    .build(),
                            ) { result, purchaseToken ->
                                for (purchaseAckedListener in purchaseAckedListeners) {
                                    purchaseAckedListener.onPurchaseAcked(purchase)
                                }
                            }
                        } else {
                            for (purchaseAckedListener in purchaseAckedListeners) {
                                purchaseAckedListener.onPurchaseAcked(purchase)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isPurchaseConsumable(purchase: Purchase): Boolean {
        return purchase.products.contains(PRODUCT_ID_IN_APP_PRODUCT_TREE)
                || purchase.products.contains(PRODUCT_ID_IN_APP_PRODUCT_ROSE)
    }
}