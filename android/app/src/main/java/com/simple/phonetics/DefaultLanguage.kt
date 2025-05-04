package com.simple.phonetics

import com.simple.phonetics.entities.Language

val DEFAULT_LANGUAGE = listOf(
    Language(
        id = Language.EN,
        name = "English",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/main/flags/en.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "UK",
                name = "English - UK",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/en_UK.txt"
            ),
            Language.IpaSource(
                code = "US",
                name = "English - US",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/en_US.txt"
            )
        )
    ),
    Language(
        id = Language.VI,
        name = "Vietnamese",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/main/flags/vi.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "N",
                name = "Northern",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/vi_N.txt"
            ),
            Language.IpaSource(
                code = "C",
                name = "Central",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/vi_C.txt"
            ),
            Language.IpaSource(
                code = "S",
                name = "Southern",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/vi_S.txt"
            )
        )
    ),
    Language(
        id = Language.ZH,
        name = "Chinese",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_zh.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "zh_Hans",
                name = "Simplified Chinese",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/zh_hans.txt"
            ),
            Language.IpaSource(
                code = "zh_Hant",
                name = "Traditional Chinese",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/zh_hant.txt"
            ),
            Language.IpaSource(
                code = "yue",
                name = "Cantonese",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/yue.txt"
            )
        )
    ),
    Language(
        id = Language.ES,
        name = "Spain, Mexico",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_es.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "es_ES",
                name = "Spanish (Spain)",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/es_ES.txt"
            ),
            Language.IpaSource(
                code = "es_MX",
                name = "Spanish (Mexico)",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/es_MX.txt"
            )
        )
    ),
    Language(
        id = Language.FR,
        name = "France, Québec",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_fr.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "fr_FR",
                name = "French (France)",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/fr_FR.txt"
            ),
            Language.IpaSource(
                code = "fr_QC",
                name = "French (Québec)",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/fr_QC.txt"
            )
        )
    ),
    Language(
        id = Language.AR,
        name = "Arab",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_ar.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "ar",
                name = "Arabic (Modern Standard)",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/ar.txt"
            )
        )
    ),
    Language(
        id = Language.DE,
        name = "Germany",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_de.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "de",
                name = "German",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/de.txt"
            )
        )
    ),
    Language(
        id = Language.EO,
        name = "Esperanto",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_eo.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "eo",
                name = "Esperanto",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/eo.txt"
            )
        )
    ),
    Language(
        id = Language.FA,
        name = "Iran",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_fa.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "fa",
                name = "Persian",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/fa.txt"
            )
        )
    ),
    Language(
        id = Language.FI,
        name = "Finland",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_fi.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "fi",
                name = "Finnish",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/fi.txt"
            )
        )
    ),
    Language(
        id = Language.IS,
        name = "Iceland",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_is.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "is",
                name = "Icelandic",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/is.txt"
            )
        )
    ),
    Language(
        id = Language.JA,
        name = "Japan",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_ja.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "ja",
                name = "Japanese",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/ja.txt"
            )
        )
    ),
    Language(
        id = Language.KM,
        name = "Cambodia",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_km.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "km",
                name = "Khmer",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/km.txt"
            )
        )
    ),
    Language(
        id = Language.KO,
        name = "South Korea",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_ko.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "ko",
                name = "Korean",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/ko.txt"
            )
        )
    ),
    Language(
        id = Language.MA,
        name = "Malaysia, Indonesia",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/malay.png",
        listIpa = listOf(
            Language.IpaSource(
                code = "ma",
                name = "Malay (Malaysian and Indonesian)",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/ma.txt"
            )
        )
    ),
    Language(
        id = Language.NB,
        name = "Norway",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/norway.png",
        listIpa = listOf(
            Language.IpaSource(
                code = "nb",
                name = "Norwegian Bokmål",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/nb.txt"
            )
        )
    ),
    Language(
        id = Language.NL,
        name = "Netherlands",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_nl.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "nl",
                name = "Dutch",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/nl.txt"
            )
        )
    ),
    Language(
        id = Language.OR,
        name = "India",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/india.png",
        listIpa = listOf(
            Language.IpaSource(
                code = "or",
                name = "Odia",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/or.txt"
            )
        )
    ),
    Language(
        id = Language.RO,
        name = "Romania",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_ro.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "ro",
                name = "Romanian",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/ro.txt"
            )
        )
    ),
    Language(
        id = Language.SV,
        name = "Sweden",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_sv.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "sv",
                name = "Swedish",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/sv.txt"
            )
        )
    ),
    Language(
        id = Language.SW,
        name = "Swahili",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/lens_flag_sw.webp",
        listIpa = listOf(
            Language.IpaSource(
                code = "sw",
                name = "Swahili",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/sw.txt"
            )
        )
    ),
    Language(
        id = Language.TTS,
        name = "Thailand",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/isan.png",
        listIpa = listOf(
            Language.IpaSource(
                code = "tts",
                name = "Isan",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/tts.txt"
            )
        )
    ),
    Language(
        id = Language.JAM,
        name = "Jamaican Creole",
        image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/data/refs/heads/main/flag/jam.png",
        listIpa = listOf(
            Language.IpaSource(
                code = "jam",
                name = "Jamaican Creole",
                source = "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/jam.txt"
            )
        )
    )
)