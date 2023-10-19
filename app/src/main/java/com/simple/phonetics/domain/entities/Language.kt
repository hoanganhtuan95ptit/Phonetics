package com.simple.phonetics.domain.entities

data class Language(val code: String, val name: String, val listIpa: List<Ipa>)

data class Ipa(val code: String, val source: String)