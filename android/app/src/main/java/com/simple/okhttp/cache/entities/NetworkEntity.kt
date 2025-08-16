package com.simple.okhttp.cache.entities

data class NetworkEntity(
    val id: String = "",

    val code: Int = 200,

    val body: String = "",
    val message: String = "",
    val createdTime: Long = 0
)