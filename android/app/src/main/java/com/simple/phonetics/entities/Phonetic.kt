package com.simple.phonetics.entities

class Phonetic(
    val text: String
) {

    var ipa: HashMap<String, List<String>> = hashMapOf()
}