#!/usr/bin/env python3
"""
Download IPA phoneme audio for EPhonetics (feature/ipa_voice_en).

Source : Wikimedia Commons (CC license — phoneme reference recordings)
Target : feature/ipa_voice_en/src/main/assets/voices/
Format : uk_<ipa>.mp3 (UK)  and  us_<ipa>.mp3 (US)
         — naming follows LocalIpaReading.kt: strip '/', prefix with dialect.

Why this script (instead of bundling files into the repo)
---------------------------------------------------------
* Wikimedia Commons hosts pure-phoneme reference audio for monophthongs and consonants.
* Pulling them at build-time keeps the repo small and respects the original licenses.
* English diphthongs do NOT have single-file reference recordings on Commons, so the
  script falls back to local TTS (macOS `say`) for those. You can replace those files
  with better recordings later.

Requirements
------------
* Python 3.7+
* ffmpeg    (brew install ffmpeg)              — for OGG → MP3 conversion
* `say`     (built-in on macOS)                — only used for diphthong fallback

Usage
-----
From the repo root:

    python3 feature/ipa_voice_en/scripts/download_ipa_audio.py

Flags:
    --output PATH       Override output dir (default: assets/voices in this module)
    --skip-diphthongs   Don't run the TTS fallback (you'll fill those in yourself)
    --force             Overwrite existing files
"""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

# ──────────────────────────────────────────────────────────────────────────────
# IPA → Wikimedia Commons file mapping
#
# `None` means "no good Wikimedia source" — handled via TTS fallback below.
# All these phonemes have the same articulation in UK and US, so the same source
# file is copied to both <ipa>.mp3 and us_<ipa>.mp3.
# ──────────────────────────────────────────────────────────────────────────────

PHONEMES: list[tuple[str, str | None]] = [
    # Monophthong vowels
    ("iː", "Close_front_unrounded_vowel.ogg"),
    ("ɪ",  "Near-close_near-front_unrounded_vowel.ogg"),
    ("e",  "Open-mid_front_unrounded_vowel.ogg"),
    ("æ",  "Near-open_front_unrounded_vowel.ogg"),
    ("ʌ",  "Open-mid_back_unrounded_vowel.ogg"),
    ("ɑː", "Open_back_unrounded_vowel.ogg"),
    ("ɔː", "Open-mid_back_rounded_vowel.ogg"),
    ("ʊ",  "Near-close_near-back_rounded_vowel.ogg"),
    ("uː", "Close_back_rounded_vowel.ogg"),
    ("ɜː", "Open-mid_central_unrounded_vowel.ogg"),
    ("ə",  "Mid-central_vowel.ogg"),

    # Diphthongs — no single-file source on Commons → TTS fallback
    ("eɪ", None),
    ("aɪ", None),
    ("ɔɪ", None),
    ("aʊ", None),
    ("oʊ", None),
    ("ɪə", None),
    ("eə", None),
    ("ʊə", None),

    # Voiced consonants
    ("b",  "Voiced_bilabial_plosive.ogg"),
    ("d",  "Voiced_alveolar_plosive.ogg"),
    ("g",  "Voiced_velar_plosive.ogg"),
    ("v",  "Voiced_labiodental_fricative.ogg"),
    ("ð",  "Voiced_dental_fricative.ogg"),
    ("z",  "Voiced_alveolar_sibilant.ogg"),
    ("ʒ",  "Voiced_palato-alveolar_sibilant.ogg"),
    ("dʒ", "Voiced_palato-alveolar_affricate.ogg"),
    ("m",  "Bilabial_nasal.ogg"),
    ("n",  "Alveolar_nasal.ogg"),
    ("ŋ",  "Velar_nasal.ogg"),
    ("l",  "Alveolar_lateral_approximant.ogg"),
    ("r",  "Alveolar_approximant.ogg"),
    ("w",  "Voiced_labio-velar_approximant.ogg"),
    ("j",  "Palatal_approximant.ogg"),

    # Voiceless consonants
    ("p",  "Voiceless_bilabial_plosive.ogg"),
    ("t",  "Voiceless_alveolar_plosive.ogg"),
    ("k",  "Voiceless_velar_plosive.ogg"),
    ("f",  "Voiceless_labiodental_fricative.ogg"),
    ("θ",  "Voiceless_dental_fricative.ogg"),
    ("s",  "Voiceless_alveolar_sibilant.ogg"),
    ("ʃ",  "Voiceless_palato-alveolar_sibilant.ogg"),
    ("tʃ", "Voiceless_palato-alveolar_affricate.ogg"),
    ("h",  "Voiceless_glottal_fricative.ogg"),
]

# Plain-text spelling used when falling back to TTS for diphthongs.
# These are NOT IPA — they're English approximations a TTS engine can handle.
TTS_HINTS = {
    "eɪ": "ay",
    "aɪ": "eye",
    "ɔɪ": "oy",
    "aʊ": "ow",
    "oʊ": "oh",
    "ɪə": "eer",
    "eə": "air",
    "ʊə": "oor",
}

WIKIMEDIA_URL = "https://commons.wikimedia.org/wiki/Special:FilePath/{}"
USER_AGENT = "EPhoneticsBot/1.0 (https://github.com/hoanganhtuan95ptit/EPhonetics; build asset script)"

# Default voices for macOS `say`. Override on the command line if you want.
UK_VOICE = "Daniel"
US_VOICE = "Samantha"


# ──────────────────────────────────────────────────────────────────────────────
# Helpers
# ──────────────────────────────────────────────────────────────────────────────

def ipa_to_filename(ipa: str, *, dialect: str) -> str:
    """Match LocalIpaReading.kt: strip '/', prefix with dialect, + .mp3"""
    base = ipa.replace("/", "").strip()
    return f"{dialect}_{base}.mp3"


def have(cmd: str) -> bool:
    return shutil.which(cmd) is not None


def download(url: str, dest: Path) -> None:
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=30) as resp, dest.open("wb") as out:
        shutil.copyfileobj(resp, out)


def ogg_to_mp3(src: Path, dst: Path) -> None:
    subprocess.run(
        ["ffmpeg", "-y", "-i", str(src), "-codec:a", "libmp3lame", "-qscale:a", "2", str(dst)],
        check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )


def say_to_mp3(text: str, voice: str, dst: Path) -> None:
    """macOS `say` → AIFF → MP3."""
    aiff = dst.with_suffix(".aiff")
    subprocess.run(["say", "-v", voice, "-o", str(aiff), text], check=True)
    try:
        subprocess.run(
            ["ffmpeg", "-y", "-i", str(aiff), "-codec:a", "libmp3lame", "-qscale:a", "2", str(dst)],
            check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        )
    finally:
        aiff.unlink(missing_ok=True)


# ──────────────────────────────────────────────────────────────────────────────
# Main
# ──────────────────────────────────────────────────────────────────────────────

def main() -> int:
    here = Path(__file__).resolve().parent
    default_out = here.parent / "src" / "main" / "assets" / "voices"

    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--output", type=Path, default=default_out,
                    help=f"Output dir (default: {default_out})")
    ap.add_argument("--skip-diphthongs", action="store_true",
                    help="Skip TTS fallback for diphthongs")
    ap.add_argument("--force", action="store_true",
                    help="Overwrite existing files")
    ap.add_argument("--uk-voice", default=UK_VOICE, help=f"macOS say voice for UK (default: {UK_VOICE})")
    ap.add_argument("--us-voice", default=US_VOICE, help=f"macOS say voice for US (default: {US_VOICE})")
    args = ap.parse_args()

    if not have("ffmpeg"):
        print("ERROR: ffmpeg not found. Install it (brew install ffmpeg) and re-run.", file=sys.stderr)
        return 1

    out_dir: Path = args.output
    out_dir.mkdir(parents=True, exist_ok=True)
    tmp_dir = out_dir / ".tmp"
    tmp_dir.mkdir(exist_ok=True)

    done: list[str] = []
    skipped: list[tuple[str, str]] = []
    failed: list[tuple[str, str]] = []

    print(f"→ Output: {out_dir}")
    print(f"→ {len(PHONEMES)} phonemes × 2 dialects = {len(PHONEMES) * 2} files\n")

    for ipa, src_file in PHONEMES:
        uk_path = out_dir / ipa_to_filename(ipa, dialect="uk")
        us_path = out_dir / ipa_to_filename(ipa, dialect="us")

        if uk_path.exists() and us_path.exists() and not args.force:
            skipped.append((ipa, "already exists"))
            continue

        if src_file is not None:
            # Wikimedia phoneme reference
            ogg = tmp_dir / src_file
            try:
                if not ogg.exists():
                    print(f"  ↓ {src_file}")
                    download(WIKIMEDIA_URL.format(src_file), ogg)
                    time.sleep(0.3)  # be polite

                master = ogg.with_suffix(ogg.suffix + ".mp3")
                if not master.exists() or args.force:
                    ogg_to_mp3(ogg, master)

                shutil.copyfile(master, uk_path)
                shutil.copyfile(master, us_path)
                done += [uk_path.name, us_path.name]
            except (urllib.error.URLError, subprocess.CalledProcessError, OSError) as e:
                failed.append((ipa, f"{type(e).__name__}: {e}"))
        else:
            # Diphthong fallback
            if args.skip_diphthongs:
                skipped.append((ipa, "diphthong (skipped)"))
                continue
            if not have("say"):
                skipped.append((ipa, "diphthong; `say` not available — install on macOS or fill in manually"))
                continue
            text = TTS_HINTS[ipa]
            try:
                say_to_mp3(text, args.uk_voice, uk_path)
                say_to_mp3(text, args.us_voice, us_path)
                done += [uk_path.name, us_path.name]
                print(f"  🗣 TTS  /{ipa}/  →  '{text}'")
            except subprocess.CalledProcessError as e:
                failed.append((ipa, f"say failed: {e}"))

    # Cleanup
    shutil.rmtree(tmp_dir, ignore_errors=True)

    # Report
    print()
    print(f"✓ Wrote   {len(done)} files")
    if skipped:
        print(f"• Skipped {len(skipped)}:")
        for ipa, why in skipped:
            print(f"    /{ipa}/  {why}")
    if failed:
        print(f"✗ Failed  {len(failed)}:")
        for ipa, why in failed:
            print(f"    /{ipa}/  {why}")
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
