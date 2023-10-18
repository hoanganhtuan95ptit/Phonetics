package com.simple.phonetics.domain.entities

class Phonetics(
    val text: String
) {

    var ipa: HashMap<String, List<String>> = hashMapOf()
}