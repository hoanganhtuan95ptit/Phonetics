package com.simple.phonetics.ui.main.services.dynamic

import androidx.lifecycle.lifecycleScope
import com.simple.analytics.logAnalytics
import com.simple.autobind.annotation.AutoBind
import com.simple.coreapp.utils.ext.handler
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.BuildConfig
import com.simple.phonetics.entities.Language
import com.simple.phonetics.ui.main.MainActivity
import com.simple.phonetics.ui.main.services.MainService
import com.simple.startapp.StartApp
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.state.isCompleted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AutoBind(MainActivity::class)
class DynamicFeatureService : MainService {

    override fun setup(mainActivity: MainActivity) {

        mainActivity.lifecycleScope.launch(handler + Dispatchers.IO) {

            if (BuildConfig.DEBUG) {
                delay(5 * 1000)
            }


            val featureListDefault = arrayOf("mlkit", "campaign", "thanks")

            featureListDefault.map { moduleName ->

                downloadModule(moduleName = moduleName)
            }


            val featureList = arrayListOf<String>()

            if (mainActivity.viewModel.inputLanguageFlow.filterNotNull().first().id.equals(Language.Companion.EN, true)) {

                featureList.add("ipa_voice_en")
            }

            featureList.map { moduleName ->

                downloadModule(moduleName = moduleName)
            }


            if (BuildConfig.DEBUG) arrayOf("mlkit").let { moduleName ->

                deleteModule(moduleName = moduleName)
            }
        }
    }

    private suspend fun downloadModule(moduleName: String): ResultState<Int> {

        val downloadState = StartApp.downloadModuleAsync(moduleName = moduleName).filter { it.isCompleted() }.first()

        downloadState.doSuccess {
            logAnalytics("dynamic_feature1_download_${moduleName.lowercase()}_${it}")
        }

        downloadState.doFailed {
            logCrashlytics("dynamic_feature1_download_module_${moduleName.lowercase()}", it)
        }

        return downloadState
    }

    private suspend fun deleteModule(vararg moduleName: String): ResultState<Int> {

        val deleteState = StartApp.deleteModuleAsync(*moduleName).first()

        deleteState.doSuccess {
            logAnalytics("dynamic_feature1_delete_status_${it}")
        }

        deleteState.doFailed {
            logCrashlytics("dynamic_feature1_delete", it)
        }

        return deleteState
    }
}