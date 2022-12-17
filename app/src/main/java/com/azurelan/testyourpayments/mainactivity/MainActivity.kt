package com.azurelan.testyourpayments.mainactivity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.android.billingclient.api.Purchase
import com.azurelan.testyourpayments.R
import com.azurelan.testyourpayments.billing.BillingUtils
import com.azurelan.testyourpayments.databinding.ActivityMainBinding
import com.azurelan.testyourpayments.preferencevaluestore.PreferencesAccessUtils
import com.azurelan.testyourpayments.utils.AzureLanLog
import com.azurelan.testyourpayments.viewmodels.BillingViewModel

class MainActivity : AppCompatActivity(),
BillingUtils.PurchaseAckedListener,
BillingUtils.SubscriptionPurchasesQueryListener,
BillingUtils.SubscriptionProductsQueryListener,
BillingUtils.InAppPurchasesQueryListener,
BillingUtils.InAppProductsQueryListener{

    private lateinit var binding: ActivityMainBinding
    private var billingUtils: BillingUtils? = null

    private var subscriptionPurchasesQueryResultCode: Int? = null
    private var subProductsQueryResultsCode: Int? = null
    private var inAppPurchaseQueryResultCode: Int? = null
    private var inAppProductsQueryResultCode: Int? = null
    private var loadingIndicator: View? = null
    private var hasInitiatedBilling = false
    private var isLoading = false
    private lateinit var viewModel: BillingViewModel

    private var gardener: Button? = null
    private var tree: Button? = null
    private var rose: Button? = null
    private var weekly: Button? = null

    private var treeCount = 0
    private var roseCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[BillingViewModel::class.java]
        isLoading = true
        loadingIndicator = findViewById(R.id.loading_indicator)
        loadingIndicator?.visibility = View.VISIBLE

        gardener = findViewById(R.id.gardener)
        tree = findViewById(R.id.tree)
        rose = findViewById(R.id.rose)
        weekly = findViewById(R.id.weekly)

        gardener?.setOnClickListener {
            billingUtils?.let {
                val productDetails = BillingUtils.getInAppProductDetails(
                    BillingUtils.PRODUCT_ID_IN_APP_PRODUCT_BECOME_GARDENER)
                if (productDetails != null) {
                    BillingUtils.launchPurchaseFlow(
                        this,
                        productDetails,
                    )
                }
            }
        }
        tree?.setOnClickListener {
            billingUtils?.let {
                val productDetails = BillingUtils.getInAppProductDetails(
                    BillingUtils.PRODUCT_ID_IN_APP_PRODUCT_TREE
                )
                if (productDetails != null) {
                    BillingUtils.launchPurchaseFlow(
                        this,
                        productDetails,
                    )
                }
            }
        }
        rose?.setOnClickListener {
            billingUtils?.let {
                val productDetails = BillingUtils.getInAppProductDetails(
                    BillingUtils.PRODUCT_ID_IN_APP_PRODUCT_ROSE)
                if (productDetails != null) {
                    BillingUtils.launchPurchaseFlow(
                        this,
                        productDetails,
                    )
                }
            }
        }
        weekly?.setOnClickListener {
            billingUtils?.let {
                val productDetails = BillingUtils.getWeeklyMembershipProductDetails()
                val offerToken = BillingUtils.getWeeklyPlanOffer()?.offerToken
                if (productDetails != null && offerToken != null) {
                        BillingUtils.launchPurchaseFlowForNewSubscription(
                            this,
                            productDetails,
                            offerToken,
                        )
                }
            }
        }
        treeCount = PreferencesAccessUtils.readPreferenceString(
            this, getString(R.string.preference_tree_key), "0").toInt()
        roseCount = PreferencesAccessUtils.readPreferenceString(
            this, getString(R.string.preference_rose_key), "0").toInt()
        updateButtonTexts()
    }

    private fun initBillingIfApplicable() {
        billingUtils = BillingUtils(this, viewModel)
        if (isLoading) {
            billingUtils?.registerOnBillingConnectFailedCallback {
                AzureLanLog.i("MainActivity: error setting up billing service")
                handleBillingExceptionOnLoading()
                // Clear itself
                billingUtils?.clearOnBillingConnectFailedCallback()
                hasInitiatedBilling = false
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        this,
                        "Unable to set up billing",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
        billingUtils?.registerPurchaseAckedListener(this)
        billingUtils?.registerSubscriptionPurchasesQueryListener(this)
        billingUtils?.registerSubscriptionProductsQueryListener(this)
        billingUtils?.registerInAppPurchasesQueryListener(this)
        billingUtils?.registerInAppProductsQueryListener(this)
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
        AzureLanLog.i("MainActivity: purchase subs acked for %s", purchase)
        if (purchase.products.contains(BillingUtils.PRODUCT_ID_IN_APP_PRODUCT_TREE)) {
            treeCount++
            PreferencesAccessUtils.writePreferenceString(
                this,
                getString(R.string.preference_tree_key),
                treeCount.toString(),
            )
        }
        if (purchase.products.contains(BillingUtils.PRODUCT_ID_IN_APP_PRODUCT_ROSE)) {
            roseCount++
            PreferencesAccessUtils.writePreferenceString(
                this,
                getString(R.string.preference_rose_key),
                roseCount.toString(),
            )
        }
        Handler(Looper.getMainLooper()).post {
            AzureLanLog.i("MainActivity: acked and recreate UI")
            isLoading = true
            loadingIndicator?.visibility = View.VISIBLE
            recreate()
        }
    }

    override fun onSubscriptionPurchasesQueryResultComplete(resultCode: Int) {
        Handler(Looper.getMainLooper()).post {
            AzureLanLog.d("MainActivity: subscription purchases query result complete")
            subscriptionPurchasesQueryResultCode = resultCode
            if (subProductsQueryResultsCode != null
                && inAppPurchaseQueryResultCode != null
                && inAppProductsQueryResultCode != null) {
                updateBehavior()
            }
        }
    }

    override fun onSubscriptionProductsQueryResultComplete(resultCode: Int) {
        Handler(Looper.getMainLooper()).post {
            AzureLanLog.d("MainActivity: subscription products query result complete")
            subProductsQueryResultsCode = resultCode
            if (subscriptionPurchasesQueryResultCode != null
                && inAppPurchaseQueryResultCode != null
                && inAppProductsQueryResultCode != null) {
                updateBehavior()
            }
        }
    }

    override fun onInAppPurchasesQueryResultComplete(resultCode: Int) {
        Handler(Looper.getMainLooper()).post {
            inAppPurchaseQueryResultCode = resultCode
            AzureLanLog.d("MainActivity: inapp purchases query result complete")
            if (subscriptionPurchasesQueryResultCode != null
                && subProductsQueryResultsCode != null
                && inAppProductsQueryResultCode != null) {
                updateBehavior()
            }
        }
    }

    override fun onInAppProductsQueryResultComplete(resultCode: Int) {
        Handler(Looper.getMainLooper()).post {
            inAppPurchaseQueryResultCode = resultCode
            AzureLanLog.d("MainActivity: inapp products query result complete")
            if (subscriptionPurchasesQueryResultCode != null
                && subProductsQueryResultsCode != null
                && inAppPurchaseQueryResultCode != null) {
                updateBehavior()
            }
        }
    }

    private fun updateBehavior() {
        isLoading = false
        loadingIndicator?.visibility = View.GONE
        billingUtils?.clearOnBillingConnectFailedCallback()
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
            treeCount.toString(),
        )
        rose?.text = getString(
            R.string.format_with_state,
            getString(R.string.buy_a_rose),
            roseCount.toString(),
        )
    }

    override fun onDestroy() {
        billingUtils?.cleanUpBillingClient()
        hasInitiatedBilling = false
        super.onDestroy()
    }

    private fun handleBillingExceptionOnLoading() {
        // Do sth about error loading
        isLoading = false
        loadingIndicator?.visibility = View.GONE
    }
}