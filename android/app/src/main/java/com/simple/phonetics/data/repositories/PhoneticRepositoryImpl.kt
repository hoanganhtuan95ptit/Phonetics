package com.simple.phonetics.data.repositories

import com.simple.phonetics.data.api.Api
import com.simple.phonetics.data.dao.PhoneticDao
import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.Phonetic

class PhoneticRepositoryImpl(
    private val api: Api,
    private val phoneticDao: PhoneticDao
) : PhoneticRepository {

    override suspend fun toPhonetics(dataSplit: String, code: String): Map<String, Phonetic> {

        val textAndPhonetic = hashMapOf<String, Phonetic>()

        dataSplit.toPhonetics(textAndPhonetic, code)

        return textAndPhonetic
    }

    override suspend fun getSourcePhonetic(it: Language.IpaSource): String {

        return api.syncPhonetics(it.source).string()
    }

    override suspend fun getPhonetics(phonetics: List<String>): List<Phonetic> {

        return phoneticDao.getListByTextList(phonetics)
    }

    override suspend fun insertOrUpdate(phonetics: List<Phonetic>) {

        phoneticDao.insertOrUpdateEntities(phonetics)
    }

    private fun String.toPhonetics(textAndPhonetic: HashMap<String, Phonetic>, ipaCode: String) = split("\n").mapNotNull { phonetics ->

        val split = phonetics.split("\t", ", ").mapNotNull { ipa -> ipa.trim().takeIf { it.isNotBlank() } }.toMutableList()

        if (split.isEmpty()) return@mapNotNull null


        val text = split.removeAt(0)

        val ipa = split.map {

            var ipa = it

            if (!it.startsWith("/")) ipa = "/$it"
            if (!it.endsWith("/")) ipa = "$it/"

            ipa
        }


        val item = textAndPhonetic[text] ?: Phonetic(text).apply {

            textAndPhonetic[text] = this
        }

        if (item.ipa.isEmpty() || (!item.ipa.values.flatten().containsAll(ipa) && !ipa.containsAll(item.ipa.values.flatten()))) {

            item.ipa[ipaCode] = ipa
        }

        item
    }
}