package com.simple.phonetics.di

import com.simple.phonetics.domain.usecase.DetectStateUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsHistoryAsyncUseCase
import com.simple.phonetics.domain.usecase.SyncUseCase
import com.simple.phonetics.domain.usecase.TranslateUseCase
import com.simple.phonetics.domain.usecase.key_translate.GetKeyTranslateAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageOutputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageSupportUseCase
import com.simple.phonetics.domain.usecase.language.GetVoiceAsyncUseCase
import com.simple.phonetics.domain.usecase.language.StartSpeakUseCase
import com.simple.phonetics.domain.usecase.language.StopSpeakUseCase
import com.simple.phonetics.domain.usecase.language.UpdateLanguageInputUseCase
import org.koin.dsl.module

@JvmField
val useCaseModule = module {

    single {
        SyncUseCase(getAll())
    }

    single {
        TranslateUseCase(getAll())
    }

    single {
        GetKeyTranslateAsyncUseCase(get())
    }

    single {
        GetPhoneticsAsyncUseCase(get(), get(), getAll())
    }

    single {
        GetPhoneticsHistoryAsyncUseCase(get())
    }

    single {
        GetLanguageInputUseCase(get())
    }

    single {
        GetLanguageInputAsyncUseCase(get())
    }

    single {
        GetLanguageOutputAsyncUseCase(get())
    }

    single {
        GetLanguageSupportUseCase(get())
    }

    single {
        UpdateLanguageInputUseCase(get())
    }


    single {
        StopSpeakUseCase(get())
    }

    single {
        StartSpeakUseCase(get())
    }

    single {
        GetVoiceAsyncUseCase(get())
    }

    single {
        DetectStateUseCase(getAll())
    }
}