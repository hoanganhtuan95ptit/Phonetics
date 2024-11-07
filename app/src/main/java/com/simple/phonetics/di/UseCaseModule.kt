package com.simple.phonetics.di

import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsHistoryAsyncUseCase
import com.simple.phonetics.domain.usecase.SyncUseCase
import com.simple.phonetics.domain.usecase.TranslateUseCase
import com.simple.phonetics.domain.usecase.key_translate.GetKeyTranslateAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageOutputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetVoiceAsyncUseCase
import com.simple.phonetics.domain.usecase.language.StartSpeakUseCase
import com.simple.phonetics.domain.usecase.language.StopSpeakUseCase
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
        GetLanguageInputAsyncUseCase(get())
    }

    single {
        GetLanguageOutputAsyncUseCase(get())
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
}