package com.azurelan.testyourpayments.mainactivity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import com.azurelan.testyourpayments.R
import com.azurelan.testyourpayments.shared.R as sharedR
import com.azurelan.testyourpayments.shared.billing.BillingUtils
import com.azurelan.testyourpayments.databinding.ActivityMainBinding
import com.azurelan.testyourpayments.externalvisiblelog.ExternalVisibleLogActivity
import com.azurelan.testyourpayments.scrollingutils.ScrollingLayoutCallback
import com.azurelan.testyourpayments.shared.billing.BillingActionsHelper
import com.azurelan.testyourpayments.shared.billing.BillingProduct
import com.azurelan.testyourpayments.shared.utils.AzureLanLog
import com.azurelan.testyourpayments.shared.externalvisiblelog.ExternallyVisibleLog
import com.azurelan.testyourpayments.shared.viewmodels.BillingViewModel

class MainActivity : AppCompatActivity(),
        BillingActionsHelper.OnPurchaseConsumedContinueRunnable,
        BillingActionsHelper.OnPurchaseAckedContinueRunnable,
        BillingActionsHelper.OnInitializeQueryCompleteRunnable,
        BillingUtils.UiActions {

    private lateinit var binding: ActivityMainBinding
    private var loadingIndicator: View? = null
    private var isLoading = false
    private lateinit var viewModel: BillingViewModel
    private lateinit var billingActionsHelper: BillingActionsHelper
    private lateinit var recyclerView: WearableRecyclerView
    private val homeItemViewDataList = ArrayList<HomeItemViewData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AzureLanLog.d("MA: onCreate. Self: %s", this)

        val splashScreen = installSplashScreen()

        billingActionsHelper = BillingActionsHelper(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[BillingViewModel::class.java]
        loadingIndicator = findViewById(R.id.loading_indicator)
        uiStartLoading()

        homeItemViewDataList.clear()
        homeItemViewDataList.addAll(createViewDataList())
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.apply {
            // To align the edge children (first and last) with the center of the screen.
            isEdgeItemsCenteringEnabled = true
            layoutManager = WearableLinearLayoutManager(
                this@MainActivity, ScrollingLayoutCallback()
            )
            adapter = HomeItemViewAdapter(homeItemViewDataList) {
                homeItemViewData -> onItemClick(homeItemViewData)
            }
            //isCircularScrollingGestureEnabled = true
            adapter?.notifyDataSetChanged()
        }


        billingActionsHelper.initialize()
        billingActionsHelper.registerOnPurchaseConsumedContinueRunnable(this)
        billingActionsHelper.registerOnPurchaseAckedContinueRunnable(this)
        billingActionsHelper.registerOnInitializeQueryCompleteRunnable(this)
        billingActionsHelper.registerUiActions(this)

        updateButtonTexts()
        recyclerView.requestFocus()
    }

    private fun createViewDataList(): List<HomeItemViewData> {
        val homeItemViewDataList = ArrayList<HomeItemViewData>()
        // 0
        homeItemViewDataList.add(
            HomeItemViewData(
                HomeItemViewAdapter.VIEW_TYPE_HOME_TITLE_ROW,
                getString(sharedR.string.one_time_purchase),
            )
        )
        // 1
        homeItemViewDataList.add(
            HomeItemViewData(
                HomeItemViewAdapter.VIEW_TYPE_HOME_GARDENER_ITEM_ROW,
                getString(sharedR.string.be_gardener),
            )
        )
        // 2
        homeItemViewDataList.add(
            HomeItemViewData(
                HomeItemViewAdapter.VIEW_TYPE_HOME_TITLE_ROW,
                getString(sharedR.string.consumable_purchase),
            )
        )
        // 3
        homeItemViewDataList.add(
            HomeItemViewData(
                HomeItemViewAdapter.VIEW_TYPE_HOME_ROSE_ITEM_ROW,
                getString(sharedR.string.buy_a_rose),
            )
        )
        // 4
        homeItemViewDataList.add(
            HomeItemViewData(
                HomeItemViewAdapter.VIEW_TYPE_HOME_TREE_ITEM_ROW,
                getString(sharedR.string.buy_a_tree),
            )
        )
        // 5
        homeItemViewDataList.add(
            HomeItemViewData(
                HomeItemViewAdapter.VIEW_TYPE_HOME_TITLE_ROW,
                getString(sharedR.string.subscriptions),
            )
        )
        // 6
        homeItemViewDataList.add(
            HomeItemViewData(
                HomeItemViewAdapter.VIEW_TYPE_HOME_SERVICE_ITEM_ROW,
                getString(sharedR.string.weekly_sub),
            )
        )
        // 7
        homeItemViewDataList.add(
            HomeItemViewData(
                HomeItemViewAdapter.VIEW_TYPE_HOME_TITLE_ROW,
                getString(sharedR.string.logs),
            )
        )
        // 8
        homeItemViewDataList.add(
            HomeItemViewData(
                HomeItemViewAdapter.VIEW_TYPE_HOME_LOG_PAGE_ROW,
                getString(sharedR.string.view),
            )
        )
        // 9
        homeItemViewDataList.add(
            HomeItemViewData(
                HomeItemViewAdapter.VIEW_TYPE_HOME_TITLE_ROW,
                getString(sharedR.string.contact),
            )
        )
        // 10
        homeItemViewDataList.add(
            HomeItemViewData(
                HomeItemViewAdapter.VIEW_TYPE_HOME_CONTACT_ROW,
                getString(sharedR.string.not_working),
            )
        )
        // 11
        homeItemViewDataList.add(
            HomeItemViewData(
                HomeItemViewAdapter.VIEW_TYPE_HOME_TITLE_ROW,
                getString(sharedR.string.github_link),
            )
        )
        return homeItemViewDataList
    }

    private fun onItemClick(homeItemViewData: HomeItemViewData) {
        when(homeItemViewData.viewType) {
            HomeItemViewAdapter.VIEW_TYPE_HOME_GARDENER_ITEM_ROW -> {
                billingActionsHelper.handleGardenerClick()
            }
            HomeItemViewAdapter.VIEW_TYPE_HOME_ROSE_ITEM_ROW -> {
                billingActionsHelper.launchPurchaseFlowFor(BillingProduct.ROSE)
            }
            HomeItemViewAdapter.VIEW_TYPE_HOME_TREE_ITEM_ROW -> {
                billingActionsHelper.launchPurchaseFlowFor(BillingProduct.TREE)
            }
            HomeItemViewAdapter.VIEW_TYPE_HOME_SERVICE_ITEM_ROW -> {
                billingActionsHelper.launchPurchaseFlowFor(BillingProduct.WEEKLY_JOB)
            }
            HomeItemViewAdapter.VIEW_TYPE_HOME_LOG_PAGE_ROW -> {
                val intent = Intent(this, ExternalVisibleLogActivity::class.java)
                intent.setPackage(packageName)
                startActivity(intent)
            }
            HomeItemViewAdapter.VIEW_TYPE_HOME_CONTACT_ROW -> {
                Toast.makeText(
                    this,
                    getString(R.string.contact_toast),
                    Toast.LENGTH_LONG,
                    ).show()
            }
        }
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
            homeItemViewDataList[1] = HomeItemViewData(
                HomeItemViewAdapter.VIEW_TYPE_HOME_GARDENER_ITEM_ROW,
                getString(sharedR.string.reset_gardener),
            )
        } else {
            homeItemViewDataList[1] = HomeItemViewData(
                HomeItemViewAdapter.VIEW_TYPE_HOME_GARDENER_ITEM_ROW,
                getString(sharedR.string.be_gardener),
            )
        }
        if (BillingUtils.isWeeklySubActive()) {
            homeItemViewDataList[6] = HomeItemViewData(
                HomeItemViewAdapter.VIEW_TYPE_HOME_SERVICE_ITEM_ROW,
                getString(
                    sharedR.string.format_with_state,
                    getString(sharedR.string.weekly_sub),
                    getString(sharedR.string.active),
                ))
        } else {
            homeItemViewDataList[6] = HomeItemViewData(
                HomeItemViewAdapter.VIEW_TYPE_HOME_SERVICE_ITEM_ROW,
                getString(sharedR.string.weekly_sub))
        }
        homeItemViewDataList[3] = HomeItemViewData(
            HomeItemViewAdapter.VIEW_TYPE_HOME_ROSE_ITEM_ROW,
            getString(
                sharedR.string.format_with_state,
                getString(sharedR.string.buy_a_rose),
                billingActionsHelper.getRoseCount().toString(),
            ))
        homeItemViewDataList[4] = HomeItemViewData(
            HomeItemViewAdapter.VIEW_TYPE_HOME_TREE_ITEM_ROW,
            getString(
                sharedR.string.format_with_state,
                getString(sharedR.string.buy_a_tree),
                billingActionsHelper.getTreeCount().toString(),
            ))
        recyclerView.adapter?.notifyItemRangeChanged(1, 6)
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