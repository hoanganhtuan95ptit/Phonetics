# IPA audio assets — download script

`download_ipa_audio.py` populates
`feature/ipa_voice_en/src/main/assets/voices/` with one `.mp3` per IPA phoneme
(UK + US), matching the naming convention that `LocalIpaReading.kt` expects.

## Quick start

```bash
brew install ffmpeg            # one-time
python3 feature/ipa_voice_en/scripts/download_ipa_audio.py
```

That's it. After it finishes, build the module — the assets are picked up automatically.

## How it works

| Phoneme group | Source | Notes |
|---------------|--------|-------|
| Monophthong vowels (11) | Wikimedia Commons phoneme reference recordings | Pure articulatory demo; CC-licensed |
| Consonants (24) | Wikimedia Commons phoneme reference recordings | Same |
| Diphthongs (8) | macOS `say` TTS fallback | Commons has no single-file diphthong audio |

UK and US get the **same** source file — the phoneme is articulated identically
in both dialects (only the dictionary symbol conventions differ). If you later
want distinct UK/US recordings for specific phonemes, just drop them into
`assets/voices/` with the right name and the script will leave them alone (unless
you pass `--force`).

## Filename convention (from `LocalIpaReading.kt`)

```kotlin
var fileName = ipa.ipa.replace("/", "").replace("ː", "_").lowercase() + ".mp3"
if (phoneticCode.equals("us", true)) fileName = "us_$fileName"
```

Examples:

| IPA   | UK file    | US file       |
|-------|------------|---------------|
| /iː/  | `i_.mp3`   | `us_i_.mp3`   |
| /æ/   | `æ.mp3`    | `us_æ.mp3`    |
| /tʃ/  | `tʃ.mp3`   | `us_tʃ.mp3`   |
| /ə/   | `ə.mp3`    | `us_ə.mp3`    |

## Replacing individual files

Want a different recording for `/r/`? Just overwrite `r.mp3` and `us_r.mp3` in
`assets/voices/`. The script never deletes existing files unless you pass
`--force`.

## Flags

| Flag | Effect |
|------|--------|
| `--output PATH` | Write somewhere else |
| `--skip-diphthongs` | Don't run the TTS fallback (fill them in yourself) |
| `--force` | Overwrite existing files |
| `--uk-voice NAME` | macOS `say` voice for UK diphthongs (default `Daniel`) |
| `--us-voice NAME` | macOS `say` voice for US diphthongs (default `Samantha`) |

Run `say -v ?` to list available voices.

## Why not bundle the mp3s in git?

* Wikimedia files are CC-licensed but their attribution is easier to maintain
  if we fetch them at build-time.
* Keeps the repo small.
* Lets you swap recordings without touching the script.
