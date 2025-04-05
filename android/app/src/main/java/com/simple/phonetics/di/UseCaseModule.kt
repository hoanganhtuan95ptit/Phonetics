package com.simple.phonetics.di

import com.simple.phonetics.domain.usecase.DetectStateUseCase
import com.simple.phonetics.domain.usecase.GetTranslateAsyncUseCase
import com.simple.phonetics.domain.usecase.SyncDataUseCase
import com.simple.phonetics.domain.usecase.TranslateUseCase
import com.simple.phonetics.domain.usecase.event.GetCurrentEventAsyncUseCase
import com.simple.phonetics.domain.usecase.event.UpdateEventShowUseCase
import com.simple.phonetics.domain.usecase.ipa.CountIpaAsyncUseCase
import com.simple.phonetics.domain.usecase.ipa.GetIpaStateAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageOutputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageSupportAsyncUseCase
import com.simple.phonetics.domain.usecase.language.UpdateLanguageInputUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsHistoryAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsRandomUseCase
import com.simple.phonetics.domain.usecase.phonetics.SyncPhoneticAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.GetVoiceAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.StartReadingUseCase
import com.simple.phonetics.domain.usecase.reading.StopReadingUseCase
import com.simple.phonetics.domain.usecase.speak.CheckSupportSpeakAsyncUseCase
import com.simple.phonetics.domain.usecase.speak.CheckSupportSpeakUseCase
import com.simple.phonetics.domain.usecase.speak.StartSpeakUseCase
import com.simple.phonetics.domain.usecase.speak.StopSpeakUseCase
import com.simple.phonetics.domain.usecase.word.CountWordAsyncUseCase
import com.simple.phonetics.domain.usecase.word.GetWordStateAsyncUseCase
import org.koin.dsl.module

@JvmField
val useCaseModule = module {

    single {
        SyncDataUseCase(get(), get())
    }

    single {
        TranslateUseCase(get())
    }

    single {
        GetTranslateAsyncUseCase(get(), get())
    }

    single {
        GetLanguageSupportAsyncUseCase(get())
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
        UpdateLanguageInputUseCase(get(), get(), get())
    }


    single {
        StopReadingUseCase(get())
    }

    single {
        StartReadingUseCase(get())
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
        CountIpaAsyncUseCase(get())
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

    single {
        SyncPhoneticAsyncUseCase(get(), get(), get())
    }

    single {
        UpdateEventShowUseCase(get())
    }

    single {
        GetCurrentEventAsyncUseCase(get())
    }
}