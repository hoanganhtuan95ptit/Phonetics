package com.simple.feature.subscription.data.repositories

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.simple.core.utils.AppException
import com.simple.feature.subscription.entities.SubscriptionPlan
import com.simple.phonetics.PhoneticsApp
import com.simple.state.ResultState
import com.simple.state.mapToData
import com.simple.state.toSuccess
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object SubscriptionRepository {


    val purchasesUpdatedListenerList = ArrayList<PurchasesUpdatedListener>()


    val billingClientAsync = object : MutableLiveData<BillingClient>() {

        private var job: Job? = null

        private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate)

        override fun onActive() {
            super.onActive()
            job = getDataAsync()
        }

        override fun onInactive() {
            super.onInactive()
            job?.cancel()
        }

        private fun getDataAsync() = coroutineScope.launch {

            var billingClient: BillingClient? = null

            billingClient = BillingClient.newBuilder(PhoneticsApp.share)
                .setListener { billingResult, purchases ->

                    purchasesUpdatedListenerList.forEach {
                        it.onPurchasesUpdated(billingResult, purchases)
                    }

                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                        postValue(billingClient)
                    }
                }
                .enablePendingPurchases()
                .build()

            billingClient.startConnection(object : BillingClientStateListener {

                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        postValue(billingClient)
                    }
                }

                override fun onBillingServiceDisconnected() {

                }
            })
        }
    }

    fun subscription(productId: String) = channelFlow {

        // 1. Lấy BillingClient và kiểm tra sẵn sàng
        val client = billingClientAsync.asFlow().filterNotNull().first()
        if (!client.isReady) {
            trySend(ResultState.Failed(RuntimeException("Billing Service chưa sẵn sàng")))
            return@channelFlow
        }


        // 2. Query thông tin sản phẩm
        val productDetails = query(listOf(productId)).first().toSuccess()?.data?.firstOrNull()
        if (productDetails == null) {
            trySend(ResultState.Failed(RuntimeException("Không tìm thấy sản phẩm")))
            return@channelFlow
        }

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            trySend(ResultState.Failed(RuntimeException("Không tìm thấy gói cước")))
            return@channelFlow
        }


        // 3. Chuẩn bị Listener để bắt kết quả TRƯỚC khi gọi launch
        val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->

            when (billingResult.responseCode) {

                BillingClient.BillingResponseCode.OK -> {
                    trySend(ResultState.Success(Unit))
                }

                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    trySend(ResultState.Failed(CancellationException()))
                }

                else -> {
                    trySend(ResultState.Failed(RuntimeException(billingResult.debugMessage)))
                }
            }
        }

        purchasesUpdatedListenerList.add(purchasesUpdatedListener)


        // 4. Launch Flow
        val activity = ActivityTracker.currentActivity.filterNotNull().first()

        val productDetailsParamsList = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParamsList))
            .build()

        val launchResult = client.launchBillingFlow(activity, billingFlowParams)

        // Kiểm tra nếu launch thất bại ngay lập tức (ví dụ: param sai)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            trySend(ResultState.Failed(RuntimeException(launchResult.debugMessage)))
        }

        awaitClose {
            purchasesUpdatedListenerList.remove(purchasesUpdatedListener)
        }
    }

    fun getSubscriptionIdStateAsync() = channelFlow {

        billingClientAsync.asFlow().launchCollect(this) { billingClient ->

            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->

                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync

                val activePurchase = purchases.firstOrNull {
                    it.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED
                }

                activePurchase?.products.orEmpty().firstOrNull()?.let {

                    trySend(ResultState.Success(it))
                }
            }
        }

        awaitClose {
        }
    }

    fun getSubscriptionPlanStateAsync(productIds: List<String>) = query(productIds).mapToData { details ->

        details.map {

            SubscriptionPlan(
                id = it.productId,
                price = it.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "",
            )
        }.sortedBy {

            productIds.indexOf(it.id)
        }
    }

    private fun query(productIds: List<String>) = channelFlow {

        billingClientAsync.asFlow().launchCollect(this) { billingClient ->

            val productList = productIds.map {
                QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(BillingClient.ProductType.SUBS).build()
            }

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            billingClient.queryProductDetailsAsync(params) { billingResult, detailsList ->

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && detailsList.isNotEmpty()) detailsList.let {

                    trySend(ResultState.Success(it))
                } else {

                    trySend(ResultState.Failed(AppException("")))
                }
            }
        }

        awaitClose {

        }
    }


}