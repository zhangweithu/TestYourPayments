package com.azurelan.testyourpayments.mainactivity

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.billingclient.api.Purchase
import com.azurelan.testyourpayments.R
import com.azurelan.testyourpayments.billing.BillingUtils
import com.azurelan.testyourpayments.databinding.ActivityMainBinding
import com.azurelan.testyourpayments.utils.AzureLanLog
import com.azurelan.testyourpayments.viewmodels.BillingViewModel

class MainActivity : AppCompatActivity(),
BillingUtils.PurchaseAckedListener,
BillingUtils.SubscriptionPurchasesQueryListener,
BillingUtils.SubscriptionProductsQueryListener,
BillingUtils.InAppPurchasesQueryListener  {

    private lateinit var binding: ActivityMainBinding
    private var billingUtils: BillingUtils? = null

    private var purchasesQueryResultCode: Int? = null
    private var subProductsQueryResultsCode: Int? = null
    private var inAppPurchaseQueryResultCode: Int? = null
    private var loadingIndicator: View? = null
    private var hasInitiatedBilling = false
    private var isLoading = false
    private lateinit var viewModel: BillingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[BillingViewModel::class.java]
        isLoading = true
        loadingIndicator = findViewById(R.id.loading_indicator)
        loadingIndicator?.visibility = View.VISIBLE

    }

    private fun initBillingIfApplicable() {
        billingUtils = BillingUtils(this, viewModel)
        if (isLoading) {
            billingUtils?.registerOnBillingConnectFailedCallback {
                AzureLanLog.d("MainActivity: error setting up billing service; treated based on last known active")
                handleBillingExceptionOnLoading()
                // Clear itself
                billingUtils?.clearOnBillingConnectFailedCallback()
                hasInitiatedBilling = false
            }
        }
        billingUtils?.registerPurchaseAckedListener(this)
        billingUtils?.registerSubscriptionPurchasesQueryListener(this)
        billingUtils?.registerSubscriptionProductsQueryListener(this)
        billingUtils?.registerInAppPurchasesQueryListener(this)
        // Must only call setup after the listener is successfully registered
        billingUtils?.setupBillingClient(this)

        hasInitiatedBilling = true
    }

    override fun onStart() {
        super.onStart()

        if (!hasInitiatedBilling) {
            Handler(Looper.getMainLooper()).post { initBillingIfApplicable() }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasInitiatedBilling) {
            billingUtils?.queryOwnedSubscriptionPurchases()
            billingUtils?.queryOwnedInAppPurchases()
        }
    }


    override fun onPurchaseAcked(purchase: Purchase) {
        if (purchase.products.contains(BillingUtils.PRODUCT_ID_WEEKLY_MEMBERSHIP)
            || purchase.products.contains(BillingUtils.PRODUCT_ID_MONTHLY_MEMBERSHIP)) {
            AzureLanLog.d("MainActivity: purchase subs acked for %s", purchase)
            // Do sth about membership
            Handler(Looper.getMainLooper()).post {
                AzureLanLog.d("MainActivity: acked and recreate UI")
                isLoading = true
                loadingIndicator?.visibility = View.VISIBLE
                recreate()
            }
        }
    }

    override fun onSubscriptionPurchasesQueryResultComplete(resultCode: Int) {
        Handler(Looper.getMainLooper()).post {
            AzureLanLog.d("MainActivity: subscription purchases query result complete")
            purchasesQueryResultCode = resultCode
            if (subProductsQueryResultsCode != null && inAppPurchaseQueryResultCode != null) {
                updateBehaviorBasedOnOwnedSubscriptionStatus()
            }
        }
    }

    override fun onSubscriptionProductsQueryResultComplete(resultCode: Int) {
        Handler(Looper.getMainLooper()).post {
            AzureLanLog.d("MainActivity: subscription products query result complete")
            subProductsQueryResultsCode = resultCode
            if (purchasesQueryResultCode != null && inAppPurchaseQueryResultCode != null) {
                updateBehaviorBasedOnOwnedSubscriptionStatus()
            }
        }
    }

    override fun onInAppPurchasesQueryResultComplete(resultCode: Int) {
        Handler(Looper.getMainLooper()).post {
            inAppPurchaseQueryResultCode = resultCode
            AzureLanLog.d("MainActivity: inapp purchases query result complete")
            if (purchasesQueryResultCode != null && subProductsQueryResultsCode != null) {
                updateBehaviorBasedOnOwnedSubscriptionStatus()
            }
        }
    }

    private fun updateBehaviorBasedOnOwnedSubscriptionStatus() {
        isLoading = false
        //loadingIndicator?.visibility = View.GONE
        billingUtils?.clearOnBillingConnectFailedCallback()
    }

    override fun onDestroy() {
        billingUtils?.cleanUpBillingClient()
        hasInitiatedBilling = false
        super.onDestroy()
    }

    private fun handleBillingExceptionOnLoading() {
        // Do sth about error loading
        isLoading = false
        //loadingIndicator?.visibility = View.GONE
    }
}