package com.simple.phonetics.entities

data class Word(
    val text: String,
    val resource: Resource,
    val languageCode: String
) {

    enum class Resource(val value: String) {

        Popular("Popular"), History("History");

        companion object {

            fun String.toResource() = Resource.entries.find { it.value.equals(this, true) }
        }
    }
}