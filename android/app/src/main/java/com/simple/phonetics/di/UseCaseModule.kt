package com.simple.phonetics.di

import com.simple.phonetics.domain.usecase.DetectStateUseCase
import com.simple.phonetics.domain.usecase.SyncUseCase
import com.simple.phonetics.domain.usecase.TranslateUseCase
import com.simple.phonetics.domain.usecase.key_translate.GetKeyTranslateAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageOutputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageSupportUseCase
import com.simple.phonetics.domain.usecase.voice.GetVoiceAsyncUseCase
import com.simple.phonetics.domain.usecase.voice.StartSpeakUseCase
import com.simple.phonetics.domain.usecase.voice.StopSpeakUseCase
import com.simple.phonetics.domain.usecase.language.UpdateLanguageInputUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsHistoryAsyncUseCase
import org.koin.dsl.module

@JvmField
val useCaseModule = module {

    single {
        SyncUseCase(getAll())
    }

    single {
        TranslateUseCase(get())
    }

    single {
        GetKeyTranslateAsyncUseCase(get(), get())
    }

    single {
        GetPhoneticsAsyncUseCase(get(), get(), get())
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
        UpdateLanguageInputUseCase(get(), get(), get())
    }


    single {
        StopSpeakUseCase(get())
    }

    single {
        StartSpeakUseCase(get())
    }

    single {
        GetVoiceAsyncUseCase(get(), get())
    }

    single {
        DetectStateUseCase(getAll())
    }
}