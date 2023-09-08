package com.simple.phonetics.data.task

import android.content.Context
import com.simple.core.utils.extentions.toArrayList
import com.simple.core.utils.extentions.toJson
import com.simple.coreapp.utils.FileUtils
import com.simple.coreapp.utils.extentions.saveSync
import com.simple.phonetics.data.api.Api
import com.simple.phonetics.data.dao.PhoneticsDao
import com.simple.phonetics.data.dao.RoomPhonetics
import com.simple.phonetics.domain.entities.PhoneticsCode
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext


class ApiSyncTask(
    private val api: Api,
    private val context: Context,
    private val phoneticsDao: PhoneticsDao
) : SyncTask {

    override suspend fun executeTask(param: Unit) = withContext(coroutineContext) {


        val deferredList = arrayListOf<Deferred<Any>>()


        val textAndPhonetics = hashMapOf<String, RoomPhonetics>()


        async {

            api.syncPhoneticsEnUk().string().toPhonetics(textAndPhonetics, PhoneticsCode.UK)
        }.let {

            deferredList.add(it)
        }

        async {

            api.syncPhoneticsEnUs().string().toPhonetics(textAndPhonetics, PhoneticsCode.US)
        }.let {

            deferredList.add(it)
        }


        deferredList.awaitAll()


        textAndPhonetics.values.toList().let {

            phoneticsDao.insertOrUpdate(it)

            FileUtils.createFile(context, true, "raw", "en.json")?.saveSync(it.toJson())
        }

        Unit
    }

    private fun String.toPhonetics(textAndPhonetics: HashMap<String, RoomPhonetics>, code: PhoneticsCode) = split("\n").mapNotNull { phonetics ->

        val split = phonetics.split("\t", ", ").mapNotNull { ipa -> ipa.trim().takeIf { it.isNotBlank() } }.toArrayList()

        if (split.isEmpty()) return@mapNotNull null


        val text = split.removeAt(0)

        val ipa = split.map {

            var ipa = it

            if (!it.startsWith("/")) ipa = "/$it"
            if (!it.endsWith("/")) ipa = "$it/"

            ipa
        }


        val item = textAndPhonetics[text] ?: RoomPhonetics(text, hashMapOf()).apply {

            textAndPhonetics[text] = this
        }

        if (item.ipa.isEmpty() || (!item.ipa.values.flatten().containsAll(ipa) && !ipa.containsAll(item.ipa.values.flatten()))) {

            item.ipa[code.value] = ipa
        }

        item
    }
}