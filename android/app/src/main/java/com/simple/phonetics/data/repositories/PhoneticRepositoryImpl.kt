package com.simple.phonetics.data.repositories

import com.simple.phonetics.data.api.Api
import com.simple.phonetics.data.dao.PhoneticsDao
import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.entities.Phonetics

class PhoneticRepositoryImpl(
    private val api: Api,
    private val phoneticsDao: PhoneticsDao
) : PhoneticRepository {

    override suspend fun toPhonetics(dataSplit: String, code: String): Map<String, Phonetics> {

        val textAndPhonetics = hashMapOf<String, Phonetics>()

        dataSplit.toPhonetics(textAndPhonetics, code)

        return textAndPhonetics
    }

    override suspend fun getSourcePhonetic(it: Ipa): String {

        return api.syncPhonetics(it.source).string()
    }

    override suspend fun getPhonetics(phonetics: List<String>): List<Phonetics> {

        return phoneticsDao.getRoomListByTextList(phonetics).map {

            Phonetics(
                text = it.text,
            ).apply {

                ipa = it.ipa
            }
        }
    }

    override suspend fun insertOrUpdate(phonetics: List<Phonetics>) {

        phoneticsDao.insertOrUpdateEntities(phonetics)
    }

    private fun String.toPhonetics(textAndPhonetics: HashMap<String, Phonetics>, ipaCode: String) = split("\n").mapNotNull { phonetics ->

        val split = phonetics.split("\t", ", ").mapNotNull { ipa -> ipa.trim().takeIf { it.isNotBlank() } }.toMutableList()

        if (split.isEmpty()) return@mapNotNull null


        val text = split.removeAt(0)

        val ipa = split.map {

            var ipa = it

            if (!it.startsWith("/")) ipa = "/$it"
            if (!it.endsWith("/")) ipa = "$it/"

            ipa
        }


        val item = textAndPhonetics[text] ?: Phonetics(text).apply {

            textAndPhonetics[text] = this
        }

        if (item.ipa.isEmpty() || (!item.ipa.values.flatten().containsAll(ipa) && !ipa.containsAll(item.ipa.values.flatten()))) {

            item.ipa[ipaCode] = ipa
        }

        item
    }
}