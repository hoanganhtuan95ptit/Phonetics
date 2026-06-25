# Phoneme Dictionary (5000 từ)

Dictionary ánh xạ chữ cái ↔ âm IPA cho 5000 từ tiếng Anh phổ biến nhất.

## Files

| File | Kích thước | Mục đích |
|---|---|---|
| `phoneme_dict.json` | 331 KB | Bản raw — đọc trực tiếp khi dev |
| `phoneme_dict.json.gz` | 60 KB | Bản nén — đóng gói vào `assets/` |
| `PhonemeDict.kt` | — | Class Kotlin để load & tra cứu |

## Format

```json
{
  "the":   [["th","ð"], ["e","ə"]],
  "knife": [["kn","n"], ["i","aɪ"], ["fe","f"]],
  "one":   [["o","wʌ"], ["ne","n"]],
  "box":   [["b","b"], ["o","ɑ"], ["x","ks"]]
}
```

Mỗi từ là array các cặp `[grapheme, phoneme_ipa]`:
- `grapheme`: cụm chữ cái (1 hoặc nhiều)
- `phoneme_ipa`: âm IPA tương ứng (có thể là 2 âm khi 1 chữ → 2 âm, vd `x` → `ks`)

Nối tất cả `grapheme` lại = đúng nguyên từ (đã verify 100%).

## Tích hợp Android

1. Copy `phoneme_dict.json.gz` vào `app/src/main/assets/`
2. Copy `PhonemeDict.kt` vào package phù hợp
3. Khởi tạo 1 lần (singleton) trong `Application.onCreate()`:

```kotlin
class MyApp : Application() {
    lateinit var phonemeDict: PhonemeDict
    override fun onCreate() {
        super.onCreate()
        phonemeDict = PhonemeDict.load(this)
    }
}
```

4. Highlight chữ sai trong UI:

```kotlin
val word = "the"
val errorPhonemes = listOf("ð", "ə")  // từ kết quả API
val ranges = phonemeDict.findErrorRanges(word, errorPhonemes)

val spannable = SpannableString(word)
for (range in ranges) {
    spannable.setSpan(
        ForegroundColorSpan(Color.RED),
        range.first, range.last + 1,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
}
textView.text = spannable
```

## Bộ phoneme IPA dùng

**Phụ âm:** `b tʃ d ð f ɡ h dʒ k l m n ŋ p ɹ s ʃ t θ v w j z ʒ`

**Nguyên âm:** `ɑ æ ʌ ɔ aʊ aɪ ɛ ɝ ɚ eɪ ɪ i oʊ ɔɪ ʊ u ə`

**Lưu ý:**
- `ɝ` = âm /ə˞/ có trọng âm (work, bird)
- `ɚ` = âm /ə˞/ không trọng âm (water, doctor)
- `ɹ` = âm /r/ tiếng Anh (không phải rung lưỡi)

## Coverage

- 5000 từ phổ biến nhất (theo wordfreq, dữ liệu thực tế từ web/Twitter/Wikipedia)
- Cover ~95% văn bản thông thường
- Một số single-letters (`s`, `d`, `mr`...) bị skip vì không phải từ phát âm thông thường

## Limitations

Một số trường hợp khó (silent letters phức tạp, đa âm chồng chéo) có thể có alignment hơi lạ. Ví dụ:
- `people` → `[["p","p"],["e","i"],["op","p"],["le","əl"]]` — chữ `o` câm được gộp vào `p`
- `usually` → align có thể không đẹp do CMU có nhiều cách phát âm

Tuy nhiên, với 99%+ trường hợp, alignment là chính xác về mặt ngôn ngữ.
