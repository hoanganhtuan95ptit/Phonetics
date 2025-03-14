package com.simple.phonetics.di

import com.simple.phonetics.domain.usecase.DetectStateUseCase
import com.simple.phonetics.domain.usecase.GetKeyTranslateAsyncUseCase
import com.simple.phonetics.domain.usecase.TranslateUseCase
import com.simple.phonetics.domain.usecase.ipa.GetIpaStateAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageOutputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageSupportUseCase
import com.simple.phonetics.domain.usecase.language.UpdateLanguageInputUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsHistoryAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsRandomUseCase
import com.simple.phonetics.domain.usecase.word.GetWordStateAsyncUseCase
import com.simple.phonetics.domain.usecase.speak.CheckSupportSpeakAsyncUseCase
import com.simple.phonetics.domain.usecase.speak.CheckSupportSpeakUseCase
import com.simple.phonetics.domain.usecase.speak.StartSpeakUseCase
import com.simple.phonetics.domain.usecase.speak.StopSpeakUseCase
import com.simple.phonetics.domain.usecase.voice.GetVoiceAsyncUseCase
import com.simple.phonetics.domain.usecase.voice.StartListenUseCase
import com.simple.phonetics.domain.usecase.voice.StopListenUseCase
import com.simple.phonetics.domain.usecase.word.CountWordAsyncUseCase
import org.koin.dsl.module

@JvmField
val useCaseModule = module {

    single {
        TranslateUseCase(get())
    }

    single {
        GetKeyTranslateAsyncUseCase(get(), get())
    }

    single {
        GetPhoneticsRandomUseCase(get(), get(), get())
    }

    single {
        GetPhoneticsAsyncUseCase(get(), get(), get(), get())
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
        StopListenUseCase(get())
    }

    single {
        StartListenUseCase(get())
    }

    single {
        GetVoiceAsyncUseCase(get(), get())
    }


    single {
        StopSpeakUseCase(get())
    }

    single {
        StartSpeakUseCase(get())
    }

    single {
        CheckSupportSpeakUseCase(get())
    }

    single {
        CheckSupportSpeakAsyncUseCase(get(), get())
    }


    single {
        DetectStateUseCase(getAll())
    }


    single {
        GetIpaStateAsyncUseCase(get(), get())
    }


    single {
        CountWordAsyncUseCase(get())
    }

    single {
        GetWordStateAsyncUseCase(get(), get(), get(), get())
    }
}