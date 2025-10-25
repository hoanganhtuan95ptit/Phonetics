package com.simple.phonetics.entities

@Deprecated("")
class Phonetic(
    val text: String
) {

    var ipa: HashMap<String, List<String>> = hashMapOf()
}