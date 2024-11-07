package com.simple.phonetics.entities

class Phonetics(
    val text: String
) {

    var ipa: HashMap<String, List<String>> = hashMapOf()
}