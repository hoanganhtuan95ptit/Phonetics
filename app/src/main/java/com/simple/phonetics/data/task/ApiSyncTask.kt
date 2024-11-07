package com.simple.phonetics.data.task

import com.simple.phonetics.data.api.Api
import com.simple.phonetics.data.dao.PhoneticsDao
import com.simple.phonetics.data.dao.RoomPhonetics
import com.simple.phonetics.domain.repositories.LanguageRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext


class ApiSyncTask(
    private val api: Api,
    private val phoneticsDao: PhoneticsDao,
    private val languageRepository: LanguageRepository
) : SyncTask {

    override suspend fun executeTask(param: Unit) = withContext(coroutineContext) {


        val textAndPhonetics = hashMapOf<String, RoomPhonetics>()


        val deferredList = languageRepository.getLanguageOutput().listIpa.map {

            async {

                api.syncPhonetics(it.source).string().toPhonetics(textAndPhonetics, it.code)
            }
        }


        deferredList.awaitAll()


        textAndPhonetics.values.toList().let {

            phoneticsDao.insertOrUpdate(it)
        }

        Unit
    }

    private fun String.toPhonetics(textAndPhonetics: HashMap<String, RoomPhonetics>, ipaCode: String) = split("\n").mapNotNull { phonetics ->

        val split = phonetics.split("\t", ", ").mapNotNull { ipa -> ipa.trim().takeIf { it.isNotBlank() } }.toMutableList()

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

            item.ipa[ipaCode] = ipa
        }

        item
    }
}