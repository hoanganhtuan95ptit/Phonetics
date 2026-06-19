package com.simple.feature.subscription

import com.simple.feature.subscription.data.repositories.SubscriptionRepository
import com.simple.feature.subscription.entities.SubscriptionPlan
import com.simple.phonetics.domain.usecase.GetConfigAsyncUseCase
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.combineSourcesWithDiff
import com.simple.phonetics.utils.exts.get
import com.simple.phonetics.utils.exts.listenerSourcesWithDiff
import com.simple.phonetics.utils.exts.mutableSharedFlowWithDiff
import com.simple.state.ResultState
import com.simple.state.doSuccess
import com.simple.state.toSuccess
import kotlinx.coroutines.flow.Flow

class SubscriptionHomeViewModel : BaseViewModel() {

    val config: Flow<Map<String, String>> = mutableSharedFlowWithDiff {

        GetConfigAsyncUseCase.install.execute().collect {

            emit(it)
        }
    }

    val subscriptionIdOld: Flow<String> = mutableSharedFlowWithDiff {

        emit("")

        SubscriptionRepository.getSubscriptionIdStateAsync().collect { state ->

            state.doSuccess {

                emit(it)
            }
        }
    }

    val subscriptionPlanListState: Flow<ResultState<List<SubscriptionPlan>>> = combineSourcesWithDiff(config) {

        val productIds = config.get()["subscription_ids"]?.split(",").orEmpty()
            .map { it.trim() }
            .takeIf { it.isNotEmpty() }

        SubscriptionRepository.getSubscriptionPlanStateAsync(productIds ?: emptyList()).collect {

            emit(it)
        }
    }

    val subscriptionPlanViewItemList: Flow<List<SubscriptionPlan>> = listenerSourcesWithDiff(subscriptionPlanListState) {

        emit(subscriptionPlanListState.get().toSuccess()?.data.orEmpty())
    }
}