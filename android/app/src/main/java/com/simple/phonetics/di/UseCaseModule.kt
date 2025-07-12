package com.simple.phonetics.di

import com.simple.phonetics.domain.usecase.GetConfigAsyncUseCase
import com.simple.phonetics.domain.usecase.GetTranslateAsyncUseCase
import com.simple.phonetics.domain.usecase.SyncDataUseCase
import com.simple.phonetics.domain.usecase.detect.CheckSupportDetectUseCase
import com.simple.phonetics.domain.usecase.detect.DetectUseCase
import com.simple.phonetics.domain.usecase.event.GetCurrentEventAsyncUseCase
import com.simple.phonetics.domain.usecase.event.UpdateEventShowUseCase
import com.simple.phonetics.domain.usecase.ipa.CountIpaAsyncUseCase
import com.simple.phonetics.domain.usecase.ipa.GetIpaStateAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageSupportAsyncUseCase
import com.simple.phonetics.domain.usecase.language.input.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.input.GetLanguageInputUseCase
import com.simple.phonetics.domain.usecase.language.input.UpdateLanguageInputUseCase
import com.simple.phonetics.domain.usecase.language.output.GetLanguageOutputAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsHistoryAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsRandomUseCase
import com.simple.phonetics.domain.usecase.phonetics.SyncPhoneticAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.code.GetPhoneticCodeSelectedAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.code.UpdatePhoneticCodeSelectedUseCase
import com.simple.phonetics.domain.usecase.phonetics.suggest.GetPhoneticsSuggestUseCase
import com.simple.phonetics.domain.usecase.reading.CheckSupportReadingAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.StartReadingUseCase
import com.simple.phonetics.domain.usecase.reading.StopReadingUseCase
import com.simple.phonetics.domain.usecase.reading.voice.GetVoiceAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.voice.selected.GetVoiceIdSelectedAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.voice.selected.UpdateVoiceIdSelectedUseCase
import com.simple.phonetics.domain.usecase.reading.voice.speed.GetVoiceSpeedAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.voice.speed.UpdateVoiceSpeedUseCase
import com.simple.phonetics.domain.usecase.speak.CheckSupportSpeakAsyncUseCase
import com.simple.phonetics.domain.usecase.speak.CheckSupportSpeakUseCase
import com.simple.phonetics.domain.usecase.speak.StartSpeakUseCase
import com.simple.phonetics.domain.usecase.speak.StopSpeakUseCase
import com.simple.phonetics.domain.usecase.translate.CheckSupportTranslateUseCase
import com.simple.phonetics.domain.usecase.translate.TranslateUseCase
import com.simple.phonetics.domain.usecase.translate.selected.GetTranslateSelectedAsyncUseCase
import com.simple.phonetics.domain.usecase.translate.selected.UpdateTranslateSelectedUseCase
import com.simple.phonetics.domain.usecase.word.CountWordAsyncUseCase
import com.simple.phonetics.domain.usecase.word.GetListWordResourceCountAsyncUseCase
import org.koin.dsl.module

@JvmField
val useCaseModule = module {

    single {
        SyncDataUseCase(getAll(), get())
    }

    single {
        DetectUseCase(get())
    }

    single {
        TranslateUseCase(get())
    }

    single {
        CheckSupportTranslateUseCase(get())
    }

    single {
        GetPhoneticsSuggestUseCase(get())
    }

    single {
        GetConfigAsyncUseCase(get())
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
        GetPhoneticCodeSelectedAsyncUseCase(get())
    }

    single {
        UpdatePhoneticCodeSelectedUseCase(get())
    }


    single {
        GetVoiceIdSelectedAsyncUseCase(get())
    }

    single {
        UpdateVoiceIdSelectedUseCase(get())
    }


    single {
        StopReadingUseCase(get())
    }

    single {
        StartReadingUseCase(get(), get())
    }

    single {
        GetVoiceAsyncUseCase(get(), get())
    }

    single {
        CheckSupportReadingAsyncUseCase(get(), get())
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
        DetectUseCase(get())
    }

    single {
        CheckSupportDetectUseCase(get())
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
        GetListWordResourceCountAsyncUseCase(get())
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


    single {
        UpdateTranslateSelectedUseCase(get())
    }

    single {
        GetTranslateSelectedAsyncUseCase(get())
    }


    single {
        UpdateVoiceSpeedUseCase(get())
    }

    single {
        GetVoiceSpeedAsyncUseCase(get())
    }
}